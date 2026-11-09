package com.example.service;

import com.example.entity.Payment;
import com.example.payment.dto.PaymentRequest;
import jakarta.validation.constraints.NotNull;

public interface PaymentService {

    Payment create(@NotNull PaymentRequest request);

    void compensate(Long paymentId);
}
