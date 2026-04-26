package com.example.transactionservice.repository;

import com.example.transactionservice.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByUserId(UUID uid);

    Optional<Wallet> findByIdAndUserId(UUID walletId, UUID userId);

}
