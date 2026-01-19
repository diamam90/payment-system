package com.example.individualsapi.exception;

import lombok.Getter;

@Getter
public class ObjectNotFoundException extends RuntimeException {

    private final ExternalService service;
    private final String error;

    public ObjectNotFoundException(ExternalService service, String message) {
        this.error = message;
        this.service = service;
    }
}
