package com.example.individualsapi.exception;

import lombok.Getter;

@Getter
public class AccessDeniedException extends RuntimeException {

    private final int status;
    private final ExternalService service;

    public AccessDeniedException(ExternalService service, int status) {
        this.status = status;
        this.service = service;
    }
}
