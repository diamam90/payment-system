package com.example.transactionservice.controller;

import com.example.transaction.api.TransactionApi;
import com.example.transaction.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.mapper.TransactionMapper;
import com.example.transactionservice.model.TransactionFilter;
import com.example.transactionservice.service.TransactionDispatcher;
import com.example.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionDispatcher transactionDispatcher;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @Override
    public ResponseEntity<List<TransactionStatusResponse>> getTransactionsByFilter(TransactionFilterRequest request) {
        TransactionFilter filter = TransactionFilter.fromRequest(request.getFilter());
        PageRequest pageable = PageRequest.of(request.getPageable().getPage(), request.getPageable().getSize());
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
    public ResponseEntity<TransactionConfirmResponse> transactionConfirm(TransactionConfirmRequest transactionConfirmRequest) {
        TransactionConfirmResponse response = transactionDispatcher.dispatchConfirm(transactionConfirmRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionInitResponse> transactionInit(TransactionInitRequest transactionInitRequest) {
        TransactionInitResponse response = transactionDispatcher.dispatchInit(transactionInitRequest);
        return ResponseEntity.ok(response);
    }
}
