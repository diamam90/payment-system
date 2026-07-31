package com.example.currencyrateservice.service;

import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.model.CurrencyRateProjection;

import java.time.ZonedDateTime;
import java.util.List;

public interface CurrencyRateService {

    CurrencyRateProjection getByCurrencyPairAndTimestamp(String sourceCode, String targetCode, ZonedDateTime dateTime);

    void saveCurrencyRates(List<CurrencyRate> currencyRate);
}
