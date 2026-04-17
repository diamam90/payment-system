package com.example.transactionservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.driver.ShardingSphereDriver;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import javax.annotation.PostConstruct;

@ConditionalOnClass(ShardingSphereDriver.class)
@RequiredArgsConstructor
public class FlywayExecutor {

    private final FlywayDataSourceProperties properties;

    @PostConstruct
    void migrate() {
        properties.getDataSources()
                .values()
                .forEach(props -> Flyway.configure()
                        .dataSource(props.getUrl(), props.getUsername(), props.getPassword())
                        .load()
                        .migrate()
                );
    }
}
