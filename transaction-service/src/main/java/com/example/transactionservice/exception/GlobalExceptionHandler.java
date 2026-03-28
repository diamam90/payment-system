package com.example.transactionservice.exception;

import com.example.transaction.dto.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Object> handleObjectNotFoundException(ObjectNotFoundException exception, WebRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setError(exception.getMessage());
        return handleExceptionInternal(exception, response, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleObjectNotFoundException(BadRequestException exception, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setError(exception.getMessage());
        return handleExceptionInternal(exception, response, new HttpHeaders(), status, request);
    }
}
