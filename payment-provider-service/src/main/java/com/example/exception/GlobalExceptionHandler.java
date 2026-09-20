package com.example.exception;

import com.example.dto.StatusCode;
import com.example.fake.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(ex.getMessage());
        response.setError(ex.getStatusCode().name());
        log.error("Получен невалидный запрос, вернули ошибку: {}", response, ex);
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Object> objectNotFound(ObjectNotFoundException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setError(ex.getStatusCode().name());
        response.setMessage(ex.getMessage());
        log.error("Объект не найден, вернули ошибку: {}", response);
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
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

        ErrorResponse response = new ErrorResponse();
        response.setError(StatusCode.ERROR_400.name());
        response.setMessage(message);
        log.error("Ошибка валидации, вернули ошибку: {}", response, ex);
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ErrorResponse response = new ErrorResponse();
        response.setError(StatusCode.ERROR_400.name());
        response.setMessage("Failed to read request");
        log.error("Невалидный запрос, вернули ошибку: {}", response, ex);
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}
