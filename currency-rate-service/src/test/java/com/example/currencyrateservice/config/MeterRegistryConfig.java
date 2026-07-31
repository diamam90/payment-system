package com.example.currencyrateservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MeterRegistryConfig {

    @Bean
    MeterRegistry registry() {
        return new SimpleMeterRegistry();
    }
}
