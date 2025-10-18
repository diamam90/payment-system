package com.example.individualsapi.service.impl;

import com.example.person.api.IndividualsApiClient;
import com.example.person.api.PrivateApiClient;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {
    @Mock
    IndividualsApiClient individualsApiClient;
    @Mock
    PrivateApiClient privateApiClient;
    @InjectMocks
    PersonService personService;
    @Mock
    ObjectMapper mapper;

    private static final UUID individualId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final IndividualRequest request = new IndividualRequest();

    private static final IndividualResponse response = new IndividualResponse();

    @Test
    void create() {
        when(individualsApiClient.create(request))
                .thenReturn(ResponseEntity.status(201).body(response));

        IndividualResponse actual = personService.create(request);
        assertEquals(response, actual);
    }

    @Test
    void update() {
        when(individualsApiClient.update(individualId, request))
                .thenReturn(ResponseEntity.status(200).body(response));

        var actual = personService.update(individualId, request);
        assertEquals(response, actual);
    }

    @Test
    void findById() {
        when(individualsApiClient.findById(individualId)).thenReturn(ResponseEntity.ok(response));

        var actual = personService.findById(individualId);
        assertEquals(response, actual);

    }

    @Test
    void findByEmail() {
        var email = "email228@a.ry";
        when(individualsApiClient.findBy(email)).thenReturn(ResponseEntity.ok(response));

        var actual = personService.findByEmail(email);
        assertEquals(response, actual);
    }

    @Test
    void deleteById() {
        when(individualsApiClient.deleteById(individualId)).thenReturn(ResponseEntity.noContent().build());

        var actual = personService.deleteById(individualId);
        assertNull(actual);
    }

    @Test
    void compensateCreation() {
        when(privateApiClient.hardDelete(individualId)).thenReturn(ResponseEntity.noContent().build());

        var actual = personService.compensateCreation(individualId);
        assertNull(actual);
    }
}