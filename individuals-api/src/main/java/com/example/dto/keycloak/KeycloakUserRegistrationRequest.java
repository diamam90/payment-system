package com.example.dto.keycloak;

import java.util.List;

public record KeycloakUserRegistrationRequest(

        String username,

        List<CredentialDto> credentials,

        boolean enabled
) {

    public static KeycloakUserRegistrationRequest withPasswordType(String username, String password) {
        var credentials = List.of(new CredentialDto("password", password));
        return new KeycloakUserRegistrationRequest(username, credentials, true);
    }

    public record CredentialDto(

            String type,

            String value
    ) {
    }
}

