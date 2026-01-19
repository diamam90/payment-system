package com.example.individualsapi.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakUserRefreshTokenRequest(

        @JsonProperty("grant_type")
        String grantType,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("client_id")
        String clientId,

        @JsonProperty("client_secret")
        String clientSecret
) {
    public static KeycloakUserRefreshTokenRequest refreshToken(String clientId, String clientSecret, String refreshToken) {
        return new KeycloakUserRefreshTokenRequest("refresh_token", refreshToken, clientId, clientSecret);
    }
}
