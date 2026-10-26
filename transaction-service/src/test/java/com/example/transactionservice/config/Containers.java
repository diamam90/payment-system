package com.example.transactionservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class Containers {

    @Bean
    PostgreSQLContainer<?> shard1() {
        var container = new PostgreSQLContainer<>("postgres:17").withReuse(true);
        container.start();
        return container;
    }

    @Bean
    PostgreSQLContainer<?> shard2() {
        var container = new PostgreSQLContainer<>("postgres:17").withReuse(true);
        container.start();
        return container;
    }

    @Bean
    PostgreSQLContainer<?> shard3() {
        var container = new PostgreSQLContainer<>("postgres:17").withReuse(true);
        container.start();
        return container;
    }

    @Bean
    KafkaContainer kafka() {
        var container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:8.0.4"))
                .withKraft().withReuse(true);
        container.start();
        return container;
    }

    @Bean
    KeycloakContainer keycloak() {
        var container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                .withRealmImportFile("realm-config.json").withReuse(true);
        container.start();
        return container;
    }
}
