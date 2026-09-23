package com.example.repository;

import com.example.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    Optional<Merchant> findByMerchantIdAndSecretKey(String merchantId, String secretKey);

    Optional<Merchant> findByMerchantId(String merchantId);
}
