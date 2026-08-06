package com.example.IT;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PayoutIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @AfterEach
    void truncate() {
        jdbc.sql("TRUNCATE TABLE payment.payouts CASCADE").update();
    }

    @Test
    void shouldCreatePayout() throws Exception {
        mvc.perform(post("/api/v1/payouts")
                        // merchant 1
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ=")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(//language=JSON
                                """
                                        {
                                          "amount": 66.6,
                                          "currency": "USD"
                                        }
                                        """))
                .andExpectAll(status().isCreated(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.merchantId").value("merchant1"),
                        jsonPath("$.amount").value(66.6),
                        jsonPath("$.status").value("PENDING"),
                        jsonPath("$.currency").value("USD"),
                        jsonPath("$.createdAt").isNotEmpty()
                );
    }

    @Sql("/sql/payout-by-id.sql")
    @Test
    void shouldFindPayoutById() throws Exception {
        mvc.perform(get("/api/v1/payouts/23")
                        // merchant 1
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ="))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(23),
                        jsonPath("$.merchantId").value("merchant1"),
                        jsonPath("$.amount").value(6.6),
                        jsonPath("$.status").value("FAILED"),
                        jsonPath("$.currency").value("EUR"),
                        jsonPath("$.createdAt").value("2027-01-01T12:00:00Z")
                );
    }

    @Test
    void getById_whenPayoutNotFound_shouldReturn404() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/payouts/444")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ="))
                .andExpectAll(status().isNotFound(),
                        jsonPath("error").value("ERROR_404"),
                        jsonPath("message").value("Payout with identifier [444] not found")
                );
    }

    @Test
    void whenGetPayouts_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/api/v1/payouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetPayouts_withInvalidAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/api/v1/payouts")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOnBhc3M="))
                .andExpect(status().isUnauthorized());
    }
}
