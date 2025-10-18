package com.example.personservice.exception;

import com.example.person.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class CommonErrorHandler {

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleObjectNotFound(ObjectNotFoundException exception) {
        var status = HttpStatus.NOT_FOUND;
        var body = new ErrorResponse();
        body.setStatus(status.value());
        body.setError(exception.getMessage());
        logError(exception, body);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSqlException(SQLException exception) {
        var body = new ErrorResponse();
        var status = HttpStatus.BAD_REQUEST;
        body.setError("SQL exception");
        body.setStatus(status.value());
        logError(exception, body);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, BindingResult binding) {
        var body = new ErrorResponse();
        var status = HttpStatus.BAD_REQUEST;
        body.setError(binding.getFieldErrors().stream().map(FieldError::getField).collect(Collectors.joining(", ")));
        body.setStatus(status.value());
        logError(ex, body);
        return ResponseEntity.status(status).body(body);
    }

    private void logError(Exception exception, ErrorResponse response) {
        log.error("{} was thrown: {}, returned: {}", exception.getClass(), exception.getMessage(), response);
    }
}
