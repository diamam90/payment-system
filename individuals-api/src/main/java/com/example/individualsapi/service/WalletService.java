package com.example.individualsapi.service;

import com.example.individuals.dto.CreateWalletRequest;
import com.example.individuals.dto.WalletResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WalletService {

    Mono<WalletResponse> create(CreateWalletRequest request);

    Mono<WalletResponse> getById(UUID walletId);

    Flux<WalletResponse> findByUserId(UUID id);
}
