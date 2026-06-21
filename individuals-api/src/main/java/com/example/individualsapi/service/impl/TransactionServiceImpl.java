package com.example.individualsapi.service.impl;

import com.example.individuals.dto.*;
import com.example.individualsapi.exception.BadRequestException;
import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.mapper.TransactionMapper;
import com.example.individualsapi.service.CurrencyRateService;
import com.example.individualsapi.service.TransactionService;
import com.example.individualsapi.service.WalletService;
import com.example.transaction.api.TransactionApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl extends AbstractFeignClientService implements TransactionService {

    private final TransactionApiClient transactionApiClient;
    private final WalletService walletService;
    private final CurrencyRateService currencyRateService;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final ExternalService service = ExternalService.TRANSACTION_SERVICE;

    private static final String TRANSFER_TYPE = "TRANSFER";
    private static final String DEPOSIT_TYPE = "DEPOSIT";
    private static final String WITHDRAWAL_TYPE = "WITHDRAWAL";

    @Override
    @NewSpan("transaction_service.transaction_init")
    public Mono<TransactionInitResponse> transactionInit(TransactionInitRequest request) {
        return switch (request.getType()) {
            case TRANSFER_TYPE -> Mono.just(request)
                    .cast(TransferInitRequest.class)
                    .flatMap(this::transferInit)
                    .map(transactionMapper::transactionInitResponse);

            case WITHDRAWAL_TYPE -> Mono.just(request)
                    .cast(WithdrawalInitRequest.class)
                    .map(transactionMapper::withdrawalInit)
                    .map(withdrawal -> executeRequest(() -> transactionApiClient.transactionInit(withdrawal)))
                    .map(transactionMapper::transactionInitResponse);

            case DEPOSIT_TYPE -> Mono.just(request)
                    .cast(DepositInitRequest.class)
                    .map(transactionMapper::depositInit)
                    .map(deposit -> executeRequest(() -> transactionApiClient.transactionInit(deposit)))
                    .map(transactionMapper::transactionInitResponse);
            default -> throw new BadRequestException("Unexpected value: " + request.getType());
        };
    }

    @Override
    @NewSpan("transaction_service.transaction_confirm")
    public Mono<TransactionConfirmResponse> transactionConfirm(TransactionConfirmRequest request) {
        return switch (request.getType()) {
            case TRANSFER_TYPE -> Mono.just(request)
                    .cast(TransferConfirmRequest.class)
                    .flatMap(this::transferConfirm)
                    .map(transactionMapper::transactionConfirmResponse);

            case WITHDRAWAL_TYPE -> Mono.just(request)
                    .cast(WithdrawalConfirmRequest.class)
                    .map(transactionMapper::withdrawalConfirm)
                    .map(withdrawal -> executeRequest(() -> transactionApiClient.transactionConfirm(withdrawal)))
                    .map(transactionMapper::transactionConfirmResponse);

            case DEPOSIT_TYPE -> Mono.just(request)
                    .cast(DepositConfirmRequest.class)
                    .map(transactionMapper::depositConfirm)
                    .map(deposit -> executeRequest(() -> transactionApiClient.transactionConfirm(deposit)))
                    .map(transactionMapper::transactionConfirmResponse);
            default -> throw new BadRequestException("Unexpected value: " + request.getType());
        };
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

    private Mono<com.example.transaction.dto.TransactionInitResponse> transferInit(TransferInitRequest request) {
        UUID walletUid = request.getWalletUid();
        UUID targetWalletUid = request.getTargetWalletUid();

        return getRate(walletUid, targetWalletUid)
                .map(rate -> transactionMapper.transferInit(request, rate))
                .map(transferInitRequest -> executeRequest(() -> transactionApiClient.transactionInit(transferInitRequest)));
    }

    private Mono<com.example.transaction.dto.TransactionConfirmResponse> transferConfirm(TransferConfirmRequest request) {
        UUID walletUid = request.getWalletUid();
        UUID targetWalletUid = request.getTargetWalletUid();

        return getRate(walletUid, targetWalletUid)
                .map(rate -> transactionMapper.transferConfirm(request, rate))
                .map(transferConfirmRequest -> executeRequest(() -> transactionApiClient.transactionConfirm(transferConfirmRequest)));
    }

    @NewSpan("currency_rate_service.getRate")
    private Mono<BigDecimal> getRate(UUID sourceWalletUid, UUID targetWalletUid) {
        Mono<WalletResponse> sourceWalletMono = walletService.getById(sourceWalletUid);
        Mono<WalletResponse> targetWalletMono = walletService.getById(targetWalletUid);

        return Mono.zip(sourceWalletMono, targetWalletMono)
                .flatMap(tuple -> {
                    String sourceCode = extractCurrencyCode(tuple.getT1());
                    String destinationCode = extractCurrencyCode(tuple.getT2());

                    return currencyRateService.getRate(sourceCode, destinationCode, ZonedDateTime.now(clock));
                });
    }

    private String extractCurrencyCode(WalletResponse response) {
        return Optional.ofNullable(response)
                .map(WalletResponse::getType)
                .map(WalletTypeResponse::getCurrencyCode)
                .orElseThrow(() -> new BadRequestException("Cannot extract currency code from response: " + response));
    }
}


