package com.example.transactionservice.service;

import com.example.transaction.dto.*;
import com.example.transactionservice.entity.TransactionType;
import com.example.transactionservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionDispatcher {
    private final TransactionService transactionService;

    public TransactionInitResponse dispatchInit(TransactionInitRequest transactionInitRequest) {
        return switch (TransactionType.valueOf(transactionInitRequest.getType())) {
            case DEPOSIT -> transactionService.depositInit((DepositInitRequest) transactionInitRequest);
            case TRANSFER -> transactionService.transferInit((TransferInitRequest) transactionInitRequest);
            case WITHDRAWAL -> transactionService.withdrawalInit((WithdrawalInitRequest) transactionInitRequest);
        };
    }

    public TransactionConfirmResponse dispatchConfirm(TransactionConfirmRequest transactionConfirmRequest) {
        return switch (TransactionType.valueOf(transactionConfirmRequest.getType())) {
            case TransactionType.DEPOSIT ->
                    transactionService.depositConfirm((DepositConfirmRequest) transactionConfirmRequest);
            case TransactionType.TRANSFER ->
                    transactionService.transferConfirm((TransferConfirmRequest) transactionConfirmRequest);
            case TransactionType.WITHDRAWAL ->
                    transactionService.withdrawalConfirm((WithdrawalConfirmRequest) transactionConfirmRequest);
        };
    }
}
