package com.example.individualsapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalService {

    PERSON_SERVICE("Person-service"),
    KEYCLOAK("Keycloak");

    private final String serviceName;
}
