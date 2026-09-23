package com.example.repository;

import com.example.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndMerchantId(Long id, Integer merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT transaction FROM Transaction transaction WHERE transaction.id = :id")
    Optional<Transaction> findByIdPessimisticWrite(Long id);

    List<Transaction> findByMerchantIdAndCreatedAtBetween(Integer merchantId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
