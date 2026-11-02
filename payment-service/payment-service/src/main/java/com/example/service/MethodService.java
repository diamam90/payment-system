package com.example.service;

import com.example.entity.PaymentMethod;
import com.example.entity.PaymentType;
import jakarta.validation.constraints.NotNull;

public interface MethodService {

    PaymentMethod findByCurrencyAndCountryAndType(@NotNull String currency, @NotNull String country, @NotNull PaymentType type);

    PaymentMethod findById(Integer id);
}
