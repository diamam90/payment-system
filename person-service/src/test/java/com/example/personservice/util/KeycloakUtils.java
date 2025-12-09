package com.example.personservice.util;

import com.example.personservice.config.AppContainers;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;

public class KeycloakUtils {

    private static final String REALM = "payment";
    private static final String CLIENT_ID = "individuals-api";
    private static final String CLIENT_SECRET = "**********";

    public static AccessTokenResponse adminToken() {
        Keycloak adminClient = KeycloakBuilder.builder()
                .serverUrl(AppContainers.keycloak.getAuthServerUrl())
                .clientId(CLIENT_ID)
                .grantType("client_credentials")
                .realm(REALM)
                .clientSecret(CLIENT_SECRET)
                .build();

        return adminClient.tokenManager().getAccessToken();
    }
}
