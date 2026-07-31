package com.example.individualsapi.service.impl;

import com.example.individuals.dto.CreateWalletRequest;
import com.example.individuals.dto.WalletResponse;
import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.mapper.WalletMapper;
import com.example.individualsapi.service.WalletService;
import com.example.transaction.api.WalletApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class WalletServiceImpl extends AbstractFeignClientService implements WalletService {

    private final WalletApiClient walletApiClient;
    private final WalletMapper walletMapper;
    private final ObjectMapper objectMapper;

    private final ExternalService service = ExternalService.TRANSACTION_SERVICE;

    @Override
    @NewSpan("wallet_service.create")
    @PreAuthorize("hasAuthority('transaction_service_wr') || principal.claims['individual_id'] ==  #request.userUid.toString()")
    public Mono<WalletResponse> create(CreateWalletRequest request) {
        return Mono.just(walletMapper.toWalletServiceRequest(request))
                .map(createWalletRequest ->
                        executeRequest(() -> walletApiClient.createWallet(createWalletRequest)))
                .map(walletMapper::toIndividualApiResponse);
    }

    @Override
    @NewSpan("wallet_service.getById")
    @PreAuthorize("hasAuthority('transaction_service_wr') || principal.claims['individual_id']!= null") // FIXME
    public Mono<WalletResponse> getById(UUID walletId) {
        return Mono.just(executeRequest(() -> walletApiClient.getWalletById(walletId)))
                .map(walletMapper::toIndividualApiResponse);
    }

    @Override
    @NewSpan("wallet_service.findByUserId")
    @PreAuthorize("hasAuthority('person_service_wr') || principal.claims['individual_id'] ==  #userId.toString()")
    public Flux<WalletResponse> findByUserId(UUID userId) {
        return Flux.fromIterable(executeRequest(() -> walletApiClient.getWalletByUserId(userId)))
                .map(walletMapper::toIndividualApiResponse);
    }
}
