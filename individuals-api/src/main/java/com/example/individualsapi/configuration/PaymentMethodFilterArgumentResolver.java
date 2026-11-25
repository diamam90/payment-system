package com.example.individualsapi.configuration;

import com.example.payment.dto.PaymentMethodFilter;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

public class PaymentMethodFilterArgumentResolver implements HttpServiceArgumentResolver {

    @Override
    public boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
        if (parameter.getParameterType().equals(PaymentMethodFilter.class)) {
            PaymentMethodFilter filter = (PaymentMethodFilter) argument;
            if (filter!= null){
                requestValues.addRequestParameter("currencyCode", filter.getCurrencyCode());
                requestValues.addRequestParameter("countryCode", filter.getCountryCode());
                requestValues.addRequestParameter("type", filter.getType().name());
            }
            return true;
        }
        return false;
    }
}
