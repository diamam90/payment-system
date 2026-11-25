package com.example.controller;

import com.example.entity.PaymentMethod;
import com.example.entity.PaymentType;
import com.example.mapper.MethodMapper;
import com.example.payment.api.PaymentMethodApi;
import com.example.payment.dto.PaymentMethodFilter;
import com.example.payment.dto.PaymentMethodResponse;
import com.example.service.MethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MethodController implements PaymentMethodApi {

    private final MethodService methodService;
    private final MethodMapper methodMapper;

    @Override
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(PaymentMethodFilter filter) {
        PaymentType type = filter.getType() != null ? PaymentType.valueOf(filter.getType().name()) : null;
        List<PaymentMethod> methods = methodService.findByCurrencyAndCountryAndType(filter.getCurrencyCode(), filter.getCountryCode(), type);
        List<PaymentMethodResponse> body = methods.stream().map(methodMapper::toResponse).toList();
        return ResponseEntity.ok(body);
    }
}
