package com.example.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.config.EnableWebFlux;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

@Configuration
@EnableWebFlux
public class AppConfig {

    @Bean
    ObjectMapper mapper() {

        return new Jackson2ObjectMapperBuilder()
                .dateFormat(new StdDateFormat())
                .serializers()
                .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS)
                .modules(new JavaTimeModule()).build();
    }
}
