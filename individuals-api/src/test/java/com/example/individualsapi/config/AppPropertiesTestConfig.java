package com.example.individualsapi.config;

import com.example.individualsapi.configuration.AppProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.function.Consumer;

@TestConfiguration
public class AppPropertiesTestConfig {

    @Bean
    AppProperties properties(List<Consumer<AppProperties>> customizers) {
        AppProperties properties = new AppProperties();
        customizers.forEach(customizer -> customizer.accept(properties));
        return properties;
    }
}
