package com.example.individualsapi.service.impl;

import com.example.individuals.dto.ErrorResponse;
import com.example.individualsapi.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractFeignClientService {

    protected abstract ExternalService getService();

    protected abstract ObjectMapper getObjectMapper();

    protected <T> T executeRequest(Supplier<ResponseEntity<T>> execution) {
        try {
            return execution.get().getBody();
        } catch (FeignException.FeignClientException ex) {
            log.warn("{} return: {}, message: {}", getService(), ex.status(), ex.getMessage());
            if (ex.status() == 401 || ex.status() == 403) {
                throw new AccessDeniedException(getService(), ex.status());
            }
            if (ex.status() == 400 || ex.status() == 409) {
                if (ex.responseBody().isPresent()) {
                    var errorResponse = getErrorResponseIfPresent(ex.status(), ex.responseBody().get().array());
                    throw new BadRequestException(errorResponse.getError());
                }
            }
            if (ex.status() == 404) {
                if (ex.responseBody().isPresent()) {
                    var errorResponse = getErrorResponseIfPresent(ex.status(), ex.responseBody().get().array());
                    throw new ObjectNotFoundException(getService(), errorResponse.getError());
                }
            }
            throw new ExternalServiceUnavailableException(getService());
        } catch (FeignException ex) {
            log.error("{} returns error: {}", getService().getServiceName(), ex.getMessage());
            throw new ExternalServiceUnavailableException(getService());
        }
    }

    private ErrorResponse getErrorResponseIfPresent(int status, byte[] body) {
        try {
            return getObjectMapper().readValue(body, ErrorResponse.class);
        } catch (IOException ex) {
            log.error("Cannot parse Error response, ex: {}", ex.getMessage());
            var error = new ErrorResponse();
            error.setStatus(status);
            return error;
        }
    }
}
