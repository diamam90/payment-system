package com.example.personservice.service.impl;

import com.example.personservice.entity.Country;
import com.example.personservice.entity.Individual;
import com.example.personservice.entity.Status;
import com.example.personservice.exception.ObjectNotFoundException;
import com.example.personservice.mapper.IndividualMapper;
import com.example.personservice.repository.IndividualRepository;
import com.example.personservice.service.CountryService;
import com.example.personservice.stub.entity.IndividualStub;
import com.example.personservice.stub.request.IndividualRequestStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndividualServiceImplTest {

    @Mock
    CountryService countryService;
    @Mock
    IndividualRepository individualRepository;

    @Spy
    IndividualMapper mapper = new IndividualMapper();

    @InjectMocks
    IndividualServiceImpl individualService;

    @Test
    void shouldCreate() {
        var request = IndividualRequestStub.request_1();

        var country = new Country();
        country.setName("Russia");
        country.setId(228);

        var expected = new Individual();
        expected.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        when(countryService.getCountryByName("Russia")).thenReturn(country);
        when(individualRepository.save(Mockito.any(Individual.class))).thenReturn(expected);

        var actual = individualService.create(request);
        assertEquals(expected, actual);
    }

    @Test
    void shouldCreateWhenRequestCountryIsNull() {
        var request = IndividualRequestStub.request_2();
        var savedIndividual = new Individual();
        savedIndividual.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        when(individualRepository.save(any(Individual.class))).thenReturn(savedIndividual);

        individualService.create(request);

        verify(mapper).create(request, null);
    }

    @Test
    void update() {
        var requestId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var request = IndividualRequestStub.request_2();
        var individualFromDb = IndividualStub.individual_1();

        when(individualRepository.findById(requestId)).thenReturn(Optional.of(individualFromDb));
        var actual = individualService.update(requestId, request);

        assertThat(actual).isNotNull()
                .hasFieldOrPropertyWithValue("id", requestId)
                .hasFieldOrPropertyWithValue("passportNumber", "1818")
                .hasFieldOrPropertyWithValue("phoneNumber", "88002000600")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("3025-06-05T12:00:00+03:00").toInstant())
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("3025-06-05T11:00:00+04:00").toInstant())
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("user.secretKey", "pssss")
                .hasFieldOrPropertyWithValue("user.email", "ya@ya.ya")
                .hasFieldOrPropertyWithValue("user.firstName", "Ivan")
                .hasFieldOrPropertyWithValue("user.lastName", "Ivanov")
                .hasFieldOrPropertyWithValue("user.created", Instant.parse("2025-05-05T10:00:00Z"))
                .hasFieldOrPropertyWithValue("user.updated", Instant.parse("2025-05-05T10:00:00Z"))
                .hasFieldOrPropertyWithValue("user.address.address", "test address 2")
                .hasFieldOrPropertyWithValue("user.address.city", "test city 2")
                .hasFieldOrPropertyWithValue("user.address.country", null)
                .hasFieldOrPropertyWithValue("user.address.zipCode", "322")
                .hasFieldOrPropertyWithValue("user.address.state", "test state 2")
                .hasFieldOrPropertyWithValue("user.address.archived", ZonedDateTime.parse("2027-01-01T00:00:00+04:00").toInstant());


    }

    @Test
    void update_WhenIndividualNotFound_ShouldThrowObjectNotFoundException() {
        var requestId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var request = IndividualRequestStub.request_2();
        when(individualRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> individualService.update(requestId, request))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Individual with id [%s] not found".formatted(requestId));
    }

    @Test
    void findById() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var expected = new Individual();

        when(individualRepository.findById(id)).thenReturn(Optional.of(expected));
        var actual = individualService.findById(id);

        assertEquals(expected, actual);
    }

    @Test
    void findByIdShouldThrowObjectNotFoundException() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000000");

        when(individualRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> individualService.findById(id))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Individual with id [%s] not found".formatted(id));
    }

    @Test
    void findByEmail() {
        var email = "test@test";
        var expected = new Individual();

        when(individualRepository.findByEmail(email)).thenReturn(Optional.of(expected));
        var actual = individualService.findByEmail(email);

        assertEquals(expected, actual);
    }

    @Test
    void findByEmailShouldThrowObjectNotFoundException() {
        var email = "test@test";

        when(individualRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> individualService.findByEmail(email))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Individual with email [%s] not found".formatted(email));
    }

    @Test
    void deleteById() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var individual = new Individual();
        individual.setId(id);
        when(individualRepository.findById(id)).thenReturn(Optional.of(individual));
        individualService.softDelete(id);
        assertThat(individual)
                .hasFieldOrPropertyWithValue("id", id)
                .hasFieldOrPropertyWithValue("status", Status.INACTIVE.getStatusCode());;
    }

    @Test
    void activateUser() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var individual = new Individual();
        individual.setId(id);
        when(individualRepository.findById(id)).thenReturn(Optional.of(individual));
        individualService.activateUser(id);

        assertThat(individual)
                .hasFieldOrPropertyWithValue("id", id)
                .hasFieldOrPropertyWithValue("status", Status.ACTIVE.getStatusCode());
    }
}