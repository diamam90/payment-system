package com.example.controller;

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

@RestController
@RequiredArgsConstructor
public class PayoutController implements PayoutApi {

    private final PayoutService payoutService;
    private final PayoutMapper payoutMapper;

    @Override
    public ResponseEntity<PayoutResponse> createPayout(PayoutRequest payoutRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Payout payout = payoutService.create(payoutRequest, authentication);
        return new ResponseEntity<>(payoutMapper.toResponse(payout), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<PayoutResponse> getPayoutById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Payout payout = payoutService.getById(id, authentication);
        return new ResponseEntity<>(payoutMapper.toResponse(payout), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<PayoutResponse>> getPayouts(@Nullable ZonedDateTime startDate, @Nullable ZonedDateTime endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Payout> payouts = payoutService.findBy(startDate, endDate, authentication);
        List<PayoutResponse> response = payouts.stream().map(payoutMapper::toResponse).toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
