package com.example.IT;

import com.example.fake.dto.TransactionResponse;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PayoutIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcClient jdbc;

    @AfterEach
    void truncate() {
        jdbc.sql("TRUNCATE TABLE payment.payouts CASCADE").update();
        jdbc.sql("TRUNCATE TABLE payment.webhooks CASCADE").update();
    }

    @Test
    void payoutCycle() throws Exception {
        // create payout
        MvcResult result = mvc.perform(post("/api/v1/payouts")
                        // merchant 2
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQyOm1lcmNoYW50IDIgcGFzc3dvcmQ=")
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
                        jsonPath("$.merchantId").value("merchant2"),
                        jsonPath("$.amount").value(66.6),
                        jsonPath("$.status").value("PENDING"),
                        jsonPath("$.currency").value("USD"),
                        jsonPath("$.createdAt").isNotEmpty()
                )
                .andReturn();

        String contentAsString = result.getResponse().getContentAsString();
        TransactionResponse response = objectMapper.readValue(contentAsString, TransactionResponse.class);

        // update transaction client2
        mvc.perform(post("/webhook/payout")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "Y2xpZW50MjpwYXNzd29yZDI=")
                        .contentType(APPLICATION_JSON)
                        .content(//language=JSON//
                                """
                                        {
                                            "id": %s,
                                            "status": "SUCCESS"
                                        }
                                        """.formatted(response.getId())))
                .andExpectAll(status().isOk());

        // get by id
        mvc.perform(get("/api/v1/payouts/{payoutId}", response.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQyOm1lcmNoYW50IDIgcGFzc3dvcmQ=")
                )
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.merchantId").value("merchant2"),
                        jsonPath("$.amount").value(66.6),
                        jsonPath("$.status").value("SUCCESS"),
                        jsonPath("$.currency").value("USD"),
                        jsonPath("$.createdAt").isNotEmpty()
                );

        // check webhook in DB
        Map<String, Object> webhookParams = jdbc.sql("SELECT * FROM payment.webhooks WHERE event_type = 'PAYOUT' and entity_id = :entityId")
                .param("entityId", response.getId())
                .query()
                .singleRow();

        assertThat(webhookParams)
                .isNotEmpty()
                .containsEntry("event_type", "PAYOUT")
                .containsEntry("entity_id", response.getId())
                .hasEntrySatisfying("payload", new Condition<>(Objects::nonNull, "Payload must not be null"))
                .hasEntrySatisfying("notification_url", new Condition<>(Objects::isNull, "Notification_url must be null"));
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
