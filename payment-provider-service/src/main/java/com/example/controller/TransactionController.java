package com.example.controller;

import com.example.entity.Merchant;
import com.example.entity.Transaction;
import com.example.fake.api.TransactionApi;
import com.example.fake.dto.TransactionRequest;
import com.example.fake.dto.TransactionResponse;
import com.example.mapper.TransactionMapper;
import com.example.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        Merchant merchant = getMerchant();
        Transaction transaction = transactionService.create(transactionRequest, merchant);
        return new ResponseEntity<>(transactionMapper.toResponse(transaction), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TransactionResponse> getTransactionById(Long id) {
        Merchant merchant = getMerchant();
        Transaction transaction = transactionService.getById(id, merchant.getId());
        return new ResponseEntity<>(transactionMapper.toResponse(transaction), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TransactionResponse>> getTransactions(ZonedDateTime startDate, ZonedDateTime endDate) {
        Merchant merchant = getMerchant();
        List<Transaction> transactions = transactionService.findBy(startDate, endDate, merchant.getId());
        List<TransactionResponse> response = transactions.stream().map(transactionMapper::toResponse).toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Merchant getMerchant() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(Merchant.class::isInstance)
                .map(Merchant.class::cast)
                .orElseThrow();
    }
}
