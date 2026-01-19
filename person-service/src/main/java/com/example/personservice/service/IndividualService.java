package com.example.personservice.service;

import com.example.personservice.entity.Individual;
import com.example.person.dto.IndividualRequest;

import java.util.UUID;

public interface IndividualService {

    Individual create(IndividualRequest request);

    Individual update(UUID id, IndividualRequest request);

    Individual findById(UUID id);

    Individual findByEmail(String email);

    void softDelete(UUID id);

    void hardDelete(UUID id);

    void activateUser(UUID id);
}
