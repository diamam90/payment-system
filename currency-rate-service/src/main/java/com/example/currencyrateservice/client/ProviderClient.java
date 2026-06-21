package com.example.currencyrateservice.client;

import com.example.currencyrateservice.model.ProviderMetadata;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProviderClient {

    List<String> getActiveCurrencies();

    Map<String, BigDecimal> getCurrencyRates(List<String> pairs);

    ProviderMetadata getMetadata();
}
