package com.example.currencyrateservice.service;

import com.example.currencyrateservice.entity.Currency;
import com.example.currencyrateservice.repository.CurrencyRepository;
import com.example.currencyrateservice.service.impl.CurrencyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    CurrencyRepository currencyRepository;
    @InjectMocks
    CurrencyServiceImpl currencyService;

    @Test
    void shouldGetCurrencies() {
        // given
        Currency currency = new Currency();
        // when
        when(currencyRepository.findAll()).thenReturn(List.of(currency));
        List<Currency> result = currencyService.getCurrencies();
        // then
        assertEquals(List.of(currency), result);
    }

    @Test
    void shouldGetAllCurrencyCodes() {
        // given
        String currencyCode = "ABC";
        // when
        when(currencyRepository.findAllCodeByActiveIsTrue()).thenReturn(List.of(currencyCode));
        List<String> result = currencyService.getAllCurrencyCodes();
        // then
        assertEquals(List.of(currencyCode), result);
    }
}