package com.example.personservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

@Import(AppContainers.class)
@TestConfiguration(proxyBeanMethods = false)
public class DatabaseConfig {

    @Autowired
    PostgreSQLContainer<?> psqlContainer;

    @PostConstruct
    void startContainer() {
        psqlContainer.start();
    }

    @Bean
    DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(psqlContainer.getJdbcUrl());
        config.setUsername(psqlContainer.getUsername());
        config.setPassword(psqlContainer.getPassword());
        return new HikariDataSource(config);
    }
}
