package com.example.individualsapi.service.impl;

import com.example.individualsapi.mapper.TransactionMapperImpl;
import com.example.individualsapi.service.TransactionService;
import com.example.individualsapi.stub.TransactionStub;
import com.example.transaction.api.TransactionApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        TransactionServiceImpl.class,
        TransactionMapperImpl.class,
        ObjectMapper.class
})
class TransactionServiceImplTest {

    @Autowired
    TransactionService transactionService;
    @MockitoBean
    TransactionApiClient transactionApiClient;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID WALLET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TRANSACTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000066");


    @Test
    void transactionInit() {
        // given
        var request = TransactionStub.IndividualService.depositInitRequest(USER_ID, WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.depositInitResponse(WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.depositInitRequest(USER_ID, WALLET_ID);
        var transactionServiceResponse = TransactionStub.TransactionService.depositInitResponse(WALLET_ID);
        // when
        when(transactionApiClient.transactionInit(transactionServiceRequest))
                .thenReturn(new ResponseEntity<>(transactionServiceResponse, HttpStatus.OK));
        // then
        StepVerifier.create(transactionService.transactionInit(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void transactionConfirm() {
        // given
        var request = TransactionStub.IndividualService.depositConfirmRequest(USER_ID, WALLET_ID);
        var expectedResponse = TransactionStub.IndividualService.depositConfirmResponse(USER_ID, WALLET_ID);
        var transactionServiceRequest = TransactionStub.TransactionService.depositConfirmRequest(USER_ID, WALLET_ID);
        var transactionServiceResponse = TransactionStub.TransactionService.depositConfirmResponse(USER_ID, WALLET_ID);
        // when
        when(transactionApiClient.transactionConfirm(transactionServiceRequest))
                .thenReturn(ResponseEntity.ok(transactionServiceResponse));
        // then
        StepVerifier.create(transactionService.transactionConfirm(request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void findBy() {
        // given
        var filter = TransactionStub.IndividualService.transactionFilterRequest(USER_ID, WALLET_ID);
        var transactionFilterRequest = TransactionStub.TransactionService.transactionFilterRequest(USER_ID, WALLET_ID);
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