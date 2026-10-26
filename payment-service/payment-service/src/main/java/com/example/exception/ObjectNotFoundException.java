package com.example.exception;

public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(String objName, String param, Object value) {
        super(objName + " with param " + param + " :" + value + " not found");
    }

    public ObjectNotFoundException(String objName, String param1, Object value1, String param2, Object value2) {
        super(objName + " with params " + param1 + " :" + value1 + param2 + " :" + value2 + " not found");
    }
}
