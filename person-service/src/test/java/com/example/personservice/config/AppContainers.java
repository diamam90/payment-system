package com.example.personservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class AppContainers {

    @Bean
    PostgreSQLContainer<?> sqlContainer() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    KeycloakContainer keycloak() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                .withRealmImportFile("realm-config.json");
    }
}
