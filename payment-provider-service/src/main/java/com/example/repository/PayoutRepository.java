package com.example.repository;

import com.example.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByIdAndMerchantId(Long id, Integer merchantId);

    List<Payout> findByMerchantIdAndCreatedAtBetween(Integer merchantId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
