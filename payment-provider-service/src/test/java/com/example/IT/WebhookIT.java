package com.example.IT;

import com.example.dto.StatusCode;
import com.example.repository.PayoutRepository;
import com.example.repository.TransactionRepository;
import com.example.repository.WebhookRepository;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebhookIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JdbcClient jdbc;

    @MockitoSpyBean
    WebhookRepository webhookRepository;
    @MockitoSpyBean
    TransactionRepository transactionRepository;
    @MockitoSpyBean
    PayoutRepository payoutRepository;


    @AfterEach
    void truncate() {
        jdbc.sql("TRUNCATE TABLE payment.transactions CASCADE").update();
        jdbc.sql("TRUNCATE TABLE payment.payouts CASCADE").update();
        jdbc.sql("TRUNCATE TABLE payment.webhooks CASCADE").update();
    }

    @Sql("/sql/webhook-transaction-update.sql")
    @Test
    void shouldUpdateTransaction() throws Exception {
        // update transaction client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isOk());

        Map<String, Object> transactionParams = jdbc.sql("SELECT * FROM payment.transactions WHERE id = :transactionId")
                .param("transactionId", 6L)
                .query()
                .singleRow();

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("status", "SUCCESS")
                .hasFieldOrPropertyWithValue("merchant_id", 1)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(66.00).setScale(2))
                .hasFieldOrPropertyWithValue("currency", "USD")
                .hasFieldOrPropertyWithValue("method", "CARD");

        verify(webhookRepository).save(any());
    }

    @Sql("/sql/webhook-transaction-update.sql")
    @Test
    void shouldUpdateTransactionFailedStatus() throws Exception {
        // update transaction client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "FAILED",
                                            "reason": "something went wrong"
                                        }
                                        """))
                .andExpect(status().isOk());

        Map<String, Object> transactionParams = jdbc.sql("SELECT * FROM payment.transactions WHERE id = :transactionId")
                .param("transactionId", 6L)
                .query()
                .singleRow();

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("status", "FAILED")
                .hasFieldOrPropertyWithValue("merchant_id", 1)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(66.00).setScale(2))
                .hasFieldOrPropertyWithValue("currency", "USD")
                .hasFieldOrPropertyWithValue("method", "CARD");

        Map<String, Object> webhookParams = jdbc.sql("SELECT * FROM payment.webhooks WHERE event_type = 'TRANSACTION' AND entity_id = :transactionId")
                .param("transactionId", 6L)
                .query()
                .singleRow();

        assertThat(webhookParams.entrySet())
                .contains(Map.entry("entity_id", 6L))
                .contains(Map.entry("event_type", "TRANSACTION"))
                .contains(new AbstractMap.SimpleEntry<>("notification_url", null))
                .anyMatch(entry -> {
                    if (entry.getKey().equals("payload") && entry.getValue() instanceof PGobject str) {
                        return str.getValue().contains("\"reason\": \"something went wrong\"");
                    }
                    return false;
                });

        verify(webhookRepository).save(any());
    }


    @Sql("/sql/webhook-transaction-final-status.sql")
    @Test
    void updateTransactionWithFinalStatus_WhenItNotEqualsRequest_shouldReturnBadRequest() throws Exception {
        // update transaction client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "FAILED"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        verify(webhookRepository, never()).save(any());
    }

    @Sql("/sql/webhook-transaction-final-status.sql")
    @Test
    void updateTransactionWithFinalStatus_WhenEqualRequest_shouldDoNothing() throws Exception {
        // update transaction client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isOk());

        verify(webhookRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenItsNotFound_shouldReturn400() throws Exception {
        // update transaction client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "FAILED"
                                        }
                                        """))
                .andExpectAll(status().isBadRequest(),
                        jsonPath("$.error").value(StatusCode.ERROR_400.name()),
                        jsonPath("$.message").value("Пополнение с идентификатором [6] не найдено")
                );

        verify(webhookRepository, never()).save(any());
    }


    @Sql("/sql/webhook-payout-update.sql")
    @Test
    void shouldUpdatePayout() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 23,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isOk());

        Map<String, Object> payoutParams = jdbc.sql("SELECT * FROM payment.payouts WHERE id = :payoutId")
                .param("payoutId", 23L)
                .query()
                .singleRow();

        assertThat(payoutParams)
                .hasFieldOrPropertyWithValue("status", "SUCCESS")
                .hasFieldOrPropertyWithValue("merchant_id", 1)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(6.6).setScale(2))
                .hasFieldOrPropertyWithValue("currency", "EUR");

        verify(webhookRepository).save(any());
    }


    @Sql("/sql/webhook-payout-update.sql")
    @Test
    void shouldUpdatePayoutFailedStatus() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 23,
                                            "status": "FAILED",
                                            "reason": "something went wrong"
                                        }
                                        """))
                .andExpect(status().isOk());

        Map<String, Object> payoutParams = jdbc.sql("SELECT * FROM payment.payouts WHERE id = :payoutId")
                .param("payoutId", 23L)
                .query()
                .singleRow();

        assertThat(payoutParams)
                .hasFieldOrPropertyWithValue("status", "FAILED")
                .hasFieldOrPropertyWithValue("merchant_id", 1)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(6.6).setScale(2))
                .hasFieldOrPropertyWithValue("currency", "EUR");

        Map<String, Object> webhookParams = jdbc.sql("SELECT * FROM payment.webhooks WHERE event_type = 'PAYOUT' AND entity_id = :payoutId")
                .param("payoutId", 23L)
                .query()
                .singleRow();

        assertThat(webhookParams.entrySet())
                .contains(Map.entry("entity_id", 23L))
                .contains(Map.entry("event_type", "PAYOUT"))
                .contains(new AbstractMap.SimpleEntry<>("notification_url", null))
                .anyMatch(entry -> {
                    if (entry.getKey().equals("payload") && entry.getValue() instanceof PGobject str) {
                        return str.getValue().contains("\"reason\": \"something went wrong\"");
                    }
                    return false;
                });

        verify(webhookRepository).save(any());
    }


    @Sql("/sql/webhook-payout-final-status.sql")
    @Test
    void updatePayoutWithFinalStatus_WhenItNotEqualsRequest_shouldReturnBadRequest() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        verify(webhookRepository, never()).save(any());
    }

    @Sql("/sql/webhook-payout-final-status.sql")
    @Test
    void updatePayoutWithFinalStatus_WhenEqualRequest_shouldDoNothing() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 23,
                                            "status": "FAILED"
                                        }
                                        """))
                .andExpect(status().isOk());

        verify(webhookRepository, never()).save(any());
    }

    @Test
    void updatePayout_whenItsNotFound_shouldReturn400() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "FAILED"
                                        }
                                        """))
                .andExpectAll(status().isBadRequest(),
                        jsonPath("$.error").value(StatusCode.ERROR_400.name()),
                        jsonPath("$.message").value("Выплата с идентификатором [6] не найдена")
                );

        verify(webhookRepository, never()).save(any());
    }
}
