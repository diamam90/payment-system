package com.example.individualsapi.controller;

import com.example.individuals.api.TransactionServiceApi;
import com.example.individuals.dto.*;
import com.example.individualsapi.saga.DepositSaga;
import com.example.individualsapi.saga.WithdrawalSaga;
import com.example.individualsapi.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionServiceApi {

    private final TransactionService transactionService;
    private final DepositSaga depositSaga;
    private final WithdrawalSaga withdrawalSaga;

    @Override
    public Mono<ResponseEntity<Flux<TransactionStatusResponse>>> getTransactionsByFilter(TransactionFilterRequest transactionFilterRequest, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(transactionService.findBy(transactionFilterRequest)));
    }

    @Override
    public Mono<ResponseEntity<TransactionStatusResponse>> getTransactionsStatusById(UUID transactionId, ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwt -> jwt.getToken().getClaim("individual_id"))
                .cast(UUID.class)
                .flatMap(id -> transactionService.findByIdAndUserId(transactionId, id))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TransactionConfirmResponse>> transactionConfirm(Mono<TransactionConfirmRequest> body, ServerWebExchange exchange) {
        return body
                .flatMap(request -> {
                    if ("DEPOSIT".equals(request.getType())) {
                        return depositSaga.process((DepositConfirmRequest) request);
                    } else if ("WITHDRAWAL".equals(request.getType())) {
                        return withdrawalSaga.process((WithdrawalConfirmRequest) request);
                    } else {
                        return transactionService.transactionConfirm(request);
                    }
                })
                .map(response -> new ResponseEntity<>(response, HttpStatus.CREATED));
    }

    @Override
    public Mono<ResponseEntity<TransactionInitResponse>> transactionInit
            (Mono<TransactionInitRequest> transactionInitRequest, ServerWebExchange exchange) {
        return transactionInitRequest.flatMap(transactionService::transactionInit)
                .map(response -> new ResponseEntity<>(response, HttpStatus.CREATED));
    }
}
