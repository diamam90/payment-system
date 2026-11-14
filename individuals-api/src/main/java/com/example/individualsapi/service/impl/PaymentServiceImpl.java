package com.example.individualsapi.service.impl;

import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.exception.ExternalServiceUnavailableException;
import com.example.individualsapi.service.PaymentService;
import com.example.payment.api.PaymentApi;
import com.example.payment.api.PaymentMethodApi;
import com.example.payment.dto.PaymentMethodFilter;
import com.example.payment.dto.PaymentMethodResponse;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentApi paymentApi;
    private final PaymentMethodApi paymentMethodApi;

    @Override
    @PreAuthorize("hasAuthority('person_service_wr') || principal.claims['individual_id'] != null")
    public Mono<PaymentMethodResponse> getMethod(PaymentMethodFilter filter) {
        ResponseEntity<PaymentMethodResponse> response = paymentMethodApi.getPaymentMethods(filter);
        if (response.getStatusCode().isError()) {
            throw new ExternalServiceUnavailableException(ExternalService.PAYMENT_SERVICE);
        }
        return Mono.justOrEmpty(response.getBody());
    }

    @Override
    @PreAuthorize("hasAuthority('person_service_wr') || principal.claims['individual_id'] != null")
    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        return Mono.just(request)
                .map(paymentApi::createPayment)
                .onErrorMap(ex -> {
                    if (ex instanceof HttpStatusCodeException) {
                        return new ExternalServiceUnavailableException(ExternalService.PAYMENT_SERVICE);
                    } else {
                        return ex;
                    }
                })
                .map(ResponseEntity::getBody)
                .doOnSuccess(body -> log.info("Payment Service response: {}", body));
    }
}
