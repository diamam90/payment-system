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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public class CurrencyRateControllerIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    TestSecurityConfig testSecurityConfig;

    @Test
    @Sql(scripts = {"/sql/clean.sql", "/sql/currency-rate-one-result.sql"})
    void shouldReturnRateByFilter() throws Exception {
        // given
        AccessTokenResponse accessTokenResponse = KeycloakUtils.adminToken(testSecurityConfig.getKeycloakUrl());
        String from = "ABC";
        String to = "BCD";
        ZonedDateTime timestamp = ZonedDateTime.parse("2026-05-05T12:06:00+00:00");
        // then
        mvc.perform(get("/api/v1/rates?from={0}&to={1}&timestamp={2}", from, to, timestamp)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenResponse.getToken()))
                .andExpectAll(status().isOk(),
                        jsonPath("$.sourceCode").value("ABC"),
                        jsonPath("$.destinationCode").value("BCD"),
                        jsonPath("$.rate").value(BigDecimal.valueOf(0.0001)),
                        jsonPath("$.providerCode").value("asc"),
                        jsonPath("$.rateTimestamp").isNotEmpty()
                );
    }

    @Test
    @Sql(scripts = "/sql/clean.sql")
    void rateByFilter_shouldReturn404() throws Exception {
        // given
        AccessTokenResponse accessTokenResponse = KeycloakUtils.adminToken(testSecurityConfig.getKeycloakUrl());
        String from = "ABC";
        String to = "DBC";
        ZonedDateTime timestamp = ZonedDateTime.parse("2025-05-05T12:00:00Z");
        // then
        mvc.perform(get("/api/v1/rates?from={0}&to={1}&timestamp={2}", from, to, timestamp)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenResponse.getToken()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("CurrencyRate not found by attribute sourceCode: %s, targetCode: %s, dateTime: %s".formatted(from, to, timestamp)));
    }

    @Test
    void rateByFilter_whenHeaderIsAbsent_shouldReturn401() throws Exception {
        String from = "ABC";
        String to = "BCD";
        ZonedDateTime timestamp = ZonedDateTime.parse("2026-05-05T12:06:00+00:00");
        // then
        mvc.perform(get("/api/v1/rates?from={0}&to={1}&timestamp={2}", from, to, timestamp))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateByFilter_withInvalidAuthorities_shouldReturn403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/currencies").with(jwt()))
                .andExpect(status().isForbidden());
    }
}
