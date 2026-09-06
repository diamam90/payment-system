package com.example.repository;

import com.example.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndMerchantId(Long id, Integer merchantId);

    List<Transaction> findByMerchantIdAndCreatedAtBetween(Integer merchantId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
