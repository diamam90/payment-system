package com.example.individualsapi.dto.keycloak;

public record KeycloakUserTokenRequest(

        String clientId,

        String clientSecret,

        String grantType,

        String username,

        String password
) {

    public static KeycloakUserTokenRequest password(String clientId, String clientSecret, String username, String password) {
        return new KeycloakUserTokenRequest(clientId, clientSecret, "password", username, password);
    }
}
