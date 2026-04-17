package com.example.transactionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "transaction-service.flyway")
public class FlywayDataSourceProperties {

    private Map<String, DataSourceProperties> dataSources;

    @Getter
    @Setter
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
    }
}
