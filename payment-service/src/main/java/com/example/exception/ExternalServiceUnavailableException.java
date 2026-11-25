package com.example.exception;

import lombok.Getter;

@Getter
public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(String serviceName) {
        super("External service " + serviceName + " is unavailable");
    }
}
