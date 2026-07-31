package com.example.individualsapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalService {

    PERSON_SERVICE("Person-service"),
    TRANSACTION_SERVICE("Transaction-service"),
    KEYCLOAK("Keycloak"),
    CURRENCY_RATE_SERVICE("Currency-rate-service")
    ;

    private final String serviceName;
}
