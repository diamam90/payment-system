package com.example.service;

import com.example.entity.Merchant;
import com.example.entity.Payout;
import com.example.fake.dto.PayoutRequest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutService {

    Payout create(PayoutRequest request, Merchant merchant);

    Payout getById(Long id, Integer merchantId);

    Optional<Payout> findById(Long id);

    List<Payout> findBy(ZonedDateTime start, ZonedDateTime end, Integer merchantId);
}
