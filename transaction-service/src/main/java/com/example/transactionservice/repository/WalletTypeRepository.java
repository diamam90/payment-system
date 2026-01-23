package com.example.transactionservice.repository;

import com.example.transactionservice.entity.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTypeRepository extends JpaRepository<WalletType, UUID> {

    List<WalletType> findByCurrencyCode(String currencyCode);
}
