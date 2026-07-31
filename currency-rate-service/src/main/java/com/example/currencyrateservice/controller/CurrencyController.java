package com.example.currencyrateservice.controller;

import com.example.currency.api.CurrencyApi;
import com.example.currency.dto.CurrencyResponse;
import com.example.currencyrateservice.entity.Currency;
import com.example.currencyrateservice.mapper.CurrencyMapper;
import com.example.currencyrateservice.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CurrencyController implements CurrencyApi {

    private final CurrencyService currencyService;
    private final CurrencyMapper currencyMapper;

    @Override
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        List<Currency> currencies = currencyService.getCurrencies();
        List<CurrencyResponse> responseList = currencies.stream().map(currencyMapper::toResponse).toList();
        return ResponseEntity.ok(responseList);
    }
}
