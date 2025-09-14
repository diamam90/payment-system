package com.example.service.impl;

import com.example.client.KeycloakClient;
import com.example.configuration.AppProperties;
import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.dto.keycloak.KeycloakUserRefreshTokenRequest;
import com.example.dto.keycloak.KeycloakUserTokenRequest;
import com.example.service.TokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class TokenServiceImplTest {

    KeycloakClient client = Mockito.mock(KeycloakClient.class);
    static AppProperties properties = Mockito.mock(AppProperties.class);

    TokenService tokenService = new TokenServiceImpl(client, properties);

    static String clientId = "clientId";
    static String admin = "adm";
    static String adminPassword = "pas";

    @BeforeAll
    public static void keycloakSettings() {
        AppProperties.KeycloakProperties keycloak = new AppProperties.KeycloakProperties();
        keycloak.setClientId(clientId);
        keycloak.setPassword(adminPassword);
        keycloak.setUsername(admin);

        when(properties.getKeycloak()).thenReturn(keycloak);
    }

    @Test
    void accessTokenShouldReturnResponse() {
        var tokenRequest = new KeycloakUserTokenRequest("clientId",
                "password",
                "ya@ya.ru",
                "pass");

        var tokenResponse = tokenResponse();


        when(client.auth(tokenRequest)).thenReturn(Mono.just(tokenResponse));

        var actual = tokenService.accessToken("ya@ya.ru", "pass");
        assertEquals(tokenResponse, actual.block());
    }

    @Test
    void getAccessTokenShouldReturnError() {
        var tokenRequest = new KeycloakUserTokenRequest(clientId,
                "password",
                "ya@ya.ru",
                "pass");

        when(client.auth(tokenRequest))
                .thenReturn(errorResponse());

        var actual = tokenService.accessToken("ya@ya.ru", "pass");
        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void refreshTokenShouldReturnResponse() {
        var tokenResponse = tokenResponse();
        var keycloakRequest = new KeycloakUserRefreshTokenRequest("refresh_token", "token value", clientId);

        when(client.refresh(keycloakRequest)).thenReturn(Mono.just(tokenResponse));

        var tokenValueMono = tokenService.refreshToken("token value");
        assertEquals(tokenResponse, tokenValueMono.block());
    }

    @Test
    void refreshTokenShouldReturnError() {
        var keycloakRequest = new KeycloakUserRefreshTokenRequest("refresh_token", "token value", clientId);
        when(client.refresh(keycloakRequest)).thenReturn(errorResponse());

        var actual = tokenService.refreshToken("token value");
        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void adminTokenShouldReturnResponse() {
        var tokenRequest = new KeycloakUserTokenRequest("admin-cli",
                "password",
                admin,
                adminPassword);

        var tokenResponse = tokenResponse();
        when(client.adminAuth(tokenRequest)).thenReturn(Mono.just(tokenResponse));

        var tokenValueMono = tokenService.adminToken();
        assertEquals(tokenResponse, tokenValueMono.block());
    }

    @Test
    void adminTokenShouldReturnError() {
        var tokenRequest = new KeycloakUserTokenRequest("admin-cli",
                "password",
                admin,
                adminPassword);

        when(client.adminAuth(tokenRequest)).thenReturn(errorResponse());

        var actual = tokenService.adminToken();
        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    private KeycloakTokenResponse tokenResponse() {
        return new KeycloakTokenResponse("access token",
                "refresh token",
                23,
                32,
                "token");
    }

    private Mono<KeycloakTokenResponse> errorResponse() {
        return Mono.error(() -> new WebClientResponseException(401,
                "Unauthorized",
                null,
                null,
                null));
    }
}