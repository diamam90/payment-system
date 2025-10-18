package com.example.personservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public interface AppContainers {

    @Container
    PostgreSQLContainer<?> sqlContainer = new PostgreSQLContainer<>("postgres:17");

    @Container
    KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
            .withRealmImportFile("realm-config.json");


    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AppContainers.sqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", AppContainers.sqlContainer::getUsername);
        registry.add("spring.datasource.password", AppContainers.sqlContainer::getPassword);

        registry.add("spring.flyway.url", AppContainers.sqlContainer::getJdbcUrl);
        registry.add("spring.flyway.user", AppContainers.sqlContainer::getUsername);
        registry.add("spring.flyway.password", AppContainers.sqlContainer::getPassword);

        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> AppContainers.keycloak.getAuthServerUrl() + "/realms/payment/protocol/openid-connect/certs");
    }
}
