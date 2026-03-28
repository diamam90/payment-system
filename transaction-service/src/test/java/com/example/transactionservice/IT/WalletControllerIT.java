package com.example.transactionservice.IT;

import com.example.transaction.dto.WalletResponse;
import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.config.KafkaTestConfig;
import com.example.transactionservice.config.SecurityTestConfig;
import com.example.transactionservice.service.WalletService;
import com.example.transactionservice.util.KeycloakUtils;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Import({KafkaTestConfig.class,
        SecurityTestConfig.class,
        DatabaseTestConfig.class})
public class WalletControllerIT {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    SecurityTestConfig securityTestConfig;
    @MockitoSpyBean
    WalletService walletService;
    ParameterizedTypeReference<List<WalletResponse>> WALLET_LIST_TYPE = new ParameterizedTypeReference<>() {};

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void shouldCreateWalletAndGetById() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(createRubWalletRequest(), headers);
        // when
        ResponseEntity<WalletResponse> actual = restTemplate.exchange("/api/v1/wallets", HttpMethod.POST, request, WalletResponse.class);

        UUID walletId = actual.getBody().getUid();
        request = new HttpEntity<>(headers);
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertThat(actual.getBody())
                .hasFieldOrPropertyWithValue("balance", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .hasFieldOrPropertyWithValue("userUid", USER_ID)
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("type.uid", UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00af"))
                .hasFieldOrPropertyWithValue("type.name", "my wallet")
                .hasFieldOrPropertyWithValue("type.currencyCode", "RUB")
                .hasFieldOrPropertyWithValue("type.status", "active")
                .hasFieldOrPropertyWithValue("type.userType", "individual")
                .hasFieldOrPropertyWithValue("name", "my RUB wallet");

        ResponseEntity<WalletResponse> actual2 = restTemplate.exchange(
                "/api/v1/wallets/{walletId}",
                HttpMethod.GET,
                request,
                WalletResponse.class,
                walletId
        );

        assertEquals(actual.getBody(), actual2.getBody());
    }

    @Test
    void shouldCreate2WalletAndGetByUserId() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestRubWallet = new HttpEntity<>(createRubWalletRequest(), headers);
        HttpEntity<String> requestUsdWallet = new HttpEntity<>(createUsdWalletRequest(), headers);
        // when
        restTemplate.exchange("/api/v1/wallets", HttpMethod.POST, requestRubWallet, WalletResponse.class);
        restTemplate.exchange("/api/v1/wallets", HttpMethod.POST, requestUsdWallet, WalletResponse.class);

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<List<WalletResponse>> actual = restTemplate.exchange(
                "/api/v1/wallets/user/{userId}",
                HttpMethod.GET,
                request,
                WALLET_LIST_TYPE,
                USER_ID
        );

        assertThat(actual.getBody())
                .hasSize(2).extracting("name", "type.uid", "userUid", "balance")
                .contains(new Tuple(
                                "my RUB wallet",
                                UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00af"),
                                USER_ID,
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        ),
                        new Tuple(
                                "my USD wallet",
                                UUID.fromString("b793b996-a439-4db8-b0a3-bb6ac099b3f6"),
                                USER_ID,
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        )
                );
    }

    private String createRubWalletRequest() {
        return  // language=JSON
                """
                        {
                          "name": "my RUB wallet",
                          "walletTypeUid": "fa65903f-2441-4ada-81fa-d8cd6a2e00af",
                          "userUid": "00000000-0000-0000-0000-000000000001"
                        }
                        """;
    }

    private String createUsdWalletRequest() {
        return  // language=JSON
                """
                        {
                          "name": "my USD wallet",
                          "walletTypeUid": "b793b996-a439-4db8-b0a3-bb6ac099b3f6",
                          "userUid": "00000000-0000-0000-0000-000000000001"
                        }
                        """;
    }

    private String authToken() {
        return KeycloakUtils.adminToken(securityTestConfig.getKeycloakServerUrl()).getToken();
    }
}
