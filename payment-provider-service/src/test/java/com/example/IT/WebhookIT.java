package com.example.IT;

import com.example.dto.StatusCode;
import com.example.repository.PayoutRepository;
import com.example.repository.TransactionRepository;
import com.example.repository.WebhookRepository;
import jakarta.annotation.PostConstruct;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
    JdbcClient jdbc;

    RestTestClient restTestClient;

    @MockitoSpyBean
    WebhookRepository webhookRepository;
    @MockitoSpyBean
    TransactionRepository transactionRepository;
    @MockitoSpyBean
    PayoutRepository payoutRepository;

    @PostConstruct
    void setup() {
        restTestClient = RestTestClient.bindTo(mvc).build();
    }

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

    @RepeatedTest(3)
    @Sql("/sql/webhook-transaction-update.sql")
    void updateTransaction_whenParallelRequestExecutes_shouldUpdateExactlyOnce() throws Exception {
        try (ExecutorService executorService = new ThreadPoolExecutor(5, 5, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>())) {
            List<Callable<RestTestClient.ResponseSpec>> tasks = new ArrayList<>();
            tasks.add(this::failedTransaction);
            tasks.add(this::failedTransaction);
            tasks.add(this::failedTransaction);
            tasks.add(this::successTransaction);
            tasks.add(this::successTransaction);
            executorService.invokeAll(tasks);

            Map<String, Object> transactionParams = jdbc.sql("SELECT * FROM payment.transactions WHERE id = :transactionId")
                    .param("transactionId", 6L)
                    .query()
                    .singleRow();

            assertThat(transactionParams)
                    .hasFieldOrPropertyWithValue("merchant_id", 1)
                    .extractingByKey("status", InstanceOfAssertFactories.STRING)
                    .isNotEqualTo("PENDING");

            assertTrue(
                    jdbc.sql("SELECT count(id) = 1 FROM payment.webhooks WHERE event_type = 'TRANSACTION' AND entity_id = :transactionId")
                            .param("transactionId", 6L)
                            .query(Boolean.class)
                            .single()
            );
        }
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

    @RepeatedTest(3)
    @Sql("/sql/webhook-payout-update.sql")
    void updatePayout_whenParallelRequestExecutes_shouldUpdateExactlyOnce() throws Exception {
        try (ExecutorService executorService = new ThreadPoolExecutor(5, 5, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>())) {
            List<Callable<RestTestClient.ResponseSpec>> tasks = new ArrayList<>();
            tasks.add(this::failedPayout);
            tasks.add(this::failedPayout);
            tasks.add(this::failedPayout);
            tasks.add(this::successPayout);
            tasks.add(this::successPayout);
            executorService.invokeAll(tasks);

            Map<String, Object> transactionParams = jdbc.sql("SELECT * FROM payment.payouts WHERE id = :paymentId")
                    .param("paymentId", 23L)
                    .query()
                    .singleRow();

            assertThat(transactionParams)
                    .hasFieldOrPropertyWithValue("merchant_id", 1)
                    .extractingByKey("status", InstanceOfAssertFactories.STRING)
                    .isNotEqualTo("PENDING");

            assertTrue(
                    jdbc.sql("SELECT count(id) = 1 FROM payment.webhooks WHERE event_type = 'PAYOUT' AND entity_id = :paymentId")
                            .param("paymentId", 23L)
                            .query(Boolean.class)
                            .single()
            );
        }
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
                                            "id": 23,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        Map<String, Object> payoutParams = jdbc.sql("SELECT * FROM payment.payouts WHERE id = :payoutId")
                .param("payoutId", 23L)
                .query()
                .singleRow();

        assertThat(payoutParams)
                .hasFieldOrPropertyWithValue("id", 23L)
                .hasFieldOrPropertyWithValue("status", "FAILED");

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

    @Sql("/sql/webhook-payout-update.sql")
    @Test
    void updatePayout_whenStatusFromRequestDoesNotEqualSuccessOrFailed_shouldReturn400() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "PENDING"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andDo(print());

        Map<String, Object> payoutParams = jdbc.sql("SELECT * FROM payment.payouts WHERE id = :payoutId")
                .param("payoutId", 23L)
                .query()
                .singleRow();

        assertThat(payoutParams)
                .hasFieldOrPropertyWithValue("id", 23L)
                .hasFieldOrPropertyWithValue("status", "PENDING");

        verify(webhookRepository, never()).save(any());
    }

    @Test
    void updatePayout_whenRequestIdIsNegative_shouldReturn400() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": -6,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andDo(print());

        verify(webhookRepository, never()).save(any());
    }

    @Sql("/sql/webhook-transaction-update.sql")
    @Test
    void updateTransaction_whenStatusFromRequestDoesNotEqualSuccessOrFailed_shouldReturn400() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": 6,
                                            "status": "PENDING"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andDo(print());

        Map<String, Object> payoutParams = jdbc.sql("SELECT * FROM payment.transactions WHERE id = :transactionId")
                .param("transactionId", 6L)
                .query()
                .singleRow();

        assertThat(payoutParams)
                .hasFieldOrPropertyWithValue("id", 6L)
                .hasFieldOrPropertyWithValue("status", "PENDING");

        verify(webhookRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenRequestIdIsNegative_shouldReturn400() throws Exception {
        // update payout client1
        mvc.perform(post("/webhook/transaction")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": -6,
                                            "status": "SUCCESS"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andDo(print());

        verify(webhookRepository, never()).save(any());
    }

    private RestTestClient.ResponseSpec failedTransaction() {
        return restTestClient.post()
                .uri("/webhook/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                .body("""
                        {
                            "id": 6,
                            "status": "FAILED"
                        }
                        """)
                .exchange();
    }

    private RestTestClient.ResponseSpec successTransaction() {
        return restTestClient.post()
                .uri("/webhook/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                .body("""
                        {
                            "id": 6,
                            "status": "SUCCESS"
                        }
                        """)
                .exchange();
    }

    private RestTestClient.ResponseSpec failedPayout() {
        return restTestClient.post()
                .uri("/webhook/payout")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                .contentType(APPLICATION_JSON)
                .body(//language=JSON//
                        """
                                {
                                    "id": 23,
                                    "status": "FAILED"
                                }
                                """)
                .exchange();
    }

    private RestTestClient.ResponseSpec successPayout() {
        return restTestClient.post()
                .uri("/webhook/payout")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MTpwYXNzd29yZA==")
                .contentType(APPLICATION_JSON)
                .body(//language=JSON//
                        """
                                {
                                    "id": 23,
                                    "status": "SUCCESS"
                                }
                                """)
                .exchange();
    }
}
