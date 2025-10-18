package com.example.individualsapi.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserInfoResponse(
        String id,
        String username,
        Long createdTimestamp
) {
}
