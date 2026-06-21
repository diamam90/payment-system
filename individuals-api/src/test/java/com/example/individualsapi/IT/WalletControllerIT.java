package com.example.individualsapi.IT;

import com.example.individuals.dto.WalletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfDockerAvailable
@AutoConfigureWebTestClient
public class WalletControllerIT extends BaseIntegrationTest {

    @Autowired
    WebTestClient client;

    private static final UUID individualId = UUID.fromString("00000000-0000-0000-0000-000011110000");

    @Test
    void shouldCreate() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();

        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        // when
        WalletResponse walletResponse = client.post()
                .uri("/api/v1/wallets")
                .bodyValue(createWalletRequest(individualId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(WalletResponse.class)
                .getResponseBody()
                .blockFirst();
        // then
        assertThat(walletResponse)
                .hasFieldOrPropertyWithValue("uid", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .hasFieldOrPropertyWithValue("name", "IT test wallet")
                .hasFieldOrPropertyWithValue("type.uid", UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .hasFieldOrPropertyWithValue("type.name", "WALLET TYPE IT name")
                .hasFieldOrPropertyWithValue("type.currencyCode", "EUR")
                .hasFieldOrPropertyWithValue("type.status", "active")
                .hasFieldOrPropertyWithValue("type.userType", "test type")
                .hasFieldOrPropertyWithValue("userUid", individualId)
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-05-05T10:00:00Z"))
                .hasFieldOrPropertyWithValue("createdAt", ZonedDateTime.parse("2025-05-05T11:00:00Z"))
                .hasFieldOrPropertyWithValue("updatedAt", ZonedDateTime.parse("2025-05-05T12:00:00Z"));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void shouldGetByUid() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();

        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        // when
        WalletResponse walletResponse = client.get()
                .uri("/api/v1/wallets/{walletUid}", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .returnResult(WalletResponse.class)
                .getResponseBody()
                .blockFirst();

        assertThat(walletResponse)
                .hasFieldOrPropertyWithValue("uid", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .hasFieldOrPropertyWithValue("name", "IT test wallet")
                .hasFieldOrPropertyWithValue("type.uid", UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .hasFieldOrPropertyWithValue("type.name", "WALLET TYPE IT name")
                .hasFieldOrPropertyWithValue("type.currencyCode", "EUR")
                .hasFieldOrPropertyWithValue("type.status", "active")
                .hasFieldOrPropertyWithValue("type.userType", "test type")
                .hasFieldOrPropertyWithValue("userUid", individualId)
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-05-05T10:00:00Z"))
                .hasFieldOrPropertyWithValue("createdAt", ZonedDateTime.parse("2025-05-05T11:00:00Z"))
                .hasFieldOrPropertyWithValue("updatedAt", ZonedDateTime.parse("2025-05-05T12:00:00Z"));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void shouldGetByUserId() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();

        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        // when
        List<WalletResponse> walletResponse = client.get()
                .uri("/api/v1/wallets/user/{userId}", individualId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<WalletResponse>>() {
                })
                .getResponseBody()
                .blockFirst();

        assertThat(walletResponse)
                .hasSize(1)
                .element(0)
                .hasFieldOrPropertyWithValue("uid", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .hasFieldOrPropertyWithValue("name", "IT test wallet")
                .hasFieldOrPropertyWithValue("type.uid", UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .hasFieldOrPropertyWithValue("type.name", "WALLET TYPE IT name")
                .hasFieldOrPropertyWithValue("type.currencyCode", "EUR")
                .hasFieldOrPropertyWithValue("type.status", "active")
                .hasFieldOrPropertyWithValue("type.userType", "test type")
                .hasFieldOrPropertyWithValue("userUid", individualId)
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_EVEN))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-05-05T10:00:00Z"))
                .hasFieldOrPropertyWithValue("createdAt", ZonedDateTime.parse("2025-05-05T11:00:00Z"))
                .hasFieldOrPropertyWithValue("updatedAt", ZonedDateTime.parse("2025-05-05T12:00:00Z"));

        deleteUser(userId, adminToken).block();
    }

    private String createWalletRequest(UUID userId) {
        return """
                {
                   "name": "IT test wallet",
                   "walletTypeUid": "22222222-2222-2222-2222-222222222222",
                   "userUid": "%s"
                }
                """.formatted(userId);
    }
}
