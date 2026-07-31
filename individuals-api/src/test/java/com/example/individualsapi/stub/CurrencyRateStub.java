package com.example.individualsapi.stub;

import com.example.currency.dto.RateResponse;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class CurrencyRateStub {

    public static RateResponse rateResponse(String sourceCode, String targetCode, BigDecimal rate) {
        RateResponse rateResponse = new RateResponse();
        rateResponse.setProviderCode("tes");
        rateResponse.setRateTimestamp(ZonedDateTime.parse("2025-05-05T12:00:00Z"));
        rateResponse.setSourceCode(sourceCode);
        rateResponse.setDestinationCode(targetCode);
        rateResponse.setRate(rate);

        return rateResponse;
    }
}
