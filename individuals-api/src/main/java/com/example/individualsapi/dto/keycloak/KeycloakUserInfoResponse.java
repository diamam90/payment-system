package com.example.individualsapi.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserInfoResponse(
        String id,
        String username,
        Long createdTimestamp,
        Map<String, List<String>> attributes
) {
}
