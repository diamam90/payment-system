package com.example.transactionservice.controller;

import com.example.transaction.api.WalletApi;
import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.WalletResponse;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.mapper.WalletMapper;
import com.example.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
public class WalletController implements WalletApi {

    private final WalletService walletService;
    private final WalletMapper walletMapper;

    @Override
    public ResponseEntity<WalletResponse> createWallet(CreateWalletRequest createWalletRequest) {
        Wallet wallet = walletService.create(createWalletRequest);
        WalletResponse response = walletMapper.toResponse(wallet);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<WalletResponse> getWalletById(UUID walletUid) {
        Wallet wallet = walletService.getById(walletUid);
        WalletResponse response = walletMapper.toResponse(wallet);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<WalletResponse>> getWalletByUserId(UUID userUid) {
        List<Wallet> wallets = walletService.findByUserId(userUid);
        List<WalletResponse> response = wallets.stream().map(walletMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }
}
