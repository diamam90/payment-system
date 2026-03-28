package com.example.transactionservice.controller;

import com.example.transaction.api.TransactionApi;
import com.example.transaction.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.entity.TransactionType;
import com.example.transactionservice.exception.BadRequestException;
import com.example.transactionservice.mapper.TransactionMapper;
import com.example.transactionservice.model.TransactionFilter;
import com.example.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @Override
    public ResponseEntity<List<TransactionStatusResponse>> getTransactionsByFilter(UUID userUid, UUID walletUid, String type, String status, ZonedDateTime dateFrom, ZonedDateTime dateTo, Integer page, Integer size) {
        TransactionType transactionType = TransactionType.valueOf(type);
        TransactionStatus transactionStatus = TransactionStatus.valueOf(status);
        TransactionFilter filter = TransactionFilter.builder()
                .userUid(userUid)
                .walletUid(walletUid)
                .type(transactionType)
                .status(transactionStatus)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
        PageRequest pageable = PageRequest.of(page, size);

        Page<Transaction> result = transactionService.findBy(filter, pageable);
        List<TransactionStatusResponse> response = result.stream().map(transactionMapper::toTransactionStatusResponse).toList();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionStatusResponse> getTransactionsStatusById(UUID transactionId) {
        Transaction transaction = transactionService.findById(transactionId);
        TransactionStatusResponse response = transactionMapper.toTransactionStatusResponse(transaction);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionConfirmResponse> transactionConfirm(String type, TransactionConfirmRequest transactionConfirmRequest) {
        if (!type.equals(transactionConfirmRequest.getType())){
            throw new BadRequestException("Type from path could be equal to request");
        }
        TransactionConfirmResponse response =
                switch (TransactionType.valueOf(type)) {
                    case TransactionType.DEPOSIT ->
                            transactionService.depositConfirm((DepositConfirmRequest) transactionConfirmRequest);
                    case TransactionType.TRANSFER ->
                            transactionService.transferConfirm((TransferConfirmRequest) transactionConfirmRequest);
                    case TransactionType.WITHDRAWAL ->
                            transactionService.withdrawalConfirm((WithdrawalConfirmRequest) transactionConfirmRequest);
                };

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionInitResponse> transactionInit(String type, TransactionInitRequest transactionInitRequest) {
        if (!type.equals(transactionInitRequest.getType())){
            throw new BadRequestException("Type from path could be equal to request");
        }
        TransactionInitResponse response =
                switch (TransactionType.valueOf(type)) {
                    case DEPOSIT -> transactionService.depositInit((DepositInitRequest) transactionInitRequest);
                    case TRANSFER -> transactionService.transferInit((TransferInitRequest) transactionInitRequest);
                    case WITHDRAWAL ->
                            transactionService.withdrawalInit((WithdrawalInitRequest) transactionInitRequest);
                };

        return ResponseEntity.ok(response);
    }
}
