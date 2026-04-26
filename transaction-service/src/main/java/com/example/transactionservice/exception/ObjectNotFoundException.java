package com.example.transactionservice.exception;

import lombok.Getter;

@Getter
public class ObjectNotFoundException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "%s with %s [%s] not found";

    public <ID> ObjectNotFoundException(String entity, String attributeName, ID identifier) {
        super(MESSAGE_TEMPLATE.formatted(entity, attributeName, identifier));
    }

    public <ID1, ID2> ObjectNotFoundException(
            String entity,
            String attributeName1,
            ID1 identifier1,
            String attributeName2,
            ID2 identifier2
    ) {
        super(MESSAGE_TEMPLATE.formatted(entity, attributeName1 + ", " + attributeName2, identifier1 + ", " + identifier2));
    }

    public <ID> ObjectNotFoundException(String entity, ID identifier) {
        this(entity, "Id", identifier);
    }
}
