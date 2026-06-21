package com.example.currencyrateservice.controller;

import com.example.currency.api.CurrencyRateApi;
import com.example.currency.dto.RateResponse;
import com.example.currencyrateservice.mapper.CurrencyRateMapper;
import com.example.currencyrateservice.model.CurrencyRateProjection;
import com.example.currencyrateservice.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@RequiredArgsConstructor
public class CurrencyRateController implements CurrencyRateApi {

    private final CurrencyRateService currencyRateService;
    private final CurrencyRateMapper currencyRateMapper;

    @Override
    public ResponseEntity<RateResponse> getRateByFilter(String from, String to, ZonedDateTime timestamp) {
        CurrencyRateProjection currencyRate = currencyRateService.getByCurrencyPairAndTimestamp(from, to, timestamp);
        return ResponseEntity.ok(currencyRateMapper.toResponse(currencyRate));
    }
}
