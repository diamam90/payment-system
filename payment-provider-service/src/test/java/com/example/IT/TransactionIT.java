package com.example.IT;

import jakarta.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;
import org.hamcrest.core.Every;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    JdbcClient jdbc;

    @AfterEach
    void truncate() {
        jdbc.sql("TRUNCATE TABLE payment.transactions CASCADE").update();
    }

    @Test
    void shouldCreateTransactionWithPendingStatus() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ=")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(//language=JSON
                                """
                                        {
                                            "amount": 12.3,
                                            "currency": "RUB",
                                            "method":  "CARD"
                                        }
                                        """))
                .andExpectAll(status().isCreated(),
                        jsonPath("status").value("PENDING"),
                        jsonPath("amount").value(12.3),
                        jsonPath("method").value("CARD"),
                        jsonPath("currency").value("RUB"),
                        jsonPath("id").isNotEmpty(),
                        jsonPath("merchantId").value("merchant1"),
                        jsonPath("createdAt").isNotEmpty())
                .andDo(print());
    }

    @Sql("/sql/transaction-by-id.sql")
    @Test
    void shouldReturnTransactionById() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions/4")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ="))
                .andExpectAll(status().isOk(),
                        jsonPath("id").value(4),
                        jsonPath("merchantId").value("merchant1"),
                        jsonPath("amount").value(20.54),
                        jsonPath("currency").value("USD"),
                        jsonPath("method").value("CARD"),
                        jsonPath("status").value("SUCCESS"),
                        jsonPath("createdAt").isNotEmpty()
                );
    }

    @Test
    void getById_whenTransactionNotFound_shouldReturn404() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions/444")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ="))
                .andExpectAll(status().isNotFound(),
                        jsonPath("error").value("ERROR_404"),
                        jsonPath("message").value("Transaction with identifier [444] not found")
                );
    }

    @Sql("/sql/transaction-filter.sql")
    @Test
    void shouldReturn2TransactionsByMerchantId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions?start_date={1}&end_date={2}",
                                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                                ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 999, ZoneOffset.UTC)
                        )
                        // merchant 2 auth
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQyOm1lcmNoYW50IDIgcGFzc3dvcmQ="))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$..merchantId").value(Every.everyItem(IsEqual.equalTo("merchant2"))),
                        jsonPath("$..id").value(IsIterableContaining.hasItems(6, 8))
                );
    }

    @Sql("/sql/transaction-filter.sql")
    @Test
    void shouldReturnTransactionByMerchant1() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions?start_date={1}&end_date={2}",
                                ZonedDateTime.of(2026, 3, 3, 0, 0, 0, 0, ZoneOffset.UTC),
                                ZonedDateTime.of(2026, 3, 4, 0, 0, 0, 0, ZoneOffset.UTC)
                        )
                        // merchant 1 auth
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ="))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$..merchantId").value(Every.everyItem(IsEqual.equalTo("merchant1"))),
                        jsonPath("$..id").value(IsIterableContaining.hasItem(7)),
                        jsonPath("$..currency").value("EUR"),
                        jsonPath("$..status").value("SUCCESS")
                );
    }

    @Test
    void whenGetTransactions_withoutRequiredParams_shouldReturn400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ="))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGetTransactions_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetTransactions_withInvalidAuth_shouldReturn401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOnBhc3M="))
                .andExpect(status().isUnauthorized());
    }
}

