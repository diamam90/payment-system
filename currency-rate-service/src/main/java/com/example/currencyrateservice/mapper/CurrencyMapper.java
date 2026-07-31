package com.example.currencyrateservice.mapper;

import com.example.currency.dto.CurrencyResponse;
import com.example.currencyrateservice.entity.Currency;
import org.springframework.stereotype.Component;

@Component
public class CurrencyMapper {

    public CurrencyResponse toResponse(Currency currency) {
        CurrencyResponse response = new CurrencyResponse();
        response.setActive(currency.getActive());
        response.setCode(currency.getCode());
        response.setIsoCode(currency.getIsoCode());
        response.setSymbol(currency.getSymbol());
        response.setDescription(currency.getDescription());
        return response;
    }
}
