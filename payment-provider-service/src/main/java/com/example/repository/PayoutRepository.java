package com.example.repository;

import com.example.entity.Payout;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByIdAndMerchantId(Long id, Integer merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT payout FROM Payout payout WHERE payout.id = :id")
    Optional<Payout> findByIdPessimisticWrite(Long id);

    List<Payout> findByMerchantIdAndCreatedAtBetween(Integer merchantId, LocalDateTime start, LocalDateTime end);

    List<Payout> findByMerchantIdAndCreatedAtAfter(Integer merchantId, LocalDateTime start);

    List<Payout> findByMerchantIdAndCreatedAtBefore(Integer merchantId, LocalDateTime End);

    List<Payout> findByMerchantId(Integer merchantId);
}
