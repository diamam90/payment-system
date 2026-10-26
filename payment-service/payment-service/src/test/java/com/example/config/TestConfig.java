package com.example.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.testcontainers.utility.MountableFile;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.LinkedHashMap;

@TestConfiguration(proxyBeanMethods = false)
public class TestConfig {

    @Bean
    WireMockContainer wiremock() {
        WireMockContainer container = new WireMockContainer("wiremock/wiremock:3.13.2")
                .withCopyFileToContainer(MountableFile.forClasspathResource("mapping"), "/home/wiremock/mappings");
        container.start();
        return container;
    }

    @Bean
    KeycloakContainer keycloak() {
        KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                .withRealmImportFile("realm-config.json");
        keycloak.start();
        return keycloak;
    }

    @Bean
    PropertySourcesPlaceholderConfigurer properties(WireMockContainer wiremock, ConfigurableEnvironment env) {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("payment-service.fake-payment-provider.base-url", wiremock.getBaseUrl());
        properties.put("payment-service.fake-payment-provider.merchant-id", "merchant 1");
        properties.put("payment-service.fake-payment-provider.merchant-password", "merchant 1 password");

        env.getPropertySources().addFirst(new MapPropertySource("testProperties", properties));
        propertySourcesPlaceholderConfigurer.setEnvironment(env);
        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    Keycloak keycloakAdmin(KeycloakContainer keycloakContainer) {
        return KeycloakBuilder.builder()
                .grantType("client_credentials")
                .clientId("individuals-api")
                .clientSecret("**********")
                .realm("payment")
                .serverUrl(keycloakContainer.getAuthServerUrl())
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(KeycloakContainer keycloakContainer) {
        return NimbusJwtDecoder.withJwkSetUri(keycloakContainer.getAuthServerUrl() + "/realms/payment/protocol/openid-connect/certs")
                .build();
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
