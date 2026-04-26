package com.example.individualsapi.service.impl;

import com.example.individuals.dto.*;
import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.mapper.TransactionMapper;
import com.example.individualsapi.service.TransactionService;
import com.example.transaction.api.TransactionApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl extends AbstractFeignClientService implements TransactionService {

    private final TransactionApiClient transactionApiClient;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @NewSpan("transaction_service.transaction_init")
    public Mono<TransactionInitResponse> transactionInit(TransactionInitRequest request) {
        return Mono.just(transactionMapper.transactionInitRequest(request))
                .map(req -> executeRequest(() -> transactionApiClient.transactionInit(req)))
                .map(transactionMapper::transactionInitResponse);
    }

    @Override
    @NewSpan("transaction_service.transaction_confirm")
    public Mono<TransactionConfirmResponse> transactionConfirm(TransactionConfirmRequest request) {
        return Mono.just(transactionMapper.transactionConfirmRequest(request))
                .map(req -> executeRequest(() -> transactionApiClient.transactionConfirm(req)))
                .map(transactionMapper::transactionConfirmResponse);
    }

    @Override
    @NewSpan("transaction_service.findBy")
    public Flux<TransactionStatusResponse> findBy(TransactionFilterRequest request) {
        com.example.transaction.dto.TransactionFilterRequest filter = transactionMapper.transactionFilterRequest(request);
        return Flux.fromIterable(executeRequest(() -> transactionApiClient.getTransactionsByFilter(filter)))
                .map(transactionMapper::toIndividualsResponse);
    }

    @Override
    @NewSpan("transaction_service.findById")
    public Mono<TransactionStatusResponse> findById(UUID transactionId) {
        return Mono.just(executeRequest(() -> transactionApiClient.getTransactionsStatusById(transactionId)))
                .map(transactionMapper::toIndividualsResponse);
    }

    @Override
    protected ExternalService getService() {
        return ExternalService.TRANSACTION_SERVICE;
    }
}


