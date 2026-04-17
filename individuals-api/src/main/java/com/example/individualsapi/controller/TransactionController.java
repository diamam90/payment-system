package com.example.individualsapi.controller;

import com.example.individuals.api.TransactionServiceApi;
import com.example.individuals.dto.*;
import com.example.individualsapi.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionServiceApi {

    private final TransactionService transactionService;

    @Override
    public Mono<ResponseEntity<Flux<TransactionStatusResponse>>> getTransactionsByFilter(TransactionFilterRequest transactionFilterRequest, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(transactionService.findBy(transactionFilterRequest)));
    }

    @Override
    public Mono<ResponseEntity<TransactionStatusResponse>> getTransactionsStatusById(UUID transactionId, ServerWebExchange exchange) {
        return transactionService.findById(transactionId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TransactionConfirmResponse>> transactionConfirm(Mono<TransactionConfirmRequest> body, ServerWebExchange exchange) {
        return body.flatMap(transactionService::transactionConfirm)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TransactionInitResponse>> transactionInit(Mono<TransactionInitRequest> transactionInitRequest, ServerWebExchange exchange) {
        return transactionInitRequest.flatMap(transactionService::transactionInit)
                .map(ResponseEntity::ok);
    }
}
