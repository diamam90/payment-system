package com.example.service.impl;

import com.example.client.KeycloakClient;
import com.example.configuration.AppProperties;
import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.dto.keycloak.KeycloakUserRefreshTokenRequest;
import com.example.dto.keycloak.KeycloakUserTokenRequest;
import com.example.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final KeycloakClient keycloakClient;
    private final AppProperties properties;

    private static final String LOG_PREFIX = TokenServiceImpl.class.getSimpleName();

    @Override
    public Mono<KeycloakTokenResponse> accessToken(String email, String password) {
        var request = KeycloakUserTokenRequest.password(properties.getKeycloak().getClientId(), email, password);
        return keycloakClient.auth(request)
                .log(LOG_PREFIX + ":access token request");
    }

    @Override
    public Mono<KeycloakTokenResponse> refreshToken(String refreshToken) {
        var request = KeycloakUserRefreshTokenRequest.refreshToken(properties.getKeycloak().getClientId(), refreshToken);
        return keycloakClient.refresh(request)
                .log(LOG_PREFIX + ":refresh token request");
    }
}
