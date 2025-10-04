package com.example.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakUserRefreshTokenRequest(

        @JsonProperty("grant_type")
        String grantType,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("client_id")
        String clientId
) {
    public static KeycloakUserRefreshTokenRequest refreshToken(String clientId, String refreshToken) {
        return new KeycloakUserRefreshTokenRequest("refresh_token", refreshToken, clientId);
    }
}
