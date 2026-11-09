package com.example.controller;

import com.example.entity.Payment;
import com.example.mapper.PaymentMapper;
import com.example.payment.api.PaymentApi;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @Override
    public ResponseEntity<PaymentResponse> createPayment(@Nullable PaymentRequest paymentRequest) {
        Payment payment = paymentService.create(paymentRequest);
        log.info("Платеж успешно создан, идентификатор: {}, статус: {}", payment.getId(), payment.getStatus());
        PaymentResponse response = paymentMapper.toResponse(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> compensatePayment(Long paymentId) {
        paymentService.compensate(paymentId);
        return ResponseEntity.ok().build();
    }
}
