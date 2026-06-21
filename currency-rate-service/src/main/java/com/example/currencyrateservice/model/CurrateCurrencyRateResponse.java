package com.example.currencyrateservice.model;

import java.math.BigDecimal;
import java.util.Map;

public record CurrateCurrencyRateResponse(
       Integer status,
       String message,
       Map<String, BigDecimal> data
) {
}
