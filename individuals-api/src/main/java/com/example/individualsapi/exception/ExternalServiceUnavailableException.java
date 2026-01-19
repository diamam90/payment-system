package com.example.individualsapi.exception;

public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(ExternalService service) {
        super("Service %s is unavailable".formatted(service.getServiceName()));
    }
}
