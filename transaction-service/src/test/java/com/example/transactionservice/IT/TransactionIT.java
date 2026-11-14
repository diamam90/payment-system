package com.example.transactionservice.IT;

import com.example.transaction.dto.*;
import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.config.SecurityTestConfig;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.repository.TransactionRepository;
import com.example.transactionservice.repository.WalletRepository;
import com.example.transactionservice.stub.ConfirmStringRequestStub;
import com.example.transactionservice.stub.InitStringRequestStub;
import com.example.transactionservice.util.KeycloakUtils;
import org.assertj.core.api.HamcrestCondition;
import org.assertj.core.condition.AnyOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.HALF_EVEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Import({SecurityTestConfig.class,
        DatabaseTestConfig.class})
public class TransactionIT {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    SecurityTestConfig securityTestConfig;
    @Spy
    TransactionRepository transactionRepository;
    @Spy
    WalletRepository walletRepository;
    @Autowired
    JdbcOperations jdbc;

    private static final String TRANSACTION_INIT_URL = "/api/v1/transactions/init";
    private static final String TRANSACTION_CONFIRM_URL = "/api/v1/transactions/confirm";
    private static final String TRANSACTION_PATCH_URL = "/api/v1/transactions/{transactionId}";
    private static final String TRANSACTION_STATUS_URL = "/api/v1/transactions/{transactionId}/users/{userId}/status";

    private static final String FIND_TRANSACTION_QUERY = "SELECT * FROM transaction.transactions WHERE uid = ? AND user_uid = ?";
    private static final String FIND_WALLET_QUERY = "SELECT * FROM transaction.wallets WHERE uid = ? AND user_uid = ?";

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @AfterEach
    void truncate() {
        jdbc.execute("TRUNCATE transaction.wallets CASCADE");
    }

    @Test
    void shouldDeposit() {
        BigDecimal amount = valueOf(250).setScale(2, HALF_EVEN);
        BigDecimal expectedFee = amount.multiply(valueOf(0.01)).setScale(2, HALF_EVEN);
        BigDecimal expectedAccrual = amount.subtract(expectedFee);
        // create wallet
        HttpEntity<String> walletRequest = withAuth(createRubWalletRequest());
        WalletResponse wallet = restTemplate.exchange("/api/v1/wallets", POST, walletRequest, WalletResponse.class)
                .getBody();

        UUID walletId = wallet.getUid();
        // deposit init
        HttpEntity<String> depositInitRequest = withAuth(InitStringRequestStub.depositInitRequest(walletId));

        TransactionInitResponse initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, depositInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = withAuth(ConfirmStringRequestStub.depositConfirmRequest(walletId));
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, depositConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before complete
        TransactionStatusResponse beforeCompleteStatus = status(transactionId, USER_ID);

        assertThat(beforeCompleteStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // complete
        PatchTransactionRequest request = new PatchTransactionRequest();
        request.setUserUid(USER_ID);
        request.setStatus(PatchTransactionRequest.StatusEnum.SUCCESS);
        request.setExternalTransactionId("provider id");
        HttpEntity<PatchTransactionRequest> patchRequest = withAuth(request);

        restTemplate.patchForObject(TRANSACTION_PATCH_URL, patchRequest, Void.class, transactionId);

        TransactionStatusResponse response = status(transactionId, USER_ID);

        assertThat(response)
                .isNotNull()
                .hasFieldOrPropertyWithValue("status", TransactionStatus.SUCCESS.name())
                .extracting("uid").isNotNull();
        // check wallet balance
        WalletResponse walletResponse = restTemplate
                .exchange("/api/v1/wallets/{walletId}", GET, withAuth(null), WalletResponse.class, walletId)
                .getBody();

        assertThat(walletResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("balance", expectedAccrual);

        Map<String, Object> transactionParams = jdbc.queryForMap(FIND_TRANSACTION_QUERY, transactionId, USER_ID);
        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("external_transaction_id", "provider id")
                .hasFieldOrPropertyWithValue("status", "SUCCESS")
                .hasFieldOrPropertyWithValue("version", 1);

        Map<String, Object> walletParams = jdbc.queryForMap(FIND_WALLET_QUERY, walletId, USER_ID);
        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("balance", expectedAccrual)
                .hasFieldOrPropertyWithValue("version", 1);
    }

    @Sql("/sql/deposit-inactive.sql")
    @Test
    void deposit_whenWalletIsInactive_shouldReturnBadRequest() {
        UUID walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        // deposit init
        HttpEntity<String> depositInitRequest = withAuth(InitStringRequestStub.depositInitRequest(walletId));

        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, depositInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "Wallet with Uid  [%s] is not available for DEPOSIT operation".formatted(walletId))
                .hasFieldOrPropertyWithValue("status", 400);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = withAuth(ConfirmStringRequestStub.depositConfirmRequest(walletId));
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, depositConfirmRequest, ErrorResponse.class);
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
        BigDecimal amount = valueOf(250).setScale(2, HALF_EVEN);
        BigDecimal expectedFee = amount.multiply(valueOf(0.01)).setScale(2, HALF_EVEN);
        String externalTransactionId = "provider id";
        // create wallet
        HttpEntity<String> walletRequest = withAuth(createRubWalletRequest());
        WalletResponse wallet = restTemplate.exchange("/api/v1/wallets", POST, walletRequest, WalletResponse.class)
                .getBody();

        UUID walletId = wallet.getUid();
        // deposit init
        HttpEntity<String> depositInitRequest = withAuth(InitStringRequestStub.depositInitRequest(walletId));

        TransactionInitResponse initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, depositInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", amount)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // deposit confirm
        HttpEntity<String> depositConfirmRequest = withAuth(ConfirmStringRequestStub.depositConfirmRequest(walletId));
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, depositConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before complete
        TransactionStatusResponse beforeCompleteStatus = status(transactionId, USER_ID);

        assertThat(beforeCompleteStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // complete
        PatchTransactionRequest request = new PatchTransactionRequest();
        request.setUserUid(USER_ID);
        request.setStatus(PatchTransactionRequest.StatusEnum.FAILED);
        request.setExternalTransactionId(externalTransactionId);
        HttpEntity<PatchTransactionRequest> patchRequest = withAuth(request);
        restTemplate.patchForObject(TRANSACTION_PATCH_URL, patchRequest, Void.class, transactionId);

        TransactionStatusResponse transactionResponse = status(transactionId, USER_ID);

        assertThat(transactionResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("status", TransactionStatus.FAILED.name())
                .hasFieldOrPropertyWithValue("failureReason", null)
                .extracting("uid").isNotNull();
        // check wallet balance
        WalletResponse walletResponse = restTemplate
                .exchange("/api/v1/wallets/{walletId}", GET, withAuth(null), WalletResponse.class, walletId)
                .getBody();

        assertThat(walletResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("balance", ZERO.setScale(2, HALF_EVEN));

        Map<String, Object> transactionParams = jdbc.queryForMap(FIND_TRANSACTION_QUERY, transactionId, USER_ID);

        assertThat(transactionParams)
                .hasFieldOrPropertyWithValue("external_transaction_id", externalTransactionId)
                .hasFieldOrPropertyWithValue("status", "FAILED");

        Map<String, Object> walletParams = jdbc.queryForMap(FIND_WALLET_QUERY, walletId, USER_ID);
        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("balance", ZERO.setScale(2, HALF_EVEN))
                .hasFieldOrPropertyWithValue("version", 0);
    }


    @Sql("/sql/withdrawal.sql")
    @Test
    void shouldWithdrawal() {
        BigDecimal balance = valueOf(99.06);
        BigDecimal amount = valueOf(40.00);
        BigDecimal expectedFee = amount.multiply(valueOf(0.02)).setScale(2, HALF_EVEN);
        BigDecimal totalSum = amount.add(expectedFee).setScale(2, HALF_EVEN);
        BigDecimal expectedBalance = balance.subtract(totalSum);

        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = withAuth(InitStringRequestStub.withdrawalInitRequest(amount));

        TransactionInitResponse initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, withdrawalInitRequest, TransactionInitResponse.class)
                .getBody();

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("amount", totalSum)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = withAuth(ConfirmStringRequestStub.withdrawalConfirmRequest(amount));
        TransactionConfirmResponse confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, withdrawalConfirmRequest, TransactionConfirmResponse.class)
                .getBody();

        UUID transactionId = confirmResponse.getUid();
        // check transaction status before complete
        TransactionStatusResponse beforeCompleteStatus = status(transactionId, USER_ID);

        assertThat(beforeCompleteStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // patch
        PatchTransactionRequest request = new PatchTransactionRequest();
        request.setUserUid(USER_ID);
        request.setStatus(PatchTransactionRequest.StatusEnum.SUCCESS);
        request.setExternalTransactionId("provider id");
        HttpEntity<PatchTransactionRequest> patchRequest = withAuth(request);
        restTemplate.patchForObject(TRANSACTION_PATCH_URL, patchRequest, Void.class, transactionId);

        TransactionStatusResponse transactionResponse = status(transactionId, USER_ID);

        assertThat(transactionResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("status", TransactionStatus.SUCCESS.name())
                .extracting("uid").isNotNull();
        // check wallet balance
        WalletResponse walletResponse = restTemplate
                .exchange("/api/v1/wallets/{walletId}", GET, withAuth(null), WalletResponse.class, UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1"))
                .getBody();

        assertThat(walletResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("balance", expectedBalance);
    }

    @Sql("/sql/withdrawal.sql")
    @Test
    void withdrawalFailed() {
        BigDecimal balance = valueOf(99.06);
        BigDecimal amount = valueOf(40.00);
        BigDecimal expectedFee = amount.multiply(valueOf(0.02)).setScale(2, HALF_EVEN);
        BigDecimal totalSum = amount.add(expectedFee).setScale(2, HALF_EVEN);

        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = withAuth(InitStringRequestStub.withdrawalInitRequest(amount));

        ResponseEntity<TransactionInitResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, withdrawalInitRequest, TransactionInitResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", totalSum)
                .hasFieldOrPropertyWithValue("fee", expectedFee);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = withAuth(ConfirmStringRequestStub.withdrawalConfirmRequest(amount));
        ResponseEntity<TransactionConfirmResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, withdrawalConfirmRequest, TransactionConfirmResponse.class);

        UUID transactionId = confirmResponse.getBody().getUid();
        // check transaction status before failed
        TransactionStatusResponse beforeFailedStatus = status(transactionId, USER_ID);

        assertThat(beforeFailedStatus)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.PENDING.name())
                .extracting("uid").isNotNull();

        // patch
        PatchTransactionRequest request = new PatchTransactionRequest();
        request.setUserUid(USER_ID);
        request.setStatus(PatchTransactionRequest.StatusEnum.FAILED);
        request.setExternalTransactionId("withdrawal failed id");
        HttpEntity<PatchTransactionRequest> patchRequest = withAuth(request);

        restTemplate.patchForObject(TRANSACTION_PATCH_URL, patchRequest, Void.class, transactionId);

        TransactionStatusResponse transactionResponse = status(transactionId, USER_ID);

        assertThat(transactionResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("status", TransactionStatus.FAILED.name())
                .hasFieldOrPropertyWithValue("failureReason", null)
                .extracting("uid").isNotNull();
        // check wallet balance
        WalletResponse walletResponse = restTemplate
                .exchange("/api/v1/wallets/{walletId}", GET, withAuth(null), WalletResponse.class, UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1"))
                .getBody();

        assertThat(walletResponse)
                .isNotNull()
                .hasFieldOrPropertyWithValue("balance", balance);
    }


    @Sql("/sql/withdrawal-inactive.sql")
    @Test
    void withdrawal_whenWalletIsInactive_shouldReturnBadRequest() {
        UUID walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        BigDecimal amount = valueOf(40.00);
        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = withAuth(InitStringRequestStub.withdrawalInitRequest(amount));

        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, withdrawalInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "Wallet with Uid  [%s] is not available for WITHDRAWAL operation".formatted(walletId))
                .hasFieldOrPropertyWithValue("status", 400);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = withAuth(ConfirmStringRequestStub.withdrawalConfirmRequest(amount));
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, withdrawalConfirmRequest, ErrorResponse.class);

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
        BigDecimal amount = valueOf(100.00);
        // withdrawal init
        HttpEntity<String> withdrawalInitRequest = withAuth(InitStringRequestStub.withdrawalInitRequest(amount));

        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, withdrawalInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);
        // withdrawal confirm
        HttpEntity<String> withdrawalConfirmRequest = withAuth(ConfirmStringRequestStub.withdrawalConfirmRequest(amount));
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, withdrawalConfirmRequest, ErrorResponse.class);
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
        BigDecimal amount = valueOf(46.06);
        BigDecimal rate = valueOf(1);
        BigDecimal expectedFee = amount.multiply(rate).multiply(valueOf(0.001)).setScale(2, HALF_EVEN);
        BigDecimal toBeTransferred = amount.subtract(expectedFee);
        // transfer init
        HttpEntity<String> transferInitRequest = withAuth(InitStringRequestStub.transferInitRequest(amount, rate));
        ResponseEntity<TransactionInitResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, transferInitRequest, TransactionInitResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", toBeTransferred)
                .hasFieldOrPropertyWithValue("fee", expectedFee);

        // transfer confirm
        HttpEntity<String> transferConfirmRequest = withAuth(InitStringRequestStub.transferInitRequest(amount, rate));
        ResponseEntity<TransactionConfirmResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, transferConfirmRequest, TransactionConfirmResponse.class);

        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", toBeTransferred)
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.SUCCESS.name());
    }

    @Sql("/sql/transfer-different-currencies.sql")
    @Test
    void shouldTransferWithDifferentCurrencies() {
        BigDecimal amount = valueOf(46.06);
        BigDecimal rate = valueOf(3.14159);
        BigDecimal expectedFee = amount.multiply(rate).multiply(valueOf(0.001)).setScale(2, HALF_EVEN);
        BigDecimal toBeTransferred = amount.subtract(expectedFee);
        // transfer init
        HttpEntity<String> transferInitRequest = withAuth(InitStringRequestStub.transferInitRequest(amount, rate));
        ResponseEntity<TransactionInitResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, transferInitRequest, TransactionInitResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", toBeTransferred)
                .hasFieldOrPropertyWithValue("fee", expectedFee);

        // transfer confirm
        HttpEntity<String> transferConfirmRequest = withAuth(InitStringRequestStub.transferInitRequest(amount, rate));
        ResponseEntity<TransactionConfirmResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, transferConfirmRequest, TransactionConfirmResponse.class);

        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.OK)
                .extracting("body")
                .hasFieldOrPropertyWithValue("amount", toBeTransferred)
                .hasFieldOrPropertyWithValue("fee", expectedFee)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.SUCCESS.name());
    }

    @Sql("/sql/transfer.sql")
    @Test
    void transfer_whenNotEnoughMoney_shouldReturnBadRequest() {
        BigDecimal amount = valueOf(246.06);
        BigDecimal rate = valueOf(1);
        // transfer init
        HttpEntity<String> transferInitRequest = withAuth(InitStringRequestStub.transferInitRequest(amount, rate));
        ResponseEntity<ErrorResponse> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, transferInitRequest, ErrorResponse.class);

        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);

        // transfer confirm
        HttpEntity<String> transferConfirmRequest = withAuth(ConfirmStringRequestStub.transferConfirmRequest(amount, rate));
        ResponseEntity<ErrorResponse> confirmResponse = restTemplate
                .exchange(TRANSACTION_CONFIRM_URL, POST, transferConfirmRequest, ErrorResponse.class);

        assertThat(confirmResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .extracting("body")
                .hasFieldOrPropertyWithValue("error", "There is not enough money in the wallet with id [fa65903f-2441-4ada-81fa-d8cd6a2e00a1].")
                .hasFieldOrPropertyWithValue("status", 400);

        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }


    @Test
    void transactionInit_WithoutAuth_ShouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> initResponse = restTemplate
                .exchange(TRANSACTION_INIT_URL, POST, new HttpEntity<>(headers), Void.class);
        assertThat(initResponse)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    private String adminToken() {
        return KeycloakUtils.adminToken(securityTestConfig.getKeycloakServerUrl()).getToken();
    }

    @Sql("/sql/complete-deposit.sql")
    @Test
    void shouldPatchUpdateTransaction() {
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        PatchTransactionRequest patchTransactionRequest = new PatchTransactionRequest();
        patchTransactionRequest.setUserUid(USER_ID);
        patchTransactionRequest.setExternalTransactionId("external transaction id");
        patchTransactionRequest.setStatus(PatchTransactionRequest.StatusEnum.FAILED);

        HttpEntity<PatchTransactionRequest> request = withAuth(patchTransactionRequest);

        restTemplate.patchForObject(TRANSACTION_PATCH_URL, request, Void.class, transactionId);

        Map<String, Object> transactionParam = jdbc.queryForMap("SELECT * FROM transaction.transactions WHERE uid = ?", transactionId);
        assertThat(transactionParam).
                hasFieldOrPropertyWithValue("status", "FAILED")
                .hasFieldOrPropertyWithValue("uid", transactionId)
                .hasFieldOrPropertyWithValue("external_transaction_id", "external transaction id");
    }

    @Sql("/sql/complete-deposit.sql")
    @RepeatedTest(5)
    void parallelPatchTransaction_shouldExecuteExactlyOnce() {
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        PatchTransactionRequest body1 = new PatchTransactionRequest();
        body1.setUserUid(USER_ID);
        body1.setExternalTransactionId("external transaction id 1");
        body1.setStatus(PatchTransactionRequest.StatusEnum.FAILED);
        HttpEntity<PatchTransactionRequest> request1 = withAuth(body1);

        PatchTransactionRequest body2 = new PatchTransactionRequest();
        body2.setUserUid(USER_ID);
        body2.setExternalTransactionId("external transaction id 2");
        body2.setStatus(PatchTransactionRequest.StatusEnum.SUCCESS);
        HttpEntity<PatchTransactionRequest> request2 = withAuth(body2);

        List<Future<ResponseEntity<?>>> futures = new ArrayList<>(10);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        IntStream.rangeClosed(1, 5)
                .forEach(_ -> {
                    futures.add(executorService.submit(
                            () -> restTemplate.exchange(TRANSACTION_PATCH_URL, HttpMethod.PATCH, request1, Void.class, transactionId))
                    );

                    futures.add(executorService.submit(
                            () -> restTemplate.exchange(TRANSACTION_PATCH_URL, HttpMethod.PATCH, request2, Void.class, transactionId))
                    );
                });

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        futures.forEach(future -> {
            try {
                if (future.get().getStatusCode().is2xxSuccessful()) {
                    success.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            } catch (Exception _) {

            }
        });
        executorService.close();

        Map<String, Object> transactionParam = jdbc.queryForMap(FIND_TRANSACTION_QUERY, transactionId, USER_ID);

        assertThat(transactionParam)
                .hasFieldOrPropertyWithValue("uid", transactionId)
                .extractingByKeys("status", "external_transaction_id")
                .has(AnyOf.anyOf(
                        new HamcrestCondition<>(IsEqual.equalTo(List.of("FAILED", "external transaction id 1"))),
                        new HamcrestCondition<>(IsEqual.equalTo(List.of("SUCCESS", "external transaction id 2")))
                ));
        assertThat(failed).hasValue(9);
        assertThat(success).hasValue(1);
    }

    @Sql("/sql/complete-deposit.sql")
    @RepeatedTest(5)
    void parallelSuccessDeposit_shouldIncreaseBalanceExactlyOnce() {
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UUID walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        PatchTransactionRequest body = new PatchTransactionRequest();
        body.setUserUid(USER_ID);
        body.setExternalTransactionId("external transaction id 2");
        body.setStatus(PatchTransactionRequest.StatusEnum.SUCCESS);
        HttpEntity<PatchTransactionRequest> request2 = withAuth(body);

        List<Future<ResponseEntity<?>>> futures = new ArrayList<>(10);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        IntStream.rangeClosed(1, 10)
                .forEach(_ ->
                        futures.add(executorService.submit(
                                () -> restTemplate.exchange(TRANSACTION_PATCH_URL, HttpMethod.PATCH, request2, Void.class, transactionId))
                        )
                );

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        futures.forEach(future -> {
            try {
                if (future.get().getStatusCode().is2xxSuccessful()) {
                    success.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            } catch (Exception _) {

            }
        });
        executorService.close();

        Map<String, Object> transactionParam = jdbc.queryForMap(FIND_TRANSACTION_QUERY, transactionId, USER_ID);

        assertThat(transactionParam)
                .hasFieldOrPropertyWithValue("uid", transactionId)
                .extractingByKeys("status", "external_transaction_id")
                .isEqualTo(List.of("SUCCESS", "external transaction id 2"));

        Map<String, Object> walletParams = jdbc.queryForMap(FIND_WALLET_QUERY, walletId, USER_ID);
        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(80.05).setScale(2, HALF_EVEN))
                .hasFieldOrPropertyWithValue("version", 1);

        assertThat(failed).hasValue(9);
        assertThat(success).hasValue(1);
    }

    @Sql("/sql/complete-withdrawal.sql")
    @RepeatedTest(5)
    void parallelFailedWithdrawal_shouldIncreaseBalanceExactlyOnce() {
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UUID walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        PatchTransactionRequest body = new PatchTransactionRequest();
        body.setUserUid(USER_ID);
        body.setExternalTransactionId("external transaction id 2");
        body.setStatus(PatchTransactionRequest.StatusEnum.FAILED);
        HttpEntity<PatchTransactionRequest> request2 = withAuth(body);

        List<Future<ResponseEntity<?>>> futures = new ArrayList<>(10);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        IntStream.rangeClosed(1, 10)
                .forEach(_ ->
                        futures.add(executorService.submit(
                                () -> restTemplate.exchange(TRANSACTION_PATCH_URL, HttpMethod.PATCH, request2, Void.class, transactionId))
                        )
                );

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        futures.forEach(future -> {
            try {
                if (future.get().getStatusCode().is2xxSuccessful()) {
                    success.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            } catch (Exception _) {

            }
        });
        executorService.close();

        Map<String, Object> transactionParam = jdbc.queryForMap(FIND_TRANSACTION_QUERY, transactionId, USER_ID);

        assertThat(transactionParam)
                .hasFieldOrPropertyWithValue("uid", transactionId)
                .extractingByKeys("status", "external_transaction_id")
                .isEqualTo(List.of("FAILED", "external transaction id 2"));

        Map<String, Object> walletParams = jdbc.queryForMap(FIND_WALLET_QUERY, walletId, USER_ID);
        assertThat(walletParams)
                .hasFieldOrPropertyWithValue("balance", BigDecimal.valueOf(80.21).setScale(2, HALF_EVEN))
                .hasFieldOrPropertyWithValue("version", 1);

        assertThat(failed).hasValue(9);
        assertThat(success).hasValue(1);
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

    private <T> HttpEntity<T> withAuth(T request) {
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(adminToken());
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (request == null) return new HttpEntity<>(authHeaders);

        return new HttpEntity<>(request, authHeaders);
    }

    private TransactionStatusResponse status(UUID transactionId, UUID userId) {
        return restTemplate
                .exchange(TRANSACTION_STATUS_URL, GET, withAuth(null), TransactionStatusResponse.class, transactionId, userId)
                .getBody();
    }
}