package com.example.service;

import com.example.dto.keycloak.KeycloakTokenResponse;
import reactor.core.publisher.Mono;

public interface TokenService {

    Mono<KeycloakTokenResponse> accessToken(String email, String password);

    Mono<KeycloakTokenResponse> refreshToken(String refreshToken);

}
