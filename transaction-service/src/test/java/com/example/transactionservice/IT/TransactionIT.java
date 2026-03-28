package com.example.transactionservice.IT;

import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.TransactionStatusResponse;
import com.example.transaction.dto.WalletResponse;
import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.config.KafkaTestConfig;
import com.example.transactionservice.config.SecurityTestConfig;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.util.KeycloakUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Import({KafkaTestConfig.class,
        SecurityTestConfig.class,
        DatabaseTestConfig.class})
public class TransactionIT {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    SecurityTestConfig securityTestConfig;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    Clock clock;

    @Test
    void shouldDeposit() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal amount = BigDecimal.valueOf(250).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedFee = amount.multiply(BigDecimal.valueOf(0.228d * 0.01d)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedAccrual = amount.subtract(expectedFee);
        // create wallet
        HttpEntity<String> walletRequest = new HttpEntity<>(createRubWalletRequest(), headers);
        WalletResponse wallet = restTemplate.exchange("/api/v1/wallets", POST, walletRequest, WalletResponse.class)
                .getBody();

        UUID walletId = wallet.getUid();
        // deposit init
        HttpEntity<String> depositInitRequest = new HttpEntity<>(depositInitRequest(walletId), headers);

        TransactionInitResponse initResponse = restTemplate
                .exchange("/api/v1/transactions/DEPOSIT/init", POST, depositInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = new HttpEntity<>(depositConfirmRequest(walletId), headers);
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange("/api/v1/transactions/DEPOSIT/confirm", POST, depositConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before kafka confirmed
        TransactionStatusResponse beforeKafkaConfirmStatus = restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        assertThat(beforeKafkaConfirmStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // check transaction status before kafka confirmed
        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(transactionId, "COMPLETED", null, BigDecimal.valueOf(250), ZonedDateTime.now(clock));
        kafkaTemplate.send("transaction.complete", completedEvent);

        Callable<TransactionStatusResponse> statusCallable = () -> restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        Awaitility.await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(statusCallable,
                        response -> assertThat(response)
                                .isNotNull()
                                .hasFieldOrPropertyWithValue("status", TransactionStatus.COMPLETED.name())
                                .extracting("uid").isNotNull()
                );
        // check wallet balance
        Callable<WalletResponse> walletCallable = () -> restTemplate
                .exchange("/api/v1/wallets/{walletId}", HttpMethod.GET, new HttpEntity<>(headers), WalletResponse.class, walletId)
                .getBody();

        Awaitility.await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(walletCallable,
                        response -> assertThat(response)
                                .isNotNull()
                                .hasFieldOrPropertyWithValue("balance", expectedAccrual)
                );
    }

    private String adminToken() {
        return KeycloakUtils.adminToken(securityTestConfig.getKeycloakServerUrl()).getToken();
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

    private String depositInitRequest(UUID walletId) {
        return //language=JSON
                """
                        {
                          "type": "DEPOSIT",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "%s",
                          "amount": 250
                        }
                        """.formatted(walletId);
    }

    private String depositConfirmRequest(UUID walletId) {
        return //language=JSON
                """
                        {
                          "type": "DEPOSIT",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "%s",
                          "amount": 250,
                          "comment": "На еду"
                        }
                        """.formatted(walletId);
    }
}
