package com.example.IT;

import com.example.config.TestConfig;
import com.example.exception.StatusCode;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public class PaymentMethodIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Keycloak keycloakAdmin;

    private static final String TRUNCATE_PAYMENTS_QUERY = "TRUNCATE payment.payments CASCADE";
    private static final String TRUNCATE_PAYMENTS_PROVIDER = "TRUNCATE payment.payment_providers CASCADE";

    @AfterEach
    void clear() {
        jdbc.execute(TRUNCATE_PAYMENTS_QUERY);
        jdbc.execute(TRUNCATE_PAYMENTS_PROVIDER);
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void shouldGetMethod() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "RUB");
        map.add("countryCode", "RUS");
        map.add("type", "TRANSACTION");
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .params(map))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(4),
                        jsonPath("$.name").value("transaction")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRATATA", "TRANS", "PAY", "PAYOUT1", "TRANSACTION228"})
    void getMethod_whenTypeInvalidShouldReturn400(String type) throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "RUB");
        map.add("countryCode", "RUS");
        map.add("type", type);
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .params(map))
                .andExpectAll(status().isBadRequest(),
                        jsonPath("$.error").value(StatusCode.ERROR_400.name()),
                        jsonPath("$.message").isNotEmpty()
                );
    }

    @Test
    void getMethod_whenMethodNotFound_shouldReturn404() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "RUB");
        map.add("countryCode", "RUS");
        map.add("type", "TRANSACTION");
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .params(map))
                .andExpectAll(status().isNotFound(),
                        jsonPath("$.error").value(StatusCode.ERROR_404.name()),
                        jsonPath("$.message").value("PaymentMethod type: TRANSACTION, with currencyCode: RUB, countryCode: RUS not found")
                );
    }
}
