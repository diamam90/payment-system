package com.example.transactionservice.service;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transactionservice.entity.Wallet;

import java.util.List;
import java.util.UUID;

public interface WalletService {

    Wallet create(CreateWalletRequest request);

    Wallet getByIdAndUserId(UUID walletId, UUID userId);

    Wallet getById(UUID walletId);

    List<Wallet> findByUserId(UUID id);
}
