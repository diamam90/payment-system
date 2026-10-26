package com.example.service.impl;

import com.example.entity.PaymentMethod;
import com.example.exception.ObjectNotFoundException;
import com.example.repository.MethodRepository;
import com.example.service.MethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MethodServiceImpl implements MethodService {

    private final MethodRepository methodRepository;

    @Override
    public PaymentMethod findByCurrencyAndCountry(String currency, String country) {
        return methodRepository.findByCurrencyCodeAndCountryCode(currency, country)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ObjectNotFoundException("PaymentMethod", "currencyCode", currency, "countryCode", country));
    }

    @Override
    public PaymentMethod findById(Integer id) {
        return methodRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("PaymentMethod", "id", id));
    }
}
