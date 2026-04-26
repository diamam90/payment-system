package com.example.transactionservice.service.impl;

import com.example.transaction.dto.*;
import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.entity.TransactionType;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.model.kafka.TransactionCreatedEvent;
import com.example.transactionservice.repository.TransactionRepository;
import com.example.transactionservice.repository.WalletRepository;
import com.example.transactionservice.service.TransactionService;
import com.example.transactionservice.stub.TransactionRequestStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import({DatabaseTestConfig.class})
class TransactionServiceImplTest {

    private static Clock fixed = Clock.fixed(Instant.parse("2026-06-06T13:00:34+03:00"), ZoneOffset.UTC);

    @Autowired
    JdbcOperations jdbc;
    @Autowired
    TransactionService transactionService;
    @MockitoSpyBean
    WalletRepository walletRepository;
    @MockitoSpyBean
    TransactionRepository transactionRepository;
    @MockitoBean
    KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    Clock clock;

    @Captor
    ArgumentCaptor<TransactionCreatedEvent> kafkaValueCaptor;
    @Captor
    ArgumentCaptor<String> kafkaKeyCaptor;

    @Value("${transaction-service.kafka.topic.out}")
    private String topic;

    private static final String TRANSACTION_BY_UID_AND_USER_UID = "SELECT * from transaction.transactions WHERE uid = ? and user_uid = ?";
    private static final String WALLET_BY_UID_AND_USER_UID = "SELECT * from transaction.wallets WHERE uid = ? and user_uid = ?";

    private static final BigDecimal RUB_DEPOSIT_FEE = BigDecimal.valueOf(0.228 * 0.01);
    private static final BigDecimal USD_TRANSFER_FEE = BigDecimal.valueOf(0.0228 * 0.001);
    private static final BigDecimal USD_WITHDRAWAL_FEE = BigDecimal.valueOf(0.0228 * 0.02);

    @AfterEach
    void truncateTable() {
        jdbc.execute("TRUNCATE TABLE transaction.wallets CASCADE");
    }

    @Sql("/sql/deposit.sql")
    @Test
    void depositInit() {
        // given
        DepositInitRequest request = TransactionRequestStub.depositInit();
        var expectedFee = RUB_DEPOSIT_FEE.multiply(request.getAmount()).setScale(2, HALF_UP);

        TransactionInitResponse expected = new TransactionInitResponse();
        expected.setAmount(request.getAmount().setScale(2, HALF_UP));
        expected.setFee(expectedFee);
        expected.setWalletUid(request.getWalletUid());
        // when
        TransactionInitResponse actual = transactionService.depositInit(request);
        // then
        assertThat(actual).isEqualTo(expected);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(eq(topic), any(), any());
    }


    @Sql("/sql/transfer.sql")
    @Test
    void transferInit() {
        // given
        TransferInitRequest request = TransactionRequestStub.transferInit();
        var expectedAmount = request.getAmount().setScale(2, HALF_UP);
        var expectedFee = USD_TRANSFER_FEE.multiply(expectedAmount).setScale(2, HALF_UP);

        TransactionInitResponse expected = new TransactionInitResponse();
        expected.setWalletUid(request.getWalletUid());
        expected.setFee(expectedFee);
        expected.setAmount(expectedAmount);
        expected.setTargetWalletUid(request.getTargetWalletUid());
        // when
        TransactionInitResponse actual = transactionService.transferInit(request);
        assertThat(actual).isEqualTo(expected);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(eq(topic), any(), any());
    }

    @Sql("/sql/withdrawal.sql")
    @Test
    void withdrawalInit() {
        // given
        WithdrawalInitRequest request = TransactionRequestStub.withdrawalInit();
        var expectedFee = USD_WITHDRAWAL_FEE.multiply(request.getAmount()).setScale(2, HALF_UP);
        var expectedAmount = request.getAmount().add(expectedFee).setScale(2, HALF_UP);

        TransactionInitResponse expected = new TransactionInitResponse();
        expected.setAmount(expectedAmount);
        expected.setFee(expectedFee);
        expected.setWalletUid(request.getWalletUid());
        // when
        TransactionInitResponse actual = transactionService.withdrawalInit(request);
        // then
        assertThat(actual).isEqualTo(expected);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Sql("/sql/deposit.sql")
    @Test
    void depositConfirm() {
        // given
        DepositConfirmRequest request = TransactionRequestStub.depositConfirm();

        var expectedAmount = request.getAmount().setScale(2, HALF_UP);
        var balance = BigDecimal.valueOf(50.05);
        var expectedFee = RUB_DEPOSIT_FEE.multiply(expectedAmount).setScale(2, HALF_UP);
        var expectedAccrual = expectedAmount.subtract(expectedFee).setScale(2, HALF_UP);

        TransactionConfirmResponse expected = new TransactionConfirmResponse();
        expected.setUserUid(request.getUserUid());
        expected.setWalletUid(request.getWalletUid());
        expected.setAmount(expectedAccrual);
        expected.setComment("Test Comment");
        expected.setFee(expectedFee);
        expected.setType("DEPOSIT");
        expected.setStatus("PENDING");

        TransactionCreatedEvent expectedKafkaRequest = TransactionCreatedEvent.builder()
                .amount(expectedAccrual)
                .walletId(request.getWalletUid())
                .userId(request.getUserUid())
                .currency("RUB")
                .type("DEPOSIT")
                .timestamp(ZonedDateTime.parse("2026-06-06T10:00:34Z"))
                .build();

        // when
        Mockito.when(clock.getZone()).thenReturn(fixed.getZone());
        Mockito.when(clock.instant()).thenReturn(fixed.instant());
        TransactionConfirmResponse result = transactionService.depositConfirm(request);
        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, result.getUid(), request.getUserUid());
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, request.getWalletUid(), request.getUserUid());
        // then
        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("user_uid", request.getUserUid())
                .hasFieldOrPropertyWithValue("wallet_uid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("amount", expectedAccrual)
                .hasFieldOrPropertyWithValue("type", "DEPOSIT")
                .hasFieldOrPropertyWithValue("status", "PENDING")
                .hasFieldOrPropertyWithValue("comment", "Test Comment")
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("uid", result.getUid())
                .extracting("created_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("wallet_type_uid", UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00af"))
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .hasFieldOrPropertyWithValue("balance", balance);

        assertThat(result)
                .usingRecursiveComparison().ignoringFields("uid").isEqualTo(expected);
        verify(kafkaTemplate).send(eq(topic), kafkaKeyCaptor.capture(), kafkaValueCaptor.capture());

        assertThat(kafkaKeyCaptor.getValue()).isEqualTo(result.getUid().toString());
        assertThat(kafkaValueCaptor.getValue())
                .hasFieldOrPropertyWithValue("transactionId", result.getUid())
                .usingRecursiveComparison().ignoringFields("transactionId")
                .isEqualTo(expectedKafkaRequest);
    }

    @Sql("/sql/withdrawal.sql")
    @Test
    void withdrawalConfirm() {
        // given
        WithdrawalConfirmRequest request = TransactionRequestStub.withdrawalConfirm();

        var balance = BigDecimal.valueOf(99.06);
        var expectedAmount = request.getAmount().setScale(2, HALF_UP);
        var expectedFee = USD_WITHDRAWAL_FEE.multiply(expectedAmount).setScale(2, HALF_UP);
        var expectedFinalAmount = expectedAmount.add(expectedFee).setScale(2, HALF_UP);
        var expectedBalance = balance.subtract(expectedFinalAmount).setScale(2, HALF_UP);

        TransactionCreatedEvent kafkaRequest = TransactionCreatedEvent.builder()
                .userId(request.getUserUid())
                .currency("USD")
                .timestamp(ZonedDateTime.parse("2026-06-06T10:00:34Z"))
                .amount(expectedAmount)
                .walletId(request.getWalletUid())
                .type("WITHDRAWAL")
                .build();

        // when
        Mockito.when(clock.getZone()).thenReturn(fixed.getZone());
        Mockito.when(clock.instant()).thenReturn(fixed.instant());
        TransactionConfirmResponse result = transactionService.withdrawalConfirm(request);
        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, result.getUid(), request.getUserUid());
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, request.getWalletUid(), request.getUserUid());

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("userUid", request.getUserUid())
                .hasFieldOrPropertyWithValue("walletUid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("amount", expectedAmount)
                .hasFieldOrPropertyWithValue("type", "WITHDRAWAL")
                .hasFieldOrPropertyWithValue("status", "PENDING")
                .hasFieldOrPropertyWithValue("comment", "Test comment")
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("uid", result.getUid());

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("user_uid", request.getUserUid())
                .hasFieldOrPropertyWithValue("wallet_uid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("amount", expectedAmount)
                .hasFieldOrPropertyWithValue("type", "WITHDRAWAL")
                .hasFieldOrPropertyWithValue("status", "PENDING")
                .hasFieldOrPropertyWithValue("comment", "Test comment")
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("uid", result.getUid())
                .extracting("created_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("wallet_type_uid", UUID.fromString("b793b996-a439-4db8-b0a3-bb6ac099b3f6"))
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .hasFieldOrPropertyWithValue("balance", expectedBalance);

        verify(kafkaTemplate).send(eq(topic), kafkaKeyCaptor.capture(), kafkaValueCaptor.capture());

        assertThat(kafkaKeyCaptor.getValue()).isEqualTo(result.getUid().toString());
        assertThat(kafkaValueCaptor.getValue())
                .hasFieldOrPropertyWithValue("transactionId", result.getUid())
                .usingRecursiveComparison()
                .ignoringFields("transactionId")
                .isEqualTo(kafkaRequest);
    }

    @Sql("/sql/transfer.sql")
    @Test
    void transferConfirm() {
        // given
        TransferConfirmRequest request = TransactionRequestStub.transferConfirm();

        var balance = BigDecimal.valueOf(99.06);
        var targetBalance = BigDecimal.valueOf(30.01);
        var expectedAmount = request.getAmount().setScale(2, HALF_UP);
        var expectedFee = USD_TRANSFER_FEE.multiply(expectedAmount).setScale(2, HALF_UP);
        var expectedAccrual = expectedAmount.subtract(expectedFee);

        var expectedBalance = balance.subtract(expectedAmount.add(expectedFee)).setScale(2, HALF_UP);
        var expectedTargetBalance = targetBalance.add(expectedAccrual).setScale(2, HALF_UP);
        // when
        TransactionConfirmResponse result = transactionService.transferConfirm(request);

        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, result.getUid(), request.getUserUid());
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, request.getWalletUid(), request.getUserUid());
        Map<String, Object> targetWalletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, request.getTargetWalletUid(), request.getTargetUserUid());

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("userUid", request.getUserUid())
                .hasFieldOrPropertyWithValue("walletUid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("amount", expectedAmount)
                .hasFieldOrPropertyWithValue("type", "TRANSFER")
                .hasFieldOrPropertyWithValue("status", "COMPLETED")
                .hasFieldOrPropertyWithValue("comment", null)
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("targetWalletUid", request.getTargetWalletUid())
                .extracting("uid").isNotNull();

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("user_uid", request.getUserUid())
                .hasFieldOrPropertyWithValue("wallet_uid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("amount", expectedAmount)
                .hasFieldOrPropertyWithValue("type", "TRANSFER")
                .hasFieldOrPropertyWithValue("status", "COMPLETED")
                .hasFieldOrPropertyWithValue("comment", null)
                .hasFieldOrPropertyWithValue("target_wallet_uid", request.getTargetWalletUid())
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("uid", result.getUid())
                .extracting("created_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", request.getWalletUid())
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("wallet_type_uid", UUID.fromString("b793b996-a439-4db8-b0a3-bb6ac099b3f6"))
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .hasFieldOrPropertyWithValue("balance", expectedBalance);

        assertThat(targetWalletParams)
                .hasFieldOrPropertyWithValue("uid", request.getTargetWalletUid())
                .hasFieldOrPropertyWithValue("name", "target_wallet")
                .hasFieldOrPropertyWithValue("wallet_type_uid", UUID.fromString("b793b996-a439-4db8-b0a3-bb6ac099b3f6"))
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2027-01-01 00:00:00"))
                .hasFieldOrPropertyWithValue("balance", expectedTargetBalance);

        verify(kafkaTemplate, never()).send(eq(topic), any(), any());
    }

    @Sql("/sql/fail-deposit.sql")
    @Test
    void deposit_whenFail_shouldDoNothingWithBalance() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "COMPLETED",
                "something went wrong",
                BigDecimal.valueOf(30.00),
                ZonedDateTime.parse("2026-02-02T15:00:00Z")
        );

        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");

        transactionService.fail(event);

        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, event.transactionId(), userId);
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, walletId, userId);

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("uid", event.transactionId())
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("wallet_uid", walletId)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(30.00).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(0.16).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("type", TransactionType.DEPOSIT.name())
                .hasFieldOrPropertyWithValue("status", TransactionStatus.FAILED.name())
                .hasFieldOrPropertyWithValue("comment", null)
                .hasFieldOrPropertyWithValue("failure_reason", event.failureReason())
                .hasFieldOrPropertyWithValue("created_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .extracting("updated_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", walletId)
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(50.05))
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"));
    }

    @Sql("/sql/fail-withdrawal.sql")
    @Test
    void withdrawal_whenFail_shouldIncreaseBalance() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "FAILED",
                "something went wrong",
                BigDecimal.valueOf(30.00),
                ZonedDateTime.parse("2026-02-02T15:00:00Z")
        );

        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");

        transactionService.fail(event);

        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, event.transactionId(), userId);
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, walletId, userId);

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("uid", event.transactionId())
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("wallet_uid", walletId)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(30.00).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(0.16).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("type", TransactionType.WITHDRAWAL.name())
                .hasFieldOrPropertyWithValue("status", TransactionStatus.FAILED.name())
                .hasFieldOrPropertyWithValue("comment", null)
                .hasFieldOrPropertyWithValue("failure_reason", event.failureReason())
                .hasFieldOrPropertyWithValue("created_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .extracting("updated_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", walletId)
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(50.05 + 30.00 + 0.16).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"));
    }

    @Sql("/sql/complete-deposit.sql")
    @Test
    void deposit_whenComplete_shouldIncreaseBalance() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "COMPLETED",
                null,
                BigDecimal.valueOf(30.00),
                ZonedDateTime.parse("2026-02-02T15:00:00Z")
        );

        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");

        transactionService.complete(event);

        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, event.transactionId(), userId);
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, walletId, userId);

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("uid", event.transactionId())
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("wallet_uid", walletId)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(30.00).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(0.16).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("type", TransactionType.DEPOSIT.name())
                .hasFieldOrPropertyWithValue("status", TransactionStatus.COMPLETED.name())
                .hasFieldOrPropertyWithValue("comment", null)
                .hasFieldOrPropertyWithValue("failure_reason", null)
                .hasFieldOrPropertyWithValue("created_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .extracting("updated_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", walletId)
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(50.05 + 30.00).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"));
    }

    @Sql("/sql/complete-withdrawal.sql")
    @Test
    void withdrawal_whenComplete_shouldDoNothingBalance() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "COMPLETE",
                null,
                BigDecimal.valueOf(30.00),
                ZonedDateTime.parse("2026-02-02T15:00:00Z")
        );

        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");

        transactionService.complete(event);

        Map<String, Object> transactionParams = jdbc.queryForMap(TRANSACTION_BY_UID_AND_USER_UID, event.transactionId(), userId);
        Map<String, Object> walletParams = jdbc.queryForMap(WALLET_BY_UID_AND_USER_UID, walletId, userId);

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("uid", event.transactionId())
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("wallet_uid", walletId)
                .hasFieldOrPropertyWithValue("amount", BigDecimal.valueOf(30.00).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("fee", BigDecimal.valueOf(0.16).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("type", TransactionType.WITHDRAWAL.name())
                .hasFieldOrPropertyWithValue("status", TransactionStatus.COMPLETED.name())
                .hasFieldOrPropertyWithValue("comment", null)
                .hasFieldOrPropertyWithValue("failure_reason", null)
                .hasFieldOrPropertyWithValue("created_at", Timestamp.valueOf("2030-01-01 00:00:00"))
                .extracting("updated_at").isNotNull();

        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("uid", walletId)
                .hasFieldOrPropertyWithValue("user_uid", userId)
                .hasFieldOrPropertyWithValue("name", "custom_wallet")
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(50.05).setScale(2, HALF_UP))
                .hasFieldOrPropertyWithValue("archived_at", Timestamp.valueOf("2030-01-01 00:00:00"));
    }

    @Sql("/sql/transaction-failed-status.sql")
    @Test
    void depositComplete_whenTransactionHasFailedStatus_shouldDoNothing() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "COMPLETED",
                null,
                BigDecimal.valueOf(30.00),
                ZonedDateTime.parse("2026-02-02T15:00:00Z")
        );
        transactionService.complete(event);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}