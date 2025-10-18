package com.example.individualsapi.dto.keycloak;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KeycloakUserRegistrationRequest(

        String username,

        List<CredentialDto> credentials,

        Map<String, String> attributes,

        boolean enabled
) {

    public static KeycloakUserRegistrationRequest withPasswordType(String username, String password, UUID individualId) {
        var credentials = List.of(new CredentialDto("password", password));
        return new KeycloakUserRegistrationRequest(
                username,
                credentials,
                individualId != null ? Map.of("individualId", individualId.toString()) : null,
                true
        );
    }

    public record CredentialDto(

            String type,

            String value
    ) {
    }
}

