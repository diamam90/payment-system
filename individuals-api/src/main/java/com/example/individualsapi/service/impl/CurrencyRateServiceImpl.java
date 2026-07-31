package com.example.individualsapi.service.impl;

import com.example.currency.api.CurrencyRateApi;
import com.example.currency.dto.RateResponse;
import com.example.individualsapi.service.CurrencyRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final CurrencyRateApi currencyRateApi;
    private final ObjectMapper objectMapper;

    private static final BigDecimal SAME_CURRENCY_RATE = BigDecimal.ONE;

    @Override
    public Mono<BigDecimal> getRate(String from, String to, ZonedDateTime timestamp) {
        if (from.equals(to)) {
            return Mono.just(SAME_CURRENCY_RATE);
        } else {
            return Mono.just(currencyRateApi.getRateByFilter(from, to, timestamp))
                    .map(ResponseEntity::getBody)
                    .map(RateResponse::getRate);
        }
    }
}
