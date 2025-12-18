package com.example.personservice.service.impl;

import com.example.personservice.entity.Country;
import com.example.personservice.exception.ObjectNotFoundException;
import com.example.personservice.repository.CountryRepository;
import com.example.personservice.service.CountryService;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @NewSpan("country_service.get_by_name")
    @Override
    public Country getCountryByName(String name) {
        return countryRepository.findByName(name)
                .orElseThrow(() -> new ObjectNotFoundException(Country.class, "name", name));
    }
}
