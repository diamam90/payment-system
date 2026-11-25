package com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "merchant")
public class MerchantProperties {

    private List<Credentials> credentialsList;

    @Data
    public static class Credentials {
        private String username;
        private String password;
    }
}
