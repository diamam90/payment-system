package com.example.currencyrateservice.client;

import com.example.currate.api.CurrateApi;
import com.example.currate.dto.CurrencyRequest;
import com.example.currate.dto.CurrencyResponse;
import com.example.currate.dto.RateRequest;
import com.example.currate.dto.RateResponse;
import com.example.currencyrateservice.exception.BadRequestException;
import com.example.currencyrateservice.model.ProviderMetadata;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CurrateClient implements ProviderClient {

    private final CurrateApi currateClient;
    private final CurrateProperties currate;

    private static final Integer OK_STATUS = 200;

    @Override
    @Timed("currate.getActiveCurrencies")
    @NewSpan("currency_rate_service.getActiveCurrencies")
    @CircuitBreaker(name = "currate")
    @Retry(name = "currate")
    @RateLimiter(name = "currate")
    @Bulkhead(name = "currate")
    public List<String> getActiveCurrencies() {
        CurrencyResponse response = currencyList();
        if (!OK_STATUS.equals(response.getStatus())) {
            throw new BadRequestException(response.getMessage());
        }

        List<String> availablePairs = response.getData();
        List<String> extendedClientCurrencyPairs = new ArrayList<>(availablePairs.size() * 2);
        availablePairs.forEach(pair -> {
            String srcCode = pair.substring(0, 3);
            String destCode = pair.substring(3);
            extendedClientCurrencyPairs.add(srcCode + destCode);
            extendedClientCurrencyPairs.add(destCode + srcCode);
        });

        return extendedClientCurrencyPairs;
    }

    @Override
    @Timed("currate.getCurrencyRates")
    @NewSpan("currency_rate_service.getCurrencyRates")
    @CircuitBreaker(name = "currate")
    @Retry(name = "currate")
    @RateLimiter(name = "currate")
    @Bulkhead(name = "currate")
    public Map<String, BigDecimal> getCurrencyRates(List<String> pairs) {
        RateResponse response = rates(pairs);
        if (!OK_STATUS.equals(response.getStatus())) {
            throw new BadRequestException(response.getMessage());
        }
        return response.getData();
    }

    private RateResponse rates(List<String> pairs) {
        RateRequest request = new RateRequest();
        request.setGet("rates");
        request.setKey(currate.getApiKey());
        if (!pairs.isEmpty()) {
            request.setPairs(String.join(",", pairs));
        }

        return (RateResponse) currateClient.actualData(request).getBody();
    }

    private CurrencyResponse currencyList() {
        CurrencyRequest request = new CurrencyRequest();
        request.setGet("currency_list");
        request.setKey(currate.getApiKey());
        return (CurrencyResponse) currateClient.actualData(request).getBody();
    }

    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
                currate.getCode(),
                currate.getName(),
                currate.getPriority(),
                currate.getDescription()
        );
    }
}
