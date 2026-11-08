package com.example.IT;

import com.example.config.TestConfig;
import com.example.fake.api.PayoutApi;
import com.example.fake.api.TransactionApi;
import org.apache.http.HttpHeaders;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class PaymentControllerIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Keycloak keycloakAdmin;

    @MockitoSpyBean
    PayoutApi payoutApi;
    @MockitoSpyBean
    TransactionApi transactionApi;

    private static final String PAYMENT_BY_INTERNAL_ID_QUERY = "SELECT * FROM payment.payments WHERE internal_transaction_id = ?";
    private static final String TRUNCATE_PAYMENTS_QUERY = "TRUNCATE payment.payments CASCADE";
    private static final String TRUNCATE_PAYMENTS_PROVIDER = "TRUNCATE payment.payment_providers CASCADE";

    @AfterEach
    void clear() {
        jdbc.execute(TRUNCATE_PAYMENTS_QUERY);
        jdbc.execute(TRUNCATE_PAYMENTS_PROVIDER);
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void shouldCreatePayment() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        mvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content("""
                                {
                                    "internalTransactionUid": "00000000-0000-0000-0000-000000000012",
                                    "methodId": 1,
                                    "amount": 50.03,
                                    "currency": "RUB"
                                }
                                """))
                .andExpectAll(status().isCreated(),
                        jsonPath("providerTransactionId").value(1),
                        jsonPath("internalTransactionUid").value("00000000-0000-0000-0000-000000000012"),
                        jsonPath("amount").value(BigDecimal.valueOf(50.03)),
                        jsonPath("status").value("SUCCESS")
                );
        Map<String, @Nullable Object> params = jdbc.queryForMap(PAYMENT_BY_INTERNAL_ID_QUERY, UUID.fromString("00000000-0000-0000-0000-000000000012"));
        assertThat(params)
                .hasFieldOrPropertyWithValue("status", "SUCCESS")
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(50.03))
                .hasFieldOrPropertyWithValue("payment_method_id", 1)
                .hasFieldOrPropertyWithValue("external_transaction_id", "1");

        verify(payoutApi).createPayout(any());
        verify(transactionApi, never()).createTransaction(any());
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void createPayment_whenFakeProviderReturnError_shouldSetStatusFailed() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        mvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "internalTransactionUid": "00000000-0000-0000-0000-000000000013",
                                    "methodId": 1,
                                    "amount": 50.03,
                                    "currency": "RUB"
                                }
                                """))
                .andExpectAll(status().isBadGateway(),
                        jsonPath("error").value("ERROR_502"),
                        jsonPath("message").value("External service FakePaymentProvider is unavailable")
                );

        Map<String, @Nullable Object> params = jdbc.queryForMap(PAYMENT_BY_INTERNAL_ID_QUERY, UUID.fromString("00000000-0000-0000-0000-000000000013"));
        assertThat(params)
                .hasFieldOrPropertyWithValue("status", "FAILED")
                .hasFieldOrPropertyWithValue("payment_method_id", 1)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(50.03))
                .hasFieldOrPropertyWithValue("external_transaction_id", null);
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void createPayment_whenFakeProviderIsUnavailable_shouldSetStatusFailed() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        mvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "internalTransactionUid": "00000000-0000-0000-0000-000000000014",
                                    "methodId": 1,
                                    "amount": 50.03,
                                    "currency": "RUB"
                                }
                                """))
                .andExpectAll(status().isBadGateway(),
                        jsonPath("error").value("ERROR_502"),
                        jsonPath("message").value("External service FakePaymentProvider is unavailable")
                );

        Map<String, @Nullable Object> params = jdbc.queryForMap(PAYMENT_BY_INTERNAL_ID_QUERY, UUID.fromString("00000000-0000-0000-0000-000000000014"));
        assertThat(params)
                .hasFieldOrPropertyWithValue("status", "FAILED")
                .hasFieldOrPropertyWithValue("payment_method_id", 1)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(50.03))
                .hasFieldOrPropertyWithValue("external_transaction_id", null);
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void shouldCreateTransaction() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        mvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "internalTransactionUid": "00000000-0000-0000-0000-000000000012",
                                    "methodId": 2,
                                    "amount": 50.00,
                                    "currency": "RUB"
                                }
                                """))
                .andExpectAll(status().isCreated(),
                        jsonPath("providerTransactionId").value(1),
                        jsonPath("internalTransactionUid").value("00000000-0000-0000-0000-000000000012"),
                        jsonPath("amount").value(BigDecimal.valueOf(50.00)),
                        jsonPath("status").value("SUCCESS")
                );
        Map<String, @Nullable Object> params = jdbc.queryForMap(PAYMENT_BY_INTERNAL_ID_QUERY, UUID.fromString("00000000-0000-0000-0000-000000000012"));
        assertThat(params)
                .hasFieldOrPropertyWithValue("status", "SUCCESS")
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("payment_method_id", 2)
                .hasFieldOrPropertyWithValue("external_transaction_id", "1");

        verify(payoutApi, never()).createPayout(any());
        verify(transactionApi).createTransaction(any());
    }

    @Sql({"/sql/find-by-currency-and-country-codes.sql", "/sql/compensate-payment.sql"})
    @Test
    void shouldCompensate() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        mvc.perform(post("/private/api/v1/payments/2/compensate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        Map<String, @Nullable Object> params = jdbc.queryForMap(PAYMENT_BY_INTERNAL_ID_QUERY, UUID.fromString("00000000-0000-0000-0000-000000000012"));
        assertThat(params)
                .hasFieldOrPropertyWithValue("status", "FAILED")
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(12.22).setScale(2, RoundingMode.HALF_EVEN));
    }

    @Sql({"/sql/find-by-currency-and-country-codes.sql", "/sql/compensate-payment.sql"})
    @Test
    void compensate_whenPaymentFinalStatus_shouldDoNothing() throws Exception {
        String token = keycloakAdmin.tokenManager().getAccessToken().getToken();
        mvc.perform(post("/private/api/v1/payments/3/compensate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        Map<String, @Nullable Object> params = jdbc.queryForMap(PAYMENT_BY_INTERNAL_ID_QUERY, UUID.fromString("00000000-0000-0000-0000-000000000013"));
        assertThat(params)
                .hasFieldOrPropertyWithValue("status", "SUCCESS")
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(12.22).setScale(2, RoundingMode.HALF_EVEN));
    }
}