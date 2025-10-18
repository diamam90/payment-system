package com.example.individualsapi.service;

import com.example.individualsapi.dto.keycloak.KeycloakTokenResponse;
import reactor.core.publisher.Mono;

public interface TokenService {

    Mono<KeycloakTokenResponse> accessToken(String email, String password);

    Mono<KeycloakTokenResponse> refreshToken(String refreshToken);

    Mono<String> adminToken();
}
