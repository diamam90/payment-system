package com.example.individualsapi.service.impl;

import com.example.currency.api.CurrencyRateApiClient;
import com.example.currency.dto.RateResponse;
import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.service.CurrencyRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl extends AbstractFeignClientService implements CurrencyRateService {

    private final CurrencyRateApiClient currencyRateApiClient;
    private final ObjectMapper objectMapper;

    private static final BigDecimal SAME_CURRENCY_RATE = BigDecimal.ONE;

    @Override
    public Mono<BigDecimal> getRate(String from, String to, ZonedDateTime timestamp) {
        if (from.equals(to)) {
            return Mono.just(SAME_CURRENCY_RATE);
        } else {
            return Mono.just(executeRequest(() -> currencyRateApiClient.getRateByFilter(from, to, timestamp)))
                    .map(RateResponse::getRate);
        }
    }

    @Override
    protected ExternalService getService() {
        return ExternalService.CURRENCY_RATE_SERVICE;
    }
}
