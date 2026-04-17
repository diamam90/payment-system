package com.example.transactionservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Flyway.class)
@EnableConfigurationProperties(FlywayDataSourceProperties.class)
public class ShardingSphereFlywayAutoConfiguration {

    @Bean
    FlywayExecutor flywayExecutor(FlywayDataSourceProperties properties) {
        return new FlywayExecutor(properties);
    }
}
