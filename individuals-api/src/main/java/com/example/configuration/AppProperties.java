package com.example.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "individuals-api")
public class AppProperties {

    private KeycloakProperties keycloak;

    @Getter
    @Setter
    public static class KeycloakProperties {
        private String baseUrl;
        private String username;
        private String password;
        private String realm;
        private String clientId;
    }
}
