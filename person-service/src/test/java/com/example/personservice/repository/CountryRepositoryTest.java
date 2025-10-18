package com.example.personservice.repository;

import com.example.personservice.config.AppContainers;
import com.example.personservice.entity.Country;
import com.example.personservice.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ImportTestcontainers(AppContainers.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CountryRepositoryTest {

    @Autowired
    CountryRepository countryRepository;

    @Test
    void findByNameShouldReturnCountry() {
        Optional<Country> country = countryRepository.findByName("Azerbaijan");
        assertThat(country).isPresent().get()
                .hasNoNullFieldsOrPropertiesExcept("status")
                .hasFieldOrPropertyWithValue("name", "Azerbaijan")
                .hasFieldOrPropertyWithValue("alpha2", "AZ")
                .hasFieldOrPropertyWithValue("alpha3", "AZE");
    }

    @Test
    void findByNotExistingNameShouldReturnEmpty() {
        Optional<Country> country = countryRepository.findByName("NotExistingName");
        assertThat(country).isEmpty();
    }

    @Test
    void findByNameWhenNameIsNullShouldReturnEmptyOptional(){
        var result = countryRepository.findByName(null);
        assertThat(result).isEmpty();
    }
}