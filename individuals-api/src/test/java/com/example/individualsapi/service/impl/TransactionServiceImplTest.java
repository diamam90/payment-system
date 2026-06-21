package com.example.individualsapi.service.impl;

import com.example.currency.api.CurrencyRateApiClient;
import com.example.currency.dto.RateResponse;
import com.example.individualsapi.mapper.TransactionMapperImpl;
import com.example.individualsapi.mapper.WalletMapperImpl;
import com.example.individualsapi.mapper.WalletTypeMapperImpl;
import com.example.individualsapi.service.TransactionService;
import com.example.individualsapi.stub.CurrencyRateStub;
import com.example.individualsapi.stub.TransactionStub;
import com.example.transaction.api.TransactionApiClient;
import com.example.transaction.api.WalletApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.example.individualsapi.stub.CurrencyRateStub.rateResponse;
import static com.example.individualsapi.stub.WalletDtoStub.walletResponse_2;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        TransactionServiceImpl.class,
        WalletServiceImpl.class,
        CurrencyRateServiceImpl.class,
        TransactionMapperImpl.class,
        WalletMapperImpl.class,
        WalletTypeMapperImpl.class,
        ObjectMapper.class
})
class TransactionServiceImplTest {

    @Autowired
    TransactionService transactionService;
    @MockitoBean
    TransactionApiClient transactionApiClient;
    @MockitoBean
    WalletApiClient walletApiClient;
    @MockitoBean
    CurrencyRateApiClient currencyRateApiClient;
    @MockitoBean
    Clock clock;

    private final Clock fixed = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);

    private static final UUID SOURCE_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_WALLET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TARGET_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TARGET_WALLET_ID = UUID.fromString("11111111-1111-1111-1111-11111111112");

    private static final UUID TRANSACTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000066");

    @Test
    void depositInit() {
        // given
        var request = TransactionStub.IndividualService.depositInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.depositInitResponse(SOURCE_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.depositInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionServiceResponse = TransactionStub.TransactionService.depositInitResponse(SOURCE_WALLET_ID);
        // when
        when(transactionApiClient.transactionInit(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionInit(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void transferInit_withSameCurrencyCodes_shouldNoInteractWithCurrencyRateApi() {
        // given
        BigDecimal rate = BigDecimal.valueOf(1);
        String currencyCode = "ABC";
        var request = TransactionStub.IndividualService.transferInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.transferInitResponse(SOURCE_WALLET_ID, TARGET_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.transferInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID, rate);
        var transactionServiceResponse = TransactionStub.TransactionService.transferInitResponse(SOURCE_WALLET_ID, TARGET_WALLET_ID);
        // when
        when(clock.getZone()).thenReturn(fixed.getZone());
        when(clock.instant()).thenReturn(fixed.instant());
        when(walletApiClient.getWalletById(SOURCE_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(SOURCE_USER_ID, SOURCE_WALLET_ID, currencyCode)));
        when(walletApiClient.getWalletById(TARGET_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(TARGET_USER_ID, TARGET_WALLET_ID, currencyCode)));
        when(transactionApiClient.transactionInit(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionInit(request))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(currencyRateApiClient, never()).getRateByFilter(any(), any(), any());
    }

    @Test
    void transferInit_withDifferentCurrencyCodes_shouldInteractWithCurrencyRateApi() {
        // given
        BigDecimal rate = BigDecimal.valueOf(30);
        String sourceCode = "ABC";
        String targetCode = "CDA";
        var request = TransactionStub.IndividualService.transferInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.transferInitResponse(SOURCE_WALLET_ID, TARGET_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.transferInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID, rate);
        var transactionServiceResponse = TransactionStub.TransactionService.transferInitResponse(SOURCE_WALLET_ID, TARGET_WALLET_ID);

        RateResponse rateResponse = rateResponse(sourceCode, targetCode, rate);
        // when
        when(clock.getZone()).thenReturn(fixed.getZone());
        when(clock.instant()).thenReturn(fixed.instant());
        when(walletApiClient.getWalletById(SOURCE_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(SOURCE_USER_ID, SOURCE_WALLET_ID, sourceCode)));
        when(walletApiClient.getWalletById(TARGET_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(TARGET_USER_ID, TARGET_WALLET_ID, targetCode)));
        when(currencyRateApiClient.getRateByFilter(sourceCode, targetCode, ZonedDateTime.now(clock)))
                .thenReturn(ResponseEntity.ok(rateResponse));
        when(transactionApiClient.transactionInit(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionInit(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void withdrawalInit() {
        // given
        var request = TransactionStub.IndividualService.withdrawalInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.withdrawalInitResponse(SOURCE_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.withdrawalInitRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionServiceResponse = TransactionStub.TransactionService.withdrawalInitResponse(SOURCE_WALLET_ID);
        // when
        when(transactionApiClient.transactionInit(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionInit(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void depositConfirm() {
        // given
        var request = TransactionStub.IndividualService.depositConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.depositConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.depositConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionServiceResponse = TransactionStub.TransactionService.depositConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID);
        // when
        when(transactionApiClient.transactionConfirm(transactionServiceRequest))
                .thenReturn(ResponseEntity.ok(transactionServiceResponse));
        // then
        StepVerifier.create(transactionService.transactionConfirm(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void withdrawalConfirm() {
        // given
        var request = TransactionStub.IndividualService.withdrawalConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.withdrawalConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.withdrawalConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionServiceResponse = TransactionStub.TransactionService.withdrawalConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID);
        // when
        when(transactionApiClient.transactionConfirm(transactionServiceRequest))
                .thenReturn(ResponseEntity.ok(transactionServiceResponse));
        // then
        StepVerifier.create(transactionService.transactionConfirm(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void transferConfirm_withSameCurrencyCodes_shouldNotInteractCurrencyRateApi() {
        // given
        BigDecimal rate = BigDecimal.valueOf(1);
        String currencyCode = "ABC";
        var request = TransactionStub.IndividualService.transferConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.transferConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.transferConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID, rate);
        var transactionServiceResponse = TransactionStub.TransactionService.transferConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_WALLET_ID);
        // when
        when(clock.getZone()).thenReturn(fixed.getZone());
        when(clock.instant()).thenReturn(fixed.instant());
        when(walletApiClient.getWalletById(SOURCE_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(SOURCE_USER_ID, SOURCE_WALLET_ID, currencyCode)));
        when(walletApiClient.getWalletById(TARGET_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(TARGET_USER_ID, TARGET_WALLET_ID, currencyCode)));
        when(transactionApiClient.transactionConfirm(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionConfirm(request))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(currencyRateApiClient, never()).getRateByFilter(any(), any(), any());
    }

    @Test
    void transferConfirm_withDifferentCurrencyCodes_shouldInteractCurrencyRateApi() {
        // given
        BigDecimal rate = BigDecimal.valueOf(656);
        String sourceCode = "ABC";
        String targetCode = "CDA";
        var request = TransactionStub.IndividualService.transferConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.transferConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.transferConfirmRequest(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_USER_ID, TARGET_WALLET_ID, rate);
        var transactionServiceResponse = TransactionStub.TransactionService.transferConfirmResponse(SOURCE_USER_ID, SOURCE_WALLET_ID, TARGET_WALLET_ID);
        RateResponse rateResponse = CurrencyRateStub.rateResponse(sourceCode,targetCode, rate);
        // when
        when(clock.getZone()).thenReturn(fixed.getZone());
        when(clock.instant()).thenReturn(fixed.instant());
        when(walletApiClient.getWalletById(SOURCE_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(SOURCE_USER_ID, SOURCE_WALLET_ID, sourceCode)));
        when(walletApiClient.getWalletById(TARGET_WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse_2(TARGET_USER_ID, TARGET_WALLET_ID, targetCode)));
        when(currencyRateApiClient.getRateByFilter(sourceCode,targetCode, ZonedDateTime.now(clock))).thenReturn(ResponseEntity.ok(rateResponse));
        when(transactionApiClient.transactionConfirm(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionConfirm(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void findBy() {
        // given
        var filter = TransactionStub.IndividualService.transactionFilterRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionFilterRequest = TransactionStub.TransactionService.transactionFilterRequest(SOURCE_USER_ID, SOURCE_WALLET_ID);
        var transactionsResponse = TransactionStub.TransactionService.transactionStatusResponse(TRANSACTION_ID);
        var expectedResponse = TransactionStub.IndividualService.transactionStatusResponse(TRANSACTION_ID);
        // when
        when(transactionApiClient.getTransactionsByFilter(transactionFilterRequest))
                .thenReturn(ResponseEntity.ok(List.of(transactionsResponse, transactionsResponse)));
        // then
        StepVerifier.create(transactionService.findBy(filter))
                .expectNext(expectedResponse)
                .expectNext(expectedResponse).verifyComplete();
    }

    @Test
    void findById() {
        var transactionResponse = TransactionStub.TransactionService.transactionStatusResponse(TRANSACTION_ID);
        var expectedResponse = TransactionStub.IndividualService.transactionStatusResponse(TRANSACTION_ID);
        // when
        when(transactionApiClient.getTransactionsStatusById(TRANSACTION_ID)).thenReturn(ResponseEntity.ok(transactionResponse));
        // then
        StepVerifier.create(transactionService.findById(TRANSACTION_ID)).expectNext(expectedResponse).verifyComplete();
    }
}