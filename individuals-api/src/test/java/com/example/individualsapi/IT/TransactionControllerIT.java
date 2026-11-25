package com.example.individualsapi.IT;

import com.example.currency.api.CurrencyRateApi;
import com.example.individuals.dto.TransactionInitResponse;
import com.example.individualsapi.dto.keycloak.KeycloakTokenResponse;
import com.example.individualsapi.service.PaymentService;
import com.example.individualsapi.service.TransactionService;
import com.example.transaction.api.TransactionApiClient;
import com.example.transaction.dto.TransferInitRequest;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnabledIfDockerAvailable
@AutoConfigureWebTestClient
public class TransactionControllerIT extends BaseIntegrationTest {

    @Autowired
    WebTestClient client;
    @MockitoSpyBean
    CurrencyRateApi currencyRateApiClient;
    @MockitoSpyBean
    TransactionApiClient transactionApiClient;
    @MockitoSpyBean
    PaymentService paymentService;
    @MockitoSpyBean
    TransactionService transactionService;

    @Captor
    ArgumentCaptor<TransferInitRequest> transferInitRequestArgumentCaptor;
    @MockitoBean
    Clock clock;

    Clock fixed = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);

    private static final UUID individualId = UUID.fromString("00000000-0000-0000-0000-000011110000");

    @Test
    void depositInit() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();
        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        var walletUid = UUID.fromString("12341234-1234-1234-1234-123412341234");
        // when
        TransactionInitResponse response = client.post()
                .uri("/api/v1/transactions/init")
                .bodyValue(depositInitRequest(individualId, walletUid))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(TransactionInitResponse.class)
                .getResponseBody()
                .blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("walletUid", walletUid)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(50.66).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(2.33).setScale(2, RoundingMode.HALF_EVEN));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void withdrawalInit() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();
        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        var walletUid = UUID.fromString("12341234-1234-1234-1234-123412341234");
        // when
        TransactionInitResponse response = client.post()
                .uri("/api/v1/transactions/init")
                .bodyValue(withdrawalInitRequest(individualId, walletUid))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(TransactionInitResponse.class)
                .getResponseBody()
                .blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("walletUid", walletUid)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(10.66).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(33.33).setScale(2, RoundingMode.HALF_EVEN));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void transferInit_withoutInteractionCurrencyRateService() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();
        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();

        var walletUid = UUID.fromString("12341234-1234-1234-1234-123412341234");
        var targetUserId = UUID.fromString("00000000-0000-0000-0000-000011110001");
        var targetWalletUid = walletUid; // same currency code
        // when
        when(clock.instant()).thenReturn(fixed.instant());
        when(clock.getZone()).thenReturn(fixed.getZone());

        TransactionInitResponse response = client.post()
                .uri("/api/v1/transactions/init")
                .bodyValue(transferInitRequest(individualId, walletUid, targetUserId, targetWalletUid))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(TransactionInitResponse.class)
                .getResponseBody()
                .blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("walletUid", walletUid)
                .hasFieldOrPropertyWithValue("targetWalletUid", targetWalletUid)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(256).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_EVEN));
        verify(transactionApiClient).transactionInit(transferInitRequestArgumentCaptor.capture());
        assertThat(transferInitRequestArgumentCaptor.getValue())
                .hasFieldOrPropertyWithValue("rate", BigDecimal.ONE);

        verify(currencyRateApiClient, never()).getRateByFilter(any(), any(), any());

        deleteUser(userId, adminToken).block();
    }

    @Test
    void transferInit_withInteractionCurrencyRateService() {
        // given
        var sourceCurrency = "EUR";
        var targetCurrency = "USD";
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();
        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();

        var walletUid = UUID.fromString("12341234-1234-1234-1234-123412341234");
        var targetUserId = UUID.fromString("00000000-0000-0000-0000-000011110001");
        var targetWalletUid = UUID.fromString("43214321-4321-4321-4321-432143214321");
        // when
        when(clock.instant()).thenReturn(fixed.instant());
        when(clock.getZone()).thenReturn(fixed.getZone());

        TransactionInitResponse response = client.post()
                .uri("/api/v1/transactions/init")
                .bodyValue(transferInitRequest(individualId, walletUid, targetUserId, targetWalletUid))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(TransactionInitResponse.class)
                .getResponseBody()
                .blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("walletUid", walletUid)
                .hasFieldOrPropertyWithValue("targetWalletUid", targetWalletUid)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(256).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_EVEN));
        verify(transactionApiClient).transactionInit(transferInitRequestArgumentCaptor.capture());
        assertThat(transferInitRequestArgumentCaptor.getValue())
                .hasFieldOrPropertyWithValue("rate", BigDecimal.valueOf(23.04));

        verify(currencyRateApiClient).getRateByFilter(sourceCurrency, targetCurrency, ZonedDateTime.now(clock));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void depositConfirm() {
        String adminToken = adminToken().block();
        UUID userUid = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        String keycloakUserId = createUser("user@aaa.ru", "34222", adminToken, userUid).block();
        KeycloakTokenResponse tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        String userToken = tokenResponse.accessToken();
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID walletId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String depositConfirmRequest = depositConfirmRequest(userUid, walletId);

        client.post()
                .uri("/api/v1/transactions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(depositConfirmRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.uid").value(IsEqual.equalTo("00000000-0000-0000-0000-000000000001"))
                .jsonPath("$.userUid").value(IsEqual.equalTo("dddddddd-dddd-dddd-dddd-dddddddddddd"))
                .jsonPath("$.type").value(IsEqual.equalTo("DEPOSIT"))
                .jsonPath("$.status").value(IsEqual.equalTo("SUCCESS"));

        verify(transactionService).success(transactionId, "prosto transaction id", userUid);
        verify(transactionService, never()).failed(any(), any());
        deleteUser(keycloakUserId, adminToken).block();
    }

    @Test
    void deposit_whenPaymentServiceUnavailable_shouldCompensateAndReturn502() {
        String adminToken = adminToken().block();
        UUID userUid = UUID.fromString("66666666-6666-6666-6666-666666666666");
        String keycloakUserId = createUser("user@aaa.ru", "34222", adminToken, userUid).block();
        KeycloakTokenResponse tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        String userToken = tokenResponse.accessToken();
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID walletId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String depositConfirmRequest = depositConfirmRequest(userUid, walletId);

        client.mutateWith((builder, handler, connection) -> builder.responseTimeout(Duration.ofSeconds(30)).build())
                .post()
                .uri("/api/v1/transactions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(depositConfirmRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatusCode.valueOf(502))
                .expectBody()
                .jsonPath("$.error").value(IsEqual.equalTo("Service Payment-service unavailable"))
                .jsonPath("$.status").value(IsEqual.equalTo(502));

        verify(transactionService).failed(transactionId, userUid);
        verify(transactionService, never()).success(any(), any(), any());
        deleteUser(keycloakUserId, adminToken).block();
    }

    @Test
    void withdrawalConfirm() {
        String adminToken = adminToken().block();
        UUID userUid = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        String keycloakUserId = createUser("user@aaa.ru", "34222", adminToken, userUid).block();
        KeycloakTokenResponse tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        String userToken = tokenResponse.accessToken();
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID walletId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        String depositConfirmRequest = withdrawalConfirmRequest(userUid, walletId);

        client.post()
                .uri("/api/v1/transactions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(depositConfirmRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.uid").value(IsEqual.equalTo("00000000-0000-0000-0000-000000000002"))
                .jsonPath("$.userUid").value(IsEqual.equalTo("cccccccc-cccc-cccc-cccc-cccccccccccc"))
                .jsonPath("$.type").value(IsEqual.equalTo("WITHDRAWAL"))
                .jsonPath("$.status").value(IsEqual.equalTo("SUCCESS"));

        verify(transactionService).success(transactionId, "prosto transaction id", userUid);
        verify(transactionService, never()).failed(any(), any());
        deleteUser(keycloakUserId, adminToken).block();
    }

    @Test
    void withdrawalConfirm_whenPaymentServiceUnavailable_shouldCompensateAndReturn502() {
        String adminToken = adminToken().block();
        UUID userUid = UUID.fromString("66666666-6666-6666-6666-666666666666");
        String keycloakUserId = createUser("user@aaa.ru", "34222", adminToken, userUid).block();
        KeycloakTokenResponse tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        String userToken = tokenResponse.accessToken();
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID walletId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        String depositConfirmRequest = withdrawalConfirmRequest(userUid, walletId);

        client.post()
                .uri("/api/v1/transactions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(depositConfirmRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatusCode.valueOf(502))
                .expectBody()
                .jsonPath("$.error").value(IsEqual.equalTo("Service Payment-service unavailable"))
                .jsonPath("$.status").value(IsEqual.equalTo(502));

        verify(transactionService).failed(transactionId, userUid);
        verify(transactionService, never()).success(any(), any(), any());
        deleteUser(keycloakUserId, adminToken).block();
    }

    private String depositInitRequest(UUID userId, UUID walletId) {
        return  // language=JSON
                """
                        {
                            "type": "DEPOSIT",
                            "userUid": "%s",
                            "walletUid": "%s",
                            "amount": "50.66"
                        }
                        """.formatted(userId, walletId);
    }

    private String withdrawalInitRequest(UUID userId, UUID walletId) {
        return  // language=JSON
                """
                        {
                            "type": "WITHDRAWAL",
                            "userUid": "%s",
                            "walletUid": "%s",
                            "amount": "10.66"
                        }
                        """.formatted(userId, walletId);
    }

    private String transferInitRequest(UUID userId, UUID walletId, UUID targetUserId, UUID targetWalletId) {
        return // language=JSON
                """
                        {
                          "type": "TRANSFER",
                          "userUid": "%s",
                          "walletUid": "%s",
                          "amount": "256",
                          "targetUserUid": "%s",
                          "targetWalletUid": "%s"
                        }
                        """.formatted(userId, walletId, targetUserId, targetWalletId);
    }

    private String depositConfirmRequest(UUID userId, UUID walletId) {
        return """
                {
                    "type": "DEPOSIT",
                    "userUid": "%s",
                    "walletUid": "%s",
                    "amount": "40.44",
                    "comment": "deposit da"
                }
                """.formatted(userId, walletId);
    }

    private String withdrawalConfirmRequest(UUID userId, UUID walletId) {
        return """
                {
                    "type": "WITHDRAWAL",
                    "userUid": "%s",
                    "walletUid": "%s",
                    "amount": 555,
                    "comment": "this it withdrawal!"
                }
                """.formatted(userId, walletId);
    }
}
