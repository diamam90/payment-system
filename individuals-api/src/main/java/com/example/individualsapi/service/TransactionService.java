package com.example.individualsapi.service;

import com.example.individuals.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TransactionService {

    Mono<TransactionInitResponse> transactionInit(TransactionInitRequest request);

    Mono<TransactionConfirmResponse> transactionConfirm(TransactionConfirmRequest request);

    Flux<TransactionStatusResponse> findBy(TransactionFilterRequest request);

    Mono<TransactionStatusResponse> findByIdAndUserId(UUID transactionId, UUID userId);

    Mono<Void> success(UUID internalTransactionId, String externalTransactionId, UUID userId);

    Mono<Void> failed(UUID internalTransactionId, UUID userId);
}
