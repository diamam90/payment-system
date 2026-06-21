package com.example.individualsapi.config;

import com.example.individualsapi.configuration.AppProperties;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.util.function.Consumer;

@TestConfiguration
public class SecurityTestConfig {

    private final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2").withRealmImportFile("realm-config.json");

    @PostConstruct
    public void setup() {
        keycloak.start();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder
                .withJwkSetUri(keycloak.getAuthServerUrl() + "/realms/payment/protocol/openid-connect/certs")
                .build();
    }

    @Bean
    public Consumer<AppProperties> securityPropertiesCustomizer() {
        return (properties) -> {
            AppProperties.KeycloakProperties keycloakProperties = new AppProperties.KeycloakProperties();
            keycloakProperties.setBaseUrl(keycloak.getAuthServerUrl());
            keycloakProperties.setClientId("individuals-api");
            keycloakProperties.setRealm("payment");
            keycloakProperties.setClientSecret("**********");

            properties.setKeycloak(keycloakProperties);
        };
    }

    public String getBaseUrl() {
        return keycloak.getAuthServerUrl();
    }
}
