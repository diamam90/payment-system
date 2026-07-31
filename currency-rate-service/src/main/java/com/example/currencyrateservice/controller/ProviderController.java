package com.example.currencyrateservice.controller;

import com.example.currency.api.ProviderApi;
import com.example.currency.dto.RateProviderResponse;
import com.example.currencyrateservice.entity.Provider;
import com.example.currencyrateservice.mapper.ProviderMapper;
import com.example.currencyrateservice.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProviderController implements ProviderApi {

    private final ProviderService providerService;
    private final ProviderMapper providerMapper;

    @Override
    public ResponseEntity<List<RateProviderResponse>> getRateProviders() {
        List<Provider> providers = providerService.findAll();
        List<RateProviderResponse> response = providers.stream()
                .map(providerMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
