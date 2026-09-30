package com.example.repository;

import com.example.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

    List<PaymentMethod> findByCurrencyCodeAndCountryCode(String currencyCode, String countryCode);
}
