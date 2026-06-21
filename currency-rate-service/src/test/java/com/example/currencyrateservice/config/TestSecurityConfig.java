package com.example.currencyrateservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@TestConfiguration
@Import(Containers.class)
public class TestSecurityConfig {

    @Autowired
    KeycloakContainer keycloakContainer;

    public String getKeycloakUrl() {
        return keycloakContainer.getAuthServerUrl();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        keycloakContainer.start();
        return NimbusJwtDecoder.withJwkSetUri(keycloakContainer.getAuthServerUrl() +
                        "/realms/payment/protocol/openid-connect/certs")
                .build();
    }
}
