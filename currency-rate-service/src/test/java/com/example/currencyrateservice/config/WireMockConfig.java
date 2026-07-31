package com.example.currencyrateservice.config;

import com.example.currencyrateservice.client.CurrateProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@TestConfiguration
@Import(Containers.class)
public class WireMockConfig {

    @Autowired
    WireMockContainer wireMockContainer;

    @Primary
    @Bean
    CurrateProperties providerProperties() {
        wireMockContainer.start();
        CurrateProperties currate = new CurrateProperties();
        currate.setName("currate");
        currate.setCode("tes");
        currate.setApiKey("SECRET");
        currate.setBaseUrl(wireMockContainer.getBaseUrl());
        currate.setDescription("mocked currate client");
        currate.setPriority(1);
        return currate;
    }
}
