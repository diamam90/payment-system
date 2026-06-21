package com.example.currencyrateservice.utils;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;

public class KeycloakUtils {

    private static final String REALM = "payment";
    private static final String CLIENT_ID = "individuals-api";
    private static final String CLIENT_SECRET = "**********";

    public static AccessTokenResponse adminToken(String url) {
        Keycloak adminClient = KeycloakBuilder.builder()
                .serverUrl(url)
                .clientId(CLIENT_ID)
                .grantType("client_credentials")
                .realm(REALM)
                .clientSecret(CLIENT_SECRET)
                .build();

        return adminClient.tokenManager().getAccessToken();
    }
}
