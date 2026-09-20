package com.example.IT;

import com.example.fake.dto.TransactionResponse;
import com.example.repository.PayoutRepository;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.Condition;
import org.hamcrest.core.Every;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockitoBean
    Clock clock;

    Clock fixed = Clock.fixed(Instant.parse("2027-05-05T12:00:00+04:00"), ZoneOffset.UTC);

    @MockitoSpyBean
    private PayoutRepository payoutRepository;

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
                                          "amount": 66.99,
                                          "currency": "USD"
                                        }
                                        """))
                .andExpectAll(status().isCreated(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.merchantId").value("merchant2"),
                        jsonPath("$.amount").value(66.99),
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
                        jsonPath("$.amount").value(66.99),
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

    @Test
    void payout_withInvalidAmountScale_shouldReturn400() throws Exception {
        mvc.perform(post("/api/v1/payouts")
                        // merchant 2
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQyOm1lcmNoYW50IDIgcGFzc3dvcmQ=")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(//language=JSON
                                """
                                        {
                                          "amount": 66.999,
                                          "currency": "USD"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void transaction_whenAmountDigitGreaterThan16_shouldReturn400() throws Exception {
        mvc.perform(post("/api/v1/payouts")
                        // merchant 2
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQyOm1lcmNoYW50IDIgcGFzc3dvcmQ=")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(//language=JSON
                                """
                                        {
                                          "amount": 12345678901234567.99,
                                          "currency": "USD"
                                        }
                                        """))
                .andExpectAll(status().isBadRequest(),
                        jsonPath("$.error").value("ERROR_400"),
                        jsonPath("$.message").exists(),
                        jsonPath("$.detail").doesNotExist(),
                        jsonPath("$.instance").doesNotExist(),
                        jsonPath("$.status").doesNotExist(),
                        jsonPath("$.title").doesNotExist(),
                        content().contentType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON))
                .andDo(print());

        verify(payoutRepository, never()).save(any());
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

    @Test
    void payout_withInvalidCurrencyRequestValue_shouldReturn400() throws Exception {
        // create payout
        mvc.perform(post("/api/v1/payouts")
                        // merchant 2
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQyOm1lcmNoYW50IDIgcGFzc3dvcmQ=")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(//language=JSON
                                """
                                        {
                                          "amount": 66.99,
                                          "currency": "US"
                                        }
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Sql("/sql/payout-filter.sql")
    @Test
    void findPayout_whenStartDateNull_shouldFindFromStartYear() throws Exception {
        mvc.perform(get("/api/v1/payouts?end_date={end}",
                        ZonedDateTime.parse("2027-05-05T12:00:00Z"))
                        // merchant 1
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ=")
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$..merchantId").value(Every.everyItem(IsEqual.equalTo("merchant1"))),
                        jsonPath("$..id").value(IsIterableContaining.hasItems(12, 13))
                );
    }

    @Sql("/sql/payout-filter.sql")
    @Test
    void findPayout_whenEndDateNull_shouldFindForEndYear() throws Exception {
        when(clock.getZone()).thenReturn(fixed.getZone());
        when(clock.instant()).thenReturn(fixed.instant());

        mvc.perform(get("/api/v1/payouts?start_date={start}",
                        ZonedDateTime.parse("2027-05-05T15:00:00+03:00"))
                        // merchant 1
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ=")
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(3),
                        jsonPath("$..merchantId").value(Every.everyItem(IsEqual.equalTo("merchant1"))),
                        jsonPath("$..id").value(IsIterableContaining.hasItems(13, 14, 15))
                );
    }

    @Sql("/sql/payout-filter.sql")
    @Test
    void findPayout() throws Exception {
        mvc.perform(get("/api/v1/payouts?start_date={start}",
                        ZonedDateTime.parse("2027-01-01T03:00:00+03:00"),
                        ZonedDateTime.parse("2027-12-31T23:59:59Z"))
                        // merchant 1
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ=")
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(4),
                        jsonPath("$..merchantId").value(Every.everyItem(IsEqual.equalTo("merchant1"))),
                        jsonPath("$..id").value(IsIterableContaining.hasItems(12, 13, 14, 15))
                );
    }

    @Test
    void findPayout_whenStartDateIsAfterEndDate_shouldReturn400() throws Exception {
        mvc.perform(get("/api/v1/payouts?start_date={end}&end_date={start}",
                        ZonedDateTime.parse("2028-01-01T03:00:00+03:00"),
                        ZonedDateTime.parse("2027-12-31T23:59:59Z"))
                        // merchant 1
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + "bWVyY2hhbnQxOm1lcmNoYW50IDEgcGFzc3dvcmQ=")
                )
                .andExpect(status().isBadRequest());
    }
}