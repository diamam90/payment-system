package com.example.transactionservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class Containers {

    @Bean
    PostgreSQLContainer<?> shard1() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    PostgreSQLContainer<?> shard2() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    PostgreSQLContainer<?> shard3() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    KafkaContainer kafka() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:8.0.4")).withKraft();
    }

    @Bean
    KeycloakContainer keycloak() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                .withRealmImportFile("realm-config.json");
    }
}
