package com.example.IT;

import com.example.config.TestConfig;
import com.example.exception.StatusCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public class PaymentMethodIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    private static final String MERCHANT_BASIC_AUTH = "aW5kaXZpZHVhbHMtYXBpOnNlY3JldA==";

    @AfterEach
    void clear() {
        jdbc.execute("DELETE FROM payment.payment_method_definitions WHERE payment_method_id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_method_required_fields WHERE payment_method_id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_methods WHERE id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_providers WHERE id NOT IN (2)");
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void shouldGetMethod() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "JPY");
        map.add("countryCode", "RUS");
        map.add("type", "PAYOUT");
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + MERCHANT_BASIC_AUTH)
                        .params(map))
                .andExpectAll(status().isOk(),
                        jsonPath("$..id").value(1),
                        jsonPath("$[0].name").value("payout")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRATATA", "TRANS", "PAY", "PAYOUT1", "TRANSACTION228"})
    void getMethod_whenTypeInvalidShouldReturn400(String type) throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "RUB");
        map.add("countryCode", "RUS");
        map.add("type", type);
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + MERCHANT_BASIC_AUTH)
                        .params(map))
                .andExpectAll(status().isBadRequest(),
                        jsonPath("$.error").value(StatusCode.ERROR_400.name()),
                        jsonPath("$.message").isNotEmpty()
                );
    }

    @Test
    void getMethod_whenMethodsNotFound_shouldReturnEmptyList() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "RUB");
        map.add("countryCode", "RUS");
        map.add("type", "TRANSACTION");
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + MERCHANT_BASIC_AUTH)
                        .params(map))
                .andExpectAll(
                        status().isOk(),
                        content().string("[]")
                );
    }

    @Test
    @Sql("/sql/payment-methods-with-required-field-and-logo.sql")
    void getMethod_whenTypeISNull_shouldReturnMethod() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "RUB");
        map.add("countryCode", "RUS");
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + MERCHANT_BASIC_AUTH)
                        .params(map))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$..id").value(Matchers.containsInAnyOrder(1, 10))
                )
                .andDo(print());
    }

    @Sql("/sql/payment-methods-with-required-field-and-logo.sql")
    @Test
    void getMethod_shouldReturnPaymentMethodWithRequiredFieldAndUrl() throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("currencyCode", "JPY");
        map.add("countryCode", "RUS");
        mvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + MERCHANT_BASIC_AUTH)
                        .params(map))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$..imageUrl").value("http://logo.url"),
                        jsonPath("$..requiredFields..dataType").value(Matchers.containsInAnyOrder("data type 1", "data type 2")),
                        jsonPath("$..requiredFields..name").value(Matchers.containsInAnyOrder("228 name", "229 name"))
                )
                .andDo(print());
    }
}
