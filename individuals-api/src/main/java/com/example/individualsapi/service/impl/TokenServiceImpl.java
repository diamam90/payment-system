package com.example.individualsapi.service.impl;

import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.configuration.AdminTokenHolder;
import com.example.individualsapi.configuration.AppProperties;
import com.example.individualsapi.dto.keycloak.KeycloakTokenResponse;
import com.example.individualsapi.dto.keycloak.KeycloakUserRefreshTokenRequest;
import com.example.individualsapi.dto.keycloak.KeycloakUserTokenRequest;
import com.example.individualsapi.service.TokenService;
import io.micrometer.context.ContextRegistry;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final KeycloakClient keycloakClient;
    private final AppProperties properties;
    private final AdminTokenHolder tokenHolder;

    @Override
    @NewSpan("token_service.access_token")
    public Mono<KeycloakTokenResponse> accessToken(String email, String password) {
        var request = KeycloakUserTokenRequest.password(
                properties.getKeycloak().getClientId(),
                properties.getKeycloak().getClientSecret(),
                email,
                password
        );
        return keycloakClient.auth(request)
                .doOnSuccess(response ->
                        log.debug("received access token {} for email {}", response.accessToken(), email));
    }

    @Override
    @NewSpan("token_service.refresh_token")
    public Mono<KeycloakTokenResponse> refreshToken(String refreshToken) {
        var request = KeycloakUserRefreshTokenRequest.refreshToken(
                properties.getKeycloak().getClientId(),
                properties.getKeycloak().getClientSecret(),
                refreshToken
        );
        return keycloakClient.refresh(request)
                .doOnSuccess(response ->
                        log.debug("received access token {} for refresh token {}", response.accessToken(), refreshToken));
    }

    @Override
    @NewSpan("token_service.admin_token")
    public Mono<String> adminToken() {
        if (!tokenHolder.isExpired()) {
            return Mono.just(tokenHolder.getAccessToken());
        }
        var adminTokenRequest = adminRequestToken();
        log.debug("updating admin token...");
        return keycloakClient.adminToken(adminTokenRequest)
                .doOnNext(tokenResponse -> {
                    log.debug("updated admin token: {}", tokenResponse.accessToken());
                    tokenHolder.update(
                            tokenResponse.accessToken(),
                            tokenResponse.expiresIn()
                    );
                })
                .map(KeycloakTokenResponse::accessToken);
    }

    private KeycloakUserTokenRequest adminRequestToken() {
        return KeycloakUserTokenRequest.clientCredentials(
                properties.getKeycloak().getClientId(),
                properties.getKeycloak().getClientSecret()
        );
    }
}
