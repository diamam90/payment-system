package com.example.exception;

import com.example.dto.StatusCode;
import lombok.Getter;

@Getter
public class ObjectNotFoundException extends ProviderException {

    public ObjectNotFoundException(String objName, Object identifier) {
        super(StatusCode.ERROR_404, objName + " with identifier [" + identifier + "] not found");
    }
}
