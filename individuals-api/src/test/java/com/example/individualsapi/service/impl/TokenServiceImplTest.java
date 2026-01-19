package com.example.individualsapi.service.impl;

import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.configuration.AdminTokenHolder;
import com.example.individualsapi.configuration.AppProperties;
import com.example.individualsapi.service.TokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import static com.example.individualsapi.stub.AuthDtoStub.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class TokenServiceImplTest {

    KeycloakClient client = Mockito.mock(KeycloakClient.class);
    AdminTokenHolder tokenHolder = Mockito.mock(AdminTokenHolder.class);
    static AppProperties properties = Mockito.mock(AppProperties.class);

    TokenService tokenService = new TokenServiceImpl(client, properties, tokenHolder);

    @BeforeAll
    public static void keycloakSettings() {
        AppProperties.KeycloakProperties keycloak = new AppProperties.KeycloakProperties();
        keycloak.setClientId(clientId);
        keycloak.setClientSecret(clientSecret);

        when(properties.getKeycloak()).thenReturn(keycloak);
    }

    @Test
    void accessTokenShouldReturnResponse() {
        var tokenRequest = keycloakUserTokenRequest();

        when(client.auth(tokenRequest)).thenReturn(keycloakUserToken());

        var actual = tokenService.accessToken("ya@ya.ru", "pass").block();
        assertEquals(keycloakUserToken().block(), actual);
    }

    @Test
    void getAccessTokenShouldReturnError() {
        var tokenRequest = keycloakUserTokenRequest();

        when(client.auth(tokenRequest)).thenReturn(accessDenied());

        var actual = tokenService.accessToken("ya@ya.ru", "pass");
        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void refreshTokenShouldReturnResponse() {
        var keycloakRequest = keycloakUserRefreshTokenRequest();
        var expectedToken = keycloakUserToken();

        when(client.refresh(keycloakRequest)).thenReturn(expectedToken);

        var tokenValueMono = tokenService.refreshToken("token value").block();
        assertEquals(expectedToken.block(), tokenValueMono);
    }

    @Test
    void refreshTokenShouldReturnError() {
        var keycloakRequest = keycloakUserRefreshTokenRequest();
        when(client.refresh(keycloakRequest)).thenReturn(accessDenied());

        var actual = tokenService.refreshToken("token value");
        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }
}