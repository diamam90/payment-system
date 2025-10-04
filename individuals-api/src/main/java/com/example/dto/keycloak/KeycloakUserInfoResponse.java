package com.example.dto.keycloak;

public record KeycloakUserInfoResponse(
        String id,
        String username,
        Long createdTimestamp
) {
}
