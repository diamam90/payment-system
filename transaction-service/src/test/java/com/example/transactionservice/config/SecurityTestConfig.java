package com.example.transactionservice.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Import(Containers.class)
@TestConfiguration
public class SecurityTestConfig {

    @Autowired
    KeycloakContainer keycloakContainer;

    @PostConstruct
    void startContainer() {
        keycloakContainer.start();
    }

    @Bean
    JwtDecoder jwtDecoderByJwkKeySetUri(@Autowired ObjectProvider<JwkSetUriJwtDecoderBuilderCustomizer> customizers) {
        NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder
                .withJwkSetUri(keycloakContainer.getAuthServerUrl() + "/realms/payment/protocol/openid-connect/certs");
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    public String getKeycloakServerUrl() {
        return keycloakContainer.getAuthServerUrl();
    }
}
