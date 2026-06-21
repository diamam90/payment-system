package com.example.currencyrateservice.mapper;

import com.example.currency.dto.RateResponse;
import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.entity.Provider;
import com.example.currencyrateservice.model.CurrencyPair;
import com.example.currencyrateservice.model.CurrencyRateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class CurrencyRateMapper {

    private final Clock clock;

    public CurrencyRate create(
            CurrencyPair pair,
            BigDecimal rate,
            Instant beginTime,
            Instant endTime,
            Provider provider
    ) {
        CurrencyRate currencyRate = new CurrencyRate();
        currencyRate.setSourceCode(pair.getSource());
        currencyRate.setDestinationCode(pair.getDestination());
        currencyRate.setRate(rate);
        currencyRate.setRateBeginTime(beginTime);
        currencyRate.setRateEndTime(endTime);
        currencyRate.setProvider(provider);
        return currencyRate;
    }

    public RateResponse toResponse(CurrencyRateProjection currencyRate) {
        RateResponse response = new RateResponse();
        response.setSourceCode(currencyRate.getSourceCode());
        response.setDestinationCode(currencyRate.getDestinationCode());
        response.setRate(currencyRate.getRate());
        response.setRateTimestamp(ZonedDateTime.now(clock));
        response.setProviderCode(currencyRate.getProviderCode());
        return response;
    }
}
