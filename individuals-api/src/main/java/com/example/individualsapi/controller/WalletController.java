package com.example.individualsapi.controller;

import com.example.individuals.api.WalletServiceApi;
import com.example.individuals.dto.CreateWalletRequest;
import com.example.individuals.dto.WalletResponse;
import com.example.individualsapi.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WalletController implements WalletServiceApi {

    private final WalletService walletService;

    @Override
    public Mono<ResponseEntity<WalletResponse>> createWallet(Mono<CreateWalletRequest> createWalletRequest, ServerWebExchange exchange) {
        return createWalletRequest
                .flatMap(walletService::create)
                .map(response -> new ResponseEntity<>(response, HttpStatus.CREATED));
    }

    @Override
    public Mono<ResponseEntity<WalletResponse>> getWalletById(UUID walletUid, ServerWebExchange exchange) {
        return walletService.getById(walletUid)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<WalletResponse>>> getWalletByUserId(UUID userUid, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(walletService.findByUserId(userUid)));
    }
}
