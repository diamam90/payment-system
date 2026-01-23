package com.example.transactionservice.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.apache.shardingsphere.broadcast.config.BroadcastRuleConfiguration;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.mode.repository.standalone.StandalonePersistRepositoryConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableReferenceRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.sqltranslator.config.SQLTranslatorRuleConfiguration;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Import(Containers.class)
@TestConfiguration(proxyBeanMethods = false)
public class DatabaseTestConfig {

    @Autowired
    List<PostgreSQLContainer<?>> containers;
    Map<String, DataSource> dataSources;

    @Bean
    DataSource dataSource() throws SQLException {
        SQLTranslatorRuleConfiguration translatorRule = new SQLTranslatorRuleConfiguration("Native", new Properties(), false);

        ModeConfiguration mode = new ModeConfiguration(
                "Standalone",
                new StandalonePersistRepositoryConfiguration("JDBC", new Properties())
        );
        ShardingRuleConfiguration rule = new ShardingRuleConfiguration();
        rule.getTables().add(getWalletsRule());
        rule.getTables().add(getTransactionsRule());

        rule.getBindingTableGroups().add(new ShardingTableReferenceRuleConfiguration("binding", "transactions,wallets"));

        Properties props = new Properties();
        props.setProperty("algorithm-expression", "shard${ Math.abs(UUID.fromString((String)user_uid).hashCode() % 3)}");
        rule.getShardingAlgorithms().put("user_inline", new AlgorithmConfiguration("INLINE", props));
        rule.setDefaultDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("user_uid", "user_inline"));

        props = new Properties();
        props.setProperty(ConfigurationPropertyKey.SQL_SHOW.getKey(), "true");
        return ShardingSphereDataSourceFactory.createDataSource(
                "transaction",
                mode,
                dataSources,
                Arrays.asList(rule, getWalletTypesRule(), translatorRule),
                props
        );
    }

    @PostConstruct
    void startContainers() {
        Map<String, DataSource> result = new HashMap<>();
        for (int i = 0; i < containers.size(); i++) {
            PostgreSQLContainer<?> container = containers.get(i);
            container.start();
            HikariDataSource shard = new HikariDataSource();
            shard.setDriverClassName("org.postgresql.Driver");
            shard.setJdbcUrl(container.getJdbcUrl());
            shard.setUsername(container.getUsername());
            shard.setPassword(container.getPassword());
            result.put("shard" + i, shard);

            Flyway flyway = Flyway.configure()
                    .dataSource(shard)
                    .load();
            flyway.migrate();
        }
        this.dataSources = result;
    }

    private ShardingTableRuleConfiguration getWalletsRule() {
        return new ShardingTableRuleConfiguration("wallets", "shard${0..2}.wallets");
    }

    private ShardingTableRuleConfiguration getTransactionsRule() {
        return new ShardingTableRuleConfiguration("transactions", "shard${0..2}.transactions");
    }

    private BroadcastRuleConfiguration getWalletTypesRule() {
        return new BroadcastRuleConfiguration(Collections.singleton("wallet_types"));
    }
}
