package com.example.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final AppProperties properties;

    @Bean
    WebClient keyCloakClient() {
        var baseUrl = properties.getKeycloak().getBaseUrl();
        log.debug("keycloak base url :{}", baseUrl);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
