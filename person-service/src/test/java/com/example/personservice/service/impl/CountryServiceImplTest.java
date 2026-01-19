package com.example.personservice.service.impl;

import com.example.personservice.entity.Country;
import com.example.personservice.exception.ObjectNotFoundException;
import com.example.personservice.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryServiceImplTest {

    @Mock
    CountryRepository repository;

    @InjectMocks
    CountryServiceImpl countryService;

    @Test
    void getCountryByName() {
        var country = new Country();
        country.setId(1);
        country.setName("Russia");

        when(repository.findByName("Russia")).thenReturn(Optional.of(country));
        var result = countryService.getCountryByName("Russia");

        assertEquals(country, result);
    }

    @Test
    void getCountryThrowExceptionIfNotExist() {
        when(repository.findByName("Russia")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> countryService.getCountryByName("Russia"))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Country with name [Russia] not found");
    }
}