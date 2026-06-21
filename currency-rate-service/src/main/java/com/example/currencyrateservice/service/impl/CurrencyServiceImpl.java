package com.example.currencyrateservice.service.impl;

import com.example.currencyrateservice.entity.Currency;
import com.example.currencyrateservice.repository.CurrencyRepository;
import com.example.currencyrateservice.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Override
    public List<Currency> getCurrencies() {
        return currencyRepository.findAll();
    }

    @Override
    public List<String> getAllCurrencyCodes() {
        return currencyRepository.findAllCodeByActiveIsTrue();
    }
}
