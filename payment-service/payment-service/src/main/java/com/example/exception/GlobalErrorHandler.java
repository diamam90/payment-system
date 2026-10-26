package com.example.exception;

import com.example.payment.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Object> handleObjectNotFound(ObjectNotFoundException exception, WebRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setError(StatusCode.ERROR_400.name());
        response.setMessage(exception.getMessage());

        return handleExceptionInternal(exception, response, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<Object> handleServiceUnavailable(ExternalServiceUnavailableException exception, WebRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setError(StatusCode.ERROR_502.name());
        response.setMessage(exception.getMessage());

        return handleExceptionInternal(exception, response, new HttpHeaders(), HttpStatus.BAD_GATEWAY, request);
    }

    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message;
        if (ex.hasFieldErrors()) {
            message = ex.getFieldErrors()
                    .stream()
                    .map(fieldError -> fieldError.getField() + ":" + fieldError.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else if (ex.hasErrors()) {
            message = ex.getGlobalErrors()
                    .stream()
                    .map(error -> error.getObjectName() + ":" + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else {
            message = ex.getLocalizedMessage();
        }

        com.example.fake.dto.ErrorResponse response = new com.example.fake.dto.ErrorResponse();
        response.setError(StatusCode.ERROR_400.name());
        response.setMessage(message);
        log.error("Ошибка валидации, вернули ошибку: {}", response, ex);
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        com.example.fake.dto.ErrorResponse response = new com.example.fake.dto.ErrorResponse();
        response.setError(StatusCode.ERROR_400.name());
        response.setMessage("Failed to read request");
        log.error("Невалидный запрос, вернули ошибку: {}", response, ex);
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}
