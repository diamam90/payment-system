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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MethodServiceImpl implements MethodService {

    private final MethodRepository methodRepository;

    @Override
    public List<PaymentMethod> findByCurrencyAndCountryAndType(@NotNull String currency, String country, PaymentType type) {
        if (type == null) return methodRepository.findByCurrencyAndCountry(currency, country);
        return methodRepository.findByCurrencyAndCountryAndType(currency, country, type);
    }

    @Override
    public PaymentMethod findActiveById(Integer id) {
        return methodRepository.findActiveById(id)
                .orElseThrow(() -> new ObjectNotFoundException("PaymentMethod", "id", id));
    }
}
