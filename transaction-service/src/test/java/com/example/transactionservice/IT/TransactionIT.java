package com.example.transactionservice.IT;

import com.example.transaction.dto.*;
import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.config.KafkaTestConfig;
import com.example.transactionservice.config.SecurityTestConfig;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.entity.TransactionType;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.repository.TransactionRepository;
import com.example.transactionservice.repository.WalletRepository;
import com.example.transactionservice.stub.ConfirmStringRequestStub;
import com.example.transactionservice.stub.InitStringRequestStub;
import com.example.transactionservice.util.KeycloakUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Spy
    TransactionRepository transactionRepository;
    @Spy
    WalletRepository walletRepository;
    @Autowired
    JdbcOperations jdbc;

    @AfterEach
    void truncate() {
        jdbc.execute("TRUNCATE transaction.wallets CASCADE");
    }

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
        HttpEntity<String> depositInitRequest = new HttpEntity<>(InitStringRequestStub.depositInitRequest(walletId), headers);

        TransactionInitResponse initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, depositInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.depositConfirmRequest(walletId), headers);
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, depositConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before kafka confirmed
        TransactionStatusResponse beforeKafkaConfirmStatus = restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        assertThat(beforeKafkaConfirmStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // check transaction status after kafka confirmed
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

    @Sql("/sql/deposit-inactive.sql")
    @Test
    void deposit_whenWalletIsInactive_shouldReturnBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        UUID walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        // deposit init
        HttpEntity<String> depositInitRequest = new HttpEntity<>(InitStringRequestStub.depositInitRequest(walletId), headers);

        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, depositInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "Wallet with Uid  [%s] is not available for DEPOSIT operation".formatted(walletId))
                .hasFieldOrPropertyWithValue("status", 400);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.depositConfirmRequest(walletId), headers);
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, depositConfirmRequest, ErrorResponse.class);
        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "Wallet with Uid  [%s] is not available for DEPOSIT operation".formatted(walletId))
                .hasFieldOrPropertyWithValue("status", 400);

        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void depositFailed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal amount = BigDecimal.valueOf(250).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedFee = amount.multiply(BigDecimal.valueOf(0.228d * 0.01d)).setScale(2, RoundingMode.HALF_UP);
        // create wallet
        HttpEntity<String> walletRequest = new HttpEntity<>(createRubWalletRequest(), headers);
        WalletResponse wallet = restTemplate.exchange("/api/v1/wallets", POST, walletRequest, WalletResponse.class)
                .getBody();

        UUID walletId = wallet.getUid();
        // deposit init
        HttpEntity<String> depositInitRequest = new HttpEntity<>(InitStringRequestStub.depositInitRequest(walletId), headers);

        TransactionInitResponse initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, depositInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.depositConfirmRequest(walletId), headers);
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, depositConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before kafka confirmed
        TransactionStatusResponse beforeKafkaConfirmStatus = restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        assertThat(beforeKafkaConfirmStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // check transaction status after kafka confirmed
        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(transactionId, "FAILED", "Service is unavailable", BigDecimal.valueOf(250), ZonedDateTime.now(clock));
        kafkaTemplate.send("transaction.complete", completedEvent);

        Callable<TransactionStatusResponse> statusCallable = () -> restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        Awaitility.await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(statusCallable,
                        response -> assertThat(response)
                                .isNotNull()
                                .hasFieldOrPropertyWithValue("status", TransactionStatus.FAILED.name())
                                .hasFieldOrPropertyWithValue("failureReason", "Service is unavailable")
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
                                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP))
                );
    }


    @Sql("/sql/withdrawal.sql")
    @Test
    void shouldWithdrawal() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal balance = BigDecimal.valueOf(99.06);
        BigDecimal amount = BigDecimal.valueOf(40.00);
        BigDecimal expectedFee = amount.multiply(BigDecimal.valueOf(0.0228d * 0.02d)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSum = amount.add(expectedFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedBalance = balance.subtract(totalSum);

        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = new HttpEntity<>(InitStringRequestStub.withdrawalInitRequest(amount), headers);

        TransactionInitResponse initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, withdrawalInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", totalSum)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.withdrawalConfirmRequest(amount), headers);
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, withdrawalConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before kafka confirmed
        TransactionStatusResponse beforeKafkaConfirmStatus = restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        assertThat(beforeKafkaConfirmStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // check transaction status after kafka confirmed
        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(transactionId, "COMPLETED", null, amount, ZonedDateTime.now(clock));
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
                .exchange("/api/v1/wallets/{walletId}", HttpMethod.GET, new HttpEntity<>(headers), WalletResponse.class, UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1"))
                .getBody();

        Awaitility.await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(walletCallable,
                        response -> assertThat(response)
                                .isNotNull()
                                .hasFieldOrPropertyWithValue("balance", expectedBalance)
                );
    }

    @Sql("/sql/withdrawal.sql")
    @Test
    void withdrawalFailed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal balance = BigDecimal.valueOf(99.06);
        BigDecimal amount = BigDecimal.valueOf(40.00);
        BigDecimal expectedFee = amount.multiply(BigDecimal.valueOf(0.0228d * 0.02d)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSum = amount.add(expectedFee).setScale(2, RoundingMode.HALF_UP);

        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = new HttpEntity<>(InitStringRequestStub.withdrawalInitRequest(amount), headers);

        ResponseEntity<TransactionInitResponse> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, withdrawalInitRequest, TransactionInitResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", totalSum)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.withdrawalConfirmRequest(amount), headers);
        ResponseEntity<TransactionConfirmResponse> confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, withdrawalConfirmRequest, TransactionConfirmResponse.class);

        UUID transactionId = confirmResponse.getBody().getUid();
        // check transaction status before kafka confirmed
        TransactionStatusResponse beforeKafkaConfirmStatus = restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        assertThat(beforeKafkaConfirmStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // check transaction status after kafka confirmed
        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(transactionId, "FAILED", "ERROR", amount, ZonedDateTime.now(clock));
        kafkaTemplate.send("transaction.complete", completedEvent);

        Callable<TransactionStatusResponse> statusCallable = () -> restTemplate
                .exchange("/api/v1/transactions/{transactionId}/status", GET, new HttpEntity<>(headers), TransactionStatusResponse.class, transactionId)
                .getBody();

        Awaitility.await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(statusCallable,
                        response -> assertThat(response)
                                .isNotNull()
                                .hasFieldOrPropertyWithValue("status", TransactionStatus.FAILED.name())
                                .hasFieldOrPropertyWithValue("failureReason", "ERROR")
                                .extracting("uid").isNotNull()
                );
        // check wallet balance
        Callable<WalletResponse> walletCallable = () -> restTemplate
                .exchange("/api/v1/wallets/{walletId}", HttpMethod.GET, new HttpEntity<>(headers), WalletResponse.class, UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1"))
                .getBody();

        Awaitility.await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(walletCallable,
                        response -> assertThat(response)
                                .isNotNull()
                                .hasFieldOrPropertyWithValue("balance", balance)
                );
    }


    @Sql("/sql/withdrawal-inactive.sql")
    @Test
    void withdrawal_whenWalletIsInactive_shouldReturnBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        UUID walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        BigDecimal amount = BigDecimal.valueOf(40.00);
        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = new HttpEntity<>(InitStringRequestStub.withdrawalInitRequest(amount), headers);

        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, withdrawalInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "Wallet with Uid  [%s] is not available for WITHDRAWAL operation".formatted(walletId))
                .hasFieldOrPropertyWithValue("status", 400);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.withdrawalConfirmRequest(amount), headers);
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, withdrawalConfirmRequest, ErrorResponse.class);

        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "Wallet with Uid  [%s] is not available for WITHDRAWAL operation".formatted(walletId))
                .hasFieldOrPropertyWithValue("status", 400);
        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }


    @Sql("/sql/withdrawal.sql")
    @Test
    void withdrawal_whenNotEnoughMoney_shouldReturnBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal amount = BigDecimal.valueOf(100.00);

        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = new HttpEntity<>(InitStringRequestStub.withdrawalInitRequest(amount), headers);

        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, withdrawalInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.withdrawalConfirmRequest(amount), headers);
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, withdrawalConfirmRequest, ErrorResponse.class);
        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);

        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Sql("/sql/transfer.sql")
    @Test
    void shouldTransfer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal amount = BigDecimal.valueOf(46.06);
        BigDecimal expectedFee = BigDecimal.valueOf(0.0228 * 0.001).setScale(2, RoundingMode.HALF_UP);
        // transfer init
        HttpEntity<String> transferInitRequest = new HttpEntity<>(InitStringRequestStub.transferInitRequest(amount), headers);
        ResponseEntity<TransactionInitResponse> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, transferInitRequest, TransactionInitResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee);

        // transfer confirm
        HttpEntity<String> transferConfirmRequest = new HttpEntity<>(InitStringRequestStub.transferInitRequest(amount), headers);
        ResponseEntity<TransactionConfirmResponse> confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, transferConfirmRequest, TransactionConfirmResponse.class);

        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.COMPLETED.name());
    }

    @Sql("/sql/transfer.sql")
    @Test
    void transfer_whenNotEnoughMoney_shouldReturnBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        BigDecimal amount = BigDecimal.valueOf(246.06);
        // transfer init
        HttpEntity<String> transferInitRequest = new HttpEntity<>(InitStringRequestStub.transferInitRequest(amount), headers);
        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, transferInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);


        // transfer confirm
        HttpEntity<String> transferConfirmRequest = new HttpEntity<>(ConfirmStringRequestStub.transferConfirmRequest(amount), headers);
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange("/api/v1/transactions/confirm", POST, transferConfirmRequest, ErrorResponse.class);

        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);

        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class)
    void transactionInit_WithoutAuth_ShouldReturn401(TransactionType type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> initResponse = restTemplate
                .exchange("/api/v1/transactions/init", POST, new HttpEntity<>(headers), Void.class);
        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
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
}
