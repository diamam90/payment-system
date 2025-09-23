package com.example.exception;

import com.example.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CommonExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ErrorResponse badRequestException(BadRequestException ex) {
        log.error(ex.getMessage());
        var response = new ErrorResponse();
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError(ex.getMessage());
        return response;
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> webClientResponseException(WebClientResponseException ex) {
        log.error(ex.getMessage());
        var response = new ErrorResponse();
        response.setStatus(ex.getStatusCode().value());
        response.setError(ex.getResponseBodyAsString());


        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }
}

