package com.example.individualsapi.service;

import com.example.payment.dto.PaymentMethodFilter;
import com.example.payment.dto.PaymentMethodResponse;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<PaymentMethodResponse> getMethod(PaymentMethodFilter filter);

    Mono<PaymentResponse> createPayment(PaymentRequest request);
}
