package com.example.individualsapi.exception;

import com.example.individuals.dto.ErrorResponse;
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
        var response = new ErrorResponse();
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setError(ex.getMessage());
        logError(ex, response);

        return response;
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> webClientResponseException(WebClientResponseException ex) {
        var response = new ErrorResponse();
        response.setStatus(ex.getStatusCode().value());
        response.setError(ex.getResponseBodyAsString());
        logError(ex, response);

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> externalServiceUnavailableException(ExternalServiceUnavailableException exception) {
        var response = new ErrorResponse();
        var status = HttpStatus.BAD_GATEWAY;
        response.setStatus(status.value());
        response.setError(exception.getMessage());
        logError(exception, response);

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex) {
        log.error("Access denied to %s with status %d".formatted(ex.getService().getServiceName(), ex.getStatus()));
        var response = new ErrorResponse();
        response.setStatus(ex.getStatus());
        response.setError("Access denied");
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> objectNotFound(ObjectNotFoundException ex) {
        log.debug(ObjectNotFoundException.class.getSimpleName() + " from " + ex.getService().getServiceName() + ": " + ex.getError());
        var response = new ErrorResponse();
        var status = HttpStatus.NOT_FOUND;
        response.setStatus(status.value());
        if (ex.getError() != null) {
            response.setError(ex.getError());
        }
        return ResponseEntity.status(status).body(response);
    }

    private void logError(Exception ex, ErrorResponse response) {
        log.error("Exception [{}] has been thrown with message [{}], response: [{}]", ex.getClass().getSimpleName(), ex.getMessage(), response);
    }
}

