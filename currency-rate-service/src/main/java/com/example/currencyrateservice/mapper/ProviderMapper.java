package com.example.currencyrateservice.mapper;

import com.example.currency.dto.RateProviderResponse;
import com.example.currencyrateservice.entity.Provider;
import org.springframework.stereotype.Component;

@Component
public class ProviderMapper {

    public RateProviderResponse toResponse(Provider provider) {
        RateProviderResponse response = new RateProviderResponse();
        response.setProviderName(provider.getProviderName());
        response.setProviderCode(provider.getProviderCode());
        response.setDescription(provider.getDescription());
        response.setActive(provider.getActive());
        response.setPriority(provider.getPriority());

        return response;
    }
}
