package com.example.individualsapi.service.impl;

import com.example.individuals.dto.ErrorResponse;
import com.example.individualsapi.exception.*;
import com.example.person.api.IndividualsApiClient;
import com.example.person.api.PrivateApiClient;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final IndividualsApiClient individualClient;
    private final PrivateApiClient privateClient;

    private final ObjectMapper mapper;

    private static final ExternalService SERVICE = ExternalService.PERSON_SERVICE;


    public IndividualResponse create(IndividualRequest request) {
        var individual = executeRequest(() -> individualClient.create(request));
        log.debug("Individual {} created in person service", individual);
        return individual;
    }

    public IndividualResponse update(UUID id, IndividualRequest request) {
        var individual = executeRequest(() -> individualClient.update(id, request));
        log.debug("Individual {} updated in person service", individual);
        return individual;
    }

    public IndividualResponse findById(UUID id) {
        var individual = executeRequest(() -> individualClient.findById(id));
        log.debug("Individual {} founded by ID in person service", individual);
        return individual;
    }

    public IndividualResponse findByEmail(String email) {
        var individual = executeRequest(() -> individualClient.findBy(email));
        log.debug("Individual {} founded by email in person service", individual);
        return individual;
    }


    public Void deleteById(UUID id) {
        var result = executeRequest(() -> individualClient.deleteById(id));
        log.debug("Individual with Id {} deleted in person service", id);
        return result;
    }

    public Void compensateCreation(UUID id) {
        var result = executeRequest(() -> privateClient.hardDelete(id));
        log.debug("Individual creation with Id {} compensated in person service", id);
        return result;
    }

    public Void compensateDeletion(UUID id) {
        var result = executeRequest(() -> privateClient.activateUser(id));
        log.debug("Individual deletion with Id {} compensated in person service", id);
        return result;
    }


    private <T> T executeRequest(Supplier<ResponseEntity<T>> execution) {
        try {
            return execution.get().getBody();
        } catch (FeignException.FeignClientException ex) {
            log.warn("{} return: {}, message: {}", SERVICE, ex.status(), ex.getMessage());
            if (ex.status() == 401 || ex.status() == 403) {
                throw new AccessDeniedException(SERVICE, ex.status());
            }
            if (ex.status() == 400 || ex.status() == 409) {
                if (ex.responseBody().isPresent()) {
                    var errorResponse = getErrorResponseIfPresent(ex.status(), ex.responseBody().get().array());
                    throw new BadRequestException(errorResponse.getError());
                }
            }
            if (ex.status() == 404) {
                if (ex.responseBody().isPresent()) {
                    var errorResponse = getErrorResponseIfPresent(ex.status(), ex.responseBody().get().array());
                    throw new ObjectNotFoundException(SERVICE, errorResponse.getError());
                }
            }

            throw new ExternalServiceUnavailableException(ExternalService.PERSON_SERVICE);
        }
    }

    private ErrorResponse getErrorResponseIfPresent(int status, byte[] body) {
        try {
            return mapper.readValue(body, ErrorResponse.class);
        } catch (IOException ex) {
            log.error("Cannot parse Error response, ex: {}", ex.getMessage());
            var error = new ErrorResponse();
            error.setStatus(status);
            return error;
        }
    }
}
