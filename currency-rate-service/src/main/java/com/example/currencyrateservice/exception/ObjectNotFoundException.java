package com.example.currencyrateservice.exception;

public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(String objectName, String attributeName, Object attributeValue) {
        super("%s with attribute name [%s] and value %s not found".formatted(objectName, attributeName, attributeValue));
    }

    public ObjectNotFoundException(String message) {
        super(message);
    }
}
