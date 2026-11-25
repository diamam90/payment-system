package com.example.service;

import com.example.entity.PaymentMethod;
import com.example.entity.PaymentType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface MethodService {

    List<PaymentMethod> findByCurrencyAndCountryAndType(@NotNull String currency, String country, PaymentType type);

    PaymentMethod findActiveById(Integer id);
}
