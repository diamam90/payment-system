package com.example.currencyrateservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@TestConfiguration
public class Containers {

    @Bean
    KeycloakContainer keycloak() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                .withRealmImportFile("realm-config.json");
    }

    @Bean
    WireMockContainer wiremock() {
        return new WireMockContainer("wiremock/wiremock:3.13.2")
                .withMappingFromResource("currencyList", Containers.class, "/mapping/currency_list.json")
                .withMappingFromResource("rates", Containers.class, "/mapping/rates.json");
    }
}
