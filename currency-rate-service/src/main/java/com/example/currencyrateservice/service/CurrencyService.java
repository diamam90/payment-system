package com.example.currencyrateservice.service;

import com.example.currencyrateservice.entity.Currency;

import java.util.List;

public interface CurrencyService {

    List<Currency> getCurrencies();

    List<String> getAllCurrencyCodes();
}
