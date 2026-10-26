package com.example.service;

import com.example.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public interface MethodService {

    PaymentMethod findByCurrencyAndCountry(@NotNull String currency, @NotNull String country);

    PaymentMethod findById(Integer id);
}
