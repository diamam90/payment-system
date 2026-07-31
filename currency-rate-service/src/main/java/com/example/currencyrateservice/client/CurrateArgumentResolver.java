package com.example.currencyrateservice.client;

import com.example.currate.dto.CurrencyRequest;
import com.example.currate.dto.RateRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

import java.util.Optional;

public class CurrateArgumentResolver implements HttpServiceArgumentResolver {

    @Override
    public boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
        if (argument == null) return false;

        if (argument.getClass().equals(CurrencyRequest.class)) {
            CurrencyRequest request = (CurrencyRequest) argument;
            Optional.ofNullable(request.getGet()).ifPresent(get -> requestValues.addRequestParameter("get", get));
            Optional.ofNullable(request.getKey()).ifPresent(key -> requestValues.addRequestParameter("key", key));
            return true;
        } else if (argument.getClass().equals(RateRequest.class)) {
            RateRequest request = (RateRequest) argument;
            Optional.ofNullable(request.getGet()).ifPresent(get -> requestValues.addRequestParameter("get", get));
            Optional.ofNullable(request.getKey()).ifPresent(key -> requestValues.addRequestParameter("key", key));
            Optional.ofNullable(request.getPairs()).ifPresent(pairs -> requestValues.addRequestParameter("pairs", pairs));

            return true;
        }
        return false;
    }
}
