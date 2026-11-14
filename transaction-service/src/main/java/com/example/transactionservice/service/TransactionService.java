package com.example.transactionservice.service;

import com.example.transaction.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.model.TransactionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionService {

    TransactionInitResponse depositInit(DepositInitRequest request);

    TransactionInitResponse transferInit(TransferInitRequest request);

    TransactionInitResponse withdrawalInit(WithdrawalInitRequest request);

    TransactionConfirmResponse depositConfirm(DepositConfirmRequest request);

    TransactionConfirmResponse transferConfirm(TransferConfirmRequest request);

    TransactionConfirmResponse withdrawalConfirm(WithdrawalConfirmRequest request);

    void complete(UUID transactionId, PatchTransactionRequest request);

    void fail(UUID transactionId, PatchTransactionRequest request);

    Page<Transaction> findBy(TransactionFilter filter, Pageable pageable);

    Transaction findByIdAndUserId(UUID transactionId, UUID userId);

}
