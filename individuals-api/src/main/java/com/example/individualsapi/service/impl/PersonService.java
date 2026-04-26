package com.example.individualsapi.service.impl;

import com.example.individualsapi.exception.ExternalService;
import com.example.person.api.IndividualsApiClient;
import com.example.person.api.PrivateApiClient;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class PersonService extends AbstractFeignClientService {

    private final IndividualsApiClient individualClient;
    private final PrivateApiClient privateClient;
    private final ObjectMapper objectMapper;

    @NewSpan("person_service.create")
    public IndividualResponse create(IndividualRequest request) {
        var individual = executeRequest(() -> individualClient.create(request));
        log.debug("Individual {} created in person service", individual);
        return individual;
    }

    @NewSpan("person_service.update")
    public IndividualResponse update(UUID id, IndividualRequest request) {
        var individual = executeRequest(() -> individualClient.update(id, request));
        log.debug("Individual {} updated in person service", individual);
        return individual;
    }

    @NewSpan("person_service.find_by_id")
    public IndividualResponse findById(UUID id) {
        var individual = executeRequest(() -> individualClient.findById(id));
        log.debug("Individual {} founded by ID in person service", individual);
        return individual;
    }

    @NewSpan("person_service.find_by_email")
    public IndividualResponse findByEmail(String email) {
        var individual = executeRequest(() -> individualClient.findBy(email));
        log.debug("Individual {} founded by email in person service", individual);
        return individual;
    }


    @NewSpan("person_service.delete_by_id")
    public Void deleteById(UUID id) {
        var result = executeRequest(() -> individualClient.deleteById(id));
        log.debug("Individual with Id {} deleted in person service", id);
        return result;
    }

    @NewSpan("person_service.compensate_creation")
    public Void compensateCreation(UUID id) {
        var result = executeRequest(() -> privateClient.hardDelete(id));
        log.debug("Individual creation with Id {} compensated in person service", id);
        return result;
    }

    @NewSpan("person_service.compensate_deletion")
    public Void compensateDeletion(UUID id) {
        var result = executeRequest(() -> privateClient.activateUser(id));
        log.debug("Individual deletion with Id {} compensated in person service", id);
        return result;
    }

    @Override
    protected ExternalService getService() {
        return ExternalService.PERSON_SERVICE;
    }
}
