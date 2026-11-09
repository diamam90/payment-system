package com.example.service.impl;

import com.example.entity.PaymentMethod;
import com.example.entity.PaymentType;
import com.example.exception.ObjectNotFoundException;
import com.example.repository.MethodRepository;
import com.example.service.MethodService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MethodServiceImpl implements MethodService {

    private final MethodRepository methodRepository;

    @Override
    public PaymentMethod findByCurrencyAndCountryAndType(@NotNull String currency, @NotNull String country, @NotNull PaymentType type) {
        return methodRepository.findByCurrencyAndCountryAndType(currency, country, type).stream().findAny()
                .orElseThrow(() -> ObjectNotFoundException.paymentMethod(currency, country, type));
    }

    @Override
    public PaymentMethod findById(Integer id) {
        return methodRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("PaymentMethod", "id", id));
    }
}
