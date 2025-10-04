package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class IndividualsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndividualsApiApplication.class, args);
    }
}
