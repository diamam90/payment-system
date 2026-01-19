package com.example.individualsapi.configuration;

import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class HttpHandlerConfiguration {

    private final ApplicationContext context;

    @Bean
    public HttpHandler handler(ObservationRegistry registry){
        return WebHttpHandlerBuilder.applicationContext(context)
                .observationRegistry(registry)
                .build();
    }
}

