package com.example.transactionservice.service;

import com.example.transaction.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionDispatcherTest {

    @Mock
    TransactionService transactionService;

    @InjectMocks
    TransactionDispatcher dispatcher;

    @Test
    void shouldDispatchInitTransfer() {
        TransferInitRequest request = new TransferInitRequest();
        request.setType("TRANSFER");
        dispatcher.dispatchInit(request);
        Mockito.verify(transactionService).transferInit(request);
    }

    @Test
    void shouldDispatchInitDeposit() {
        DepositInitRequest request = new DepositInitRequest();
        request.setType("DEPOSIT");
        dispatcher.dispatchInit(request);
        Mockito.verify(transactionService).depositInit(request);
    }

    @Test
    void shouldDispatchInitWithdrawal() {
        WithdrawalInitRequest request = new WithdrawalInitRequest();
        request.setType("WITHDRAWAL");
        dispatcher.dispatchInit(request);
        Mockito.verify(transactionService).withdrawalInit(request);
    }

    @Test
    void shouldDispatchConfirmTransfer() {
        TransferConfirmRequest request = new TransferConfirmRequest();
        request.setType("TRANSFER");
        dispatcher.dispatchConfirm(request);
        Mockito.verify(transactionService).transferConfirm(request);
    }

    @Test
    void shouldDispatchConfirmDeposit() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setType("DEPOSIT");
        dispatcher.dispatchConfirm(request);
        Mockito.verify(transactionService).depositConfirm(request);
    }

    @Test
    void shouldDispatchConfirmWithdrawal() {
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setType("WITHDRAWAL");
        dispatcher.dispatchConfirm(request);
        Mockito.verify(transactionService).withdrawalConfirm(request);
    }
}