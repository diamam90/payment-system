package com.example.individualsapi.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public interface CurrencyRateService {

    Mono<BigDecimal> getRate(String from, String to, ZonedDateTime timestamp);
}
