package com.example.individualsapi.config;

import com.example.individualsapi.IT.BaseIntegrationTest;
import com.example.individualsapi.configuration.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.MountableFile;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.function.Consumer;

@Getter
@TestConfiguration
public class MockServiceConfig {

    private final WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:3.13.2")
            .withCopyFileToContainer(MountableFile.forClasspathResource("mapping"),"/home/wiremock/mappings");

    @PostConstruct
    public void setup() {
        wireMock.start();
    }

    @Bean
    Consumer<AppProperties> mockServicePropertiesCustomizer() {
        return (properties) -> {
            AppProperties.CurrencyRateServiceProperties currencyRateProperties = new AppProperties.CurrencyRateServiceProperties();
            currencyRateProperties.setBaseUrl(wireMock.getBaseUrl());
            properties.setCurrencyRate(currencyRateProperties);

            AppProperties.PersonServiceProperties personServiceProperties = new AppProperties.PersonServiceProperties();
            personServiceProperties.setBaseUrl(wireMock.getBaseUrl());
            properties.setPerson(personServiceProperties);

            AppProperties.TransactionServiceProperties transactionServiceProperties = new AppProperties.TransactionServiceProperties();
            transactionServiceProperties.setBaseUrl(wireMock.getBaseUrl());
            properties.setTransaction(transactionServiceProperties);
        };
    }
}
