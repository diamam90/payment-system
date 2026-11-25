package com.example.exception;

import com.example.entity.PaymentType;

public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(String objName, String param, Object value) {
        super(objName + " with param " + param + ": " + value + " not found");
    }

    public ObjectNotFoundException(String message) {
        super(message);
    }

    public static ObjectNotFoundException paymentMethod(String currencyCode, String countryCode, PaymentType type) {
        return new ObjectNotFoundException("PaymentMethod type: " + type + ", with currencyCode: " + currencyCode + ", countryCode: " + countryCode + " not found");
    }
}
