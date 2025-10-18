package com.example.individualsapi.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class AppConfig {

    @Bean
    ObjectMapper mapper() {
        return new Jackson2ObjectMapperBuilder()
                .dateFormat(new StdDateFormat())
                .modules(new JavaTimeModule())
                .build();
    }
}
