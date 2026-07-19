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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public class ProviderControllerIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    TestSecurityConfig testSecurityConfig;

    @Test
    @Sql(scripts = "/sql/clean.sql")
    void shouldReturnEmptyProviderList() throws Exception {
        // given
        AccessTokenResponse accessTokenResponse = KeycloakUtils.adminToken(testSecurityConfig.getKeycloakUrl());
        // then
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/rate-providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenResponse.getToken()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$").isEmpty()
                );
    }

    @Sql({"/sql/clean.sql", "/sql/find-provider-code-in.sql"})
    @Test
    void shouldReturn2Providers() throws Exception {
        // given
        AccessTokenResponse accessTokenResponse = KeycloakUtils.adminToken(testSecurityConfig.getKeycloakUrl());
        // then
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/rate-providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenResponse.getToken()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.*", hasSize(2)),
                        jsonPath("$.[0].providerCode").value("cur"),
                        jsonPath("$.[0].providerName").value("test provider"),
                        jsonPath("$.[0].description").value("test provider"),
                        jsonPath("$.[0].priority").value(1),
                        jsonPath("$.[0].active").value(true),

                        jsonPath("$.[1].providerCode").value("tes"),
                        jsonPath("$.[1].providerName").value("test provider 2"),
                        jsonPath("$.[1].description").value("test provider 2"),
                        jsonPath("$.[1].priority").value(2),
                        jsonPath("$.[1].active").value(true)
                );
    }


    @Test
    void rateProviders_whenHeaderIsAbsent_shouldReturn401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/rate-providers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateProviders_withInvalidAuthorities_shouldReturn403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/rate-providers").with(jwt()))
                .andExpect(status().isForbidden());
    }
}
