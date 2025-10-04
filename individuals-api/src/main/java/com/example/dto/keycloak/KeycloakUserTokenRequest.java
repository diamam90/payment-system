package com.example.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakUserTokenRequest(

        @JsonProperty("client_id")
        String clientId,

        @JsonProperty("grant_type")
        String grantType,

        String username,

        String password
) {

    public static KeycloakUserTokenRequest password(String clientId, String username, String password) {
        return new KeycloakUserTokenRequest(clientId, "password", username, password);
    }
}
