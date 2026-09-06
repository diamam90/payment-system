package com.example.controller;

import com.example.entity.Merchant;
import com.example.entity.Payout;
import com.example.fake.api.PayoutApi;
import com.example.fake.dto.PayoutRequest;
import com.example.fake.dto.PayoutResponse;
import com.example.mapper.PayoutMapper;
import com.example.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class PayoutController implements PayoutApi {

    private final PayoutService payoutService;
    private final PayoutMapper payoutMapper;

    @Override
    public ResponseEntity<PayoutResponse> createPayout(PayoutRequest payoutRequest) {
        Merchant merchant = getMerchant();
        Payout payout = payoutService.create(payoutRequest, merchant);
        return new ResponseEntity<>(payoutMapper.toResponse(payout), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<PayoutResponse> getPayoutById(Long id) {
        Merchant merchant = getMerchant();
        Payout payout = payoutService.getById(id, merchant.getId());
        return new ResponseEntity<>(payoutMapper.toResponse(payout), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<PayoutResponse>> getPayouts(@Nullable ZonedDateTime startDate, @Nullable ZonedDateTime endDate) {
        Merchant merchant = getMerchant();
        List<Payout> payouts = payoutService.findBy(startDate, endDate, merchant.getId());
        List<PayoutResponse> response = payouts.stream().map(payoutMapper::toResponse).toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Merchant getMerchant() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(Merchant.class::isInstance)
                .map(Merchant.class::cast)
                .orElseThrow();
    }
}
