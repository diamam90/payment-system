package com.example.exception;

import com.example.dto.StatusCode;
import lombok.Getter;

@Getter
public abstract class ProviderException extends RuntimeException {

    private final StatusCode statusCode;

    public ProviderException(StatusCode statusCode, String errorMessage) {
        super(errorMessage);
        this.statusCode = statusCode;
    }
}
