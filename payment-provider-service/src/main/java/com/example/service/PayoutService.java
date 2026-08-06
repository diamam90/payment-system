package com.example.service;

import com.example.entity.Payout;
import com.example.fake.dto.PayoutRequest;
import org.springframework.security.core.Authentication;

import java.time.ZonedDateTime;
import java.util.List;

public interface PayoutService {

    Payout create(PayoutRequest request, Authentication auth);

    Payout getById(Long id, Authentication auth);

    List<Payout> findBy(ZonedDateTime start, ZonedDateTime end, Authentication auth);
}
