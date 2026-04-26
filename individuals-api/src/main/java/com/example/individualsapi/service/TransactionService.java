package com.example.individualsapi.service;

import com.example.individuals.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TransactionService {

    Mono<TransactionInitResponse> transactionInit(TransactionInitRequest request);

    Mono<TransactionConfirmResponse> transactionConfirm(TransactionConfirmRequest request);

    /*
    Mono<TransactionInitResponse> depositInit(DepositInitRequest request);

    Mono<TransactionInitResponse> transferInit(TransferInitRequest request);

    Mono<TransactionInitResponse> withdrawalInit(WithdrawalInitRequest request);

    Mono<TransactionConfirmResponse> depositConfirm(DepositConfirmRequest request);

    Mono<TransactionConfirmResponse> transferConfirm(TransferConfirmRequest request);

    Mono<TransactionConfirmResponse> withdrawalConfirm(WithdrawalConfirmRequest request);

     */

    Flux<TransactionStatusResponse> findBy(TransactionFilterRequest request);

    Mono<TransactionStatusResponse> findById(UUID transactionId);
}
