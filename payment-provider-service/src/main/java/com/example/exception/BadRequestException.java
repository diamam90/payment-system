package com.example.exception;

import com.example.dto.StatusCode;

public class BadRequestException extends ProviderException{

    public BadRequestException( String errorMessage) {
        super(StatusCode.ERROR_400, errorMessage);
    }
}
