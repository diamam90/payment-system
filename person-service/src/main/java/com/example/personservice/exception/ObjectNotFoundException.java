package com.example.personservice.exception;

public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(Class<?> clazz, String paramName, Object id) {
        super("%s with %s [%s] not found".formatted(clazz.getSimpleName(), paramName, id));
    }
}
