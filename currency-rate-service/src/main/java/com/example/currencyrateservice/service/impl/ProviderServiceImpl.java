package com.example.currencyrateservice.service.impl;

import com.example.currencyrateservice.entity.Provider;
import com.example.currencyrateservice.repository.ProviderRepository;
import com.example.currencyrateservice.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;

    @Override
    public List<Provider> findAll() {
        return providerRepository.findAll();
    }

    @Override
    public List<Provider> saveAll(Collection<Provider> providers) {
        return providerRepository.saveAll(providers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getByProviderCodes(Collection<String> codes) {
        return providerRepository.findByProviderCodeIn(codes);
    }

}
