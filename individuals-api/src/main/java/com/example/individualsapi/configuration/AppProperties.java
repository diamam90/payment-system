package com.example.individualsapi.configuration;

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
    private PersonServiceProperties person;
    private TransactionServiceProperties transaction;

    @Getter
    @Setter
    public static class KeycloakProperties {
        private String baseUrl;
        private String realm;
        private String clientId;
        private String clientSecret;
    }

    @Getter
    @Setter
    public static class PersonServiceProperties {
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class TransactionServiceProperties {
        private String baseUrl;
    }
}
