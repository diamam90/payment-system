package com.example.client;

import com.example.configuration.AppProperties;
import com.example.dto.keycloak.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final WebClient keycloakClient;
    private final AppProperties properties;

    private String regUserUri = "/admin/realms/{realm}/users";
    private String userInfoUri = "/admin/realms/{realm}/users/{userId}";
    private String tokenUri = "/realms/{realm}/protocol/openid-connect/token";
    private String adminTokenUri = "/realms/master/protocol/openid-connect/token";

    public Mono<Void> registration(KeycloakUserRegistrationRequest request, String adminToken) {
        return keycloakClient.post()
                .uri(regUserUri, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .bodyValue(request)
                .exchangeToMono(responseHandler(Void.class))
                .log("keycloak registration request");
    }

    public Mono<KeycloakTokenResponse> auth(KeycloakUserTokenRequest request) {
        return keycloakClient.post()
                .uri(tokenUri, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(responseHandler(KeycloakTokenResponse.class))
                .log("keycloak auth request");
    }

    public Mono<KeycloakTokenResponse> refresh(KeycloakUserRefreshTokenRequest request) {
        return keycloakClient.post()
                .uri(tokenUri, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(responseHandler(KeycloakTokenResponse.class))
                .log("keycloak refresh token request");
    }

    public Mono<KeycloakUserInfoResponse> userInfo(String userId, String token) {
        return keycloakClient.get()
                .uri(userInfoUri, properties.getKeycloak().getRealm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .exchangeToMono(responseHandler(KeycloakUserInfoResponse.class))
                .log("keycloak userInfo request");
    }

    public Mono<KeycloakTokenResponse> adminAuth(KeycloakUserTokenRequest request) {
        return keycloakClient.post()
                .uri(adminTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(responseHandler(KeycloakTokenResponse.class))
                .log("admin token request");
    }

    private <T> Function<ClientResponse, Mono<T>> responseHandler(Class<T> clazz) {
        return response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(clazz);
            } else {
                return response.createError();
            }
        };
    }

    private MultiValueMap<String, String> convertToMap(KeycloakUserTokenRequest request) {
        var map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", request.clientId());
        map.add("grant_type", request.grantType());
        map.add("username", request.username());
        map.add("password", request.password());

        return map;
    }

    private MultiValueMap<String, String> convertToMap(KeycloakUserRefreshTokenRequest request) {
        var map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", request.clientId());
        map.add("grant_type", request.grantType());
        map.add("refresh_token", request.refreshToken());

        return map;
    }
}
