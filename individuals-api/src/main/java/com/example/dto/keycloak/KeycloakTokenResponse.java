package com.example.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        Integer expiresIn,

        @JsonProperty("refresh_expires_in")
        Integer refreshExpiresIn,

        @JsonProperty("token_type")
        String tokenType
) {
}
