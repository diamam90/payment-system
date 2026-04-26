package com.example.transactionservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.TimeZone;

@Configuration
public class AppConfig {

    @PostConstruct
    void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @Bean
    Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
