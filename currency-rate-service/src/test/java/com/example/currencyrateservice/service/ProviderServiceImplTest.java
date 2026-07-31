package com.example.currencyrateservice.service;

import com.example.currencyrateservice.entity.Provider;
import com.example.currencyrateservice.repository.ProviderRepository;
import com.example.currencyrateservice.service.impl.ProviderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock
    ProviderRepository providerRepository;
    @InjectMocks
    ProviderServiceImpl providerService;

    @Test
    void findAll() {
        // given
        Provider provider = new Provider();
        // when
        when(providerRepository.findAll()).thenReturn(List.of(provider));
        List<Provider> result = providerService.findAll();
        // then
        assertEquals(List.of(provider), result);
    }

    @Test
    void saveAll() {
        // given
        List<Provider> providers = List.of(new Provider());
        // when
        providerService.saveAll(providers);
        // then
        verify(providerRepository).saveAll(providers);
    }

    @Test
    void getByProviderCodes() {
        // given
        Provider provider = new Provider();
        // when
        when(providerRepository.findByProviderCodeIn(List.of("aaa", "bbb", "ccc")))
                .thenReturn(List.of(provider));
        List<Provider> result = providerService.getByProviderCodes(List.of("aaa", "bbb", "ccc"));
        // then
        assertEquals(List.of(provider), result);
    }
}