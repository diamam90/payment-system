package com.example.currencyrateservice.IT;

import com.example.currencyrateservice.config.TestSecurityConfig;
import com.example.currencyrateservice.utils.KeycloakUtils;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestSecurityConfig.class)
public class CurrencyControllerIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    TestSecurityConfig testSecurityConfig;

    @Test
    void shouldReturnAllCurrencies() throws Exception {
        AccessTokenResponse accessTokenResponse = KeycloakUtils.adminToken(testSecurityConfig.getKeycloakUrl());

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/currencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenResponse.getToken()))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.*", hasSize(54)),
                        jsonPath("$.[0].code").value("AUD"),
                        jsonPath("$.[0].isoCode").value(36),
                        jsonPath("$.[0].description").value("Доллар(австралийский доллар)"),
                        jsonPath("$.[0].active").value(true),
                        jsonPath("$.[0].symbol").value("$"),

                        jsonPath("$.[53].code").value("CNY"),
                        jsonPath("$.[53].isoCode").value(156),
                        jsonPath("$.[53].description").value("Юань"),
                        jsonPath("$.[53].active").value(true),
                        jsonPath("$.[53].symbol").value("¥")
                );
    }

    @Test
    void getCurrencies_whenHeaderIsAbsent_shouldReturn401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/currencies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrencies_withInvalidAuthorities_shouldReturn403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/currencies").with(jwt()))
                .andExpect(status().isForbidden());
    }
}
