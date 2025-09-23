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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final WebClient keycloakClient;
    private final JwtHolder jwtHolder = new JwtHolder();
    private final AppProperties properties;

    private static final String REG_USER_URI = "/admin/realms/{realm}/users";
    private static final String USER_INFO_URI = "/admin/realms/{realm}/users/{userId}";
    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String ADMIN_TOKEN_URI = "/realms/master/protocol/openid-connect/token";

    private static final String ADMIN_CLIENT_ID = "admin-cli";

    private static final String LOG_PREFIX = KeycloakClient.class.getSimpleName();

    public Mono<Void> registration(KeycloakUserRegistrationRequest request) {
        return adminToken()
                .flatMap(token ->
                        keycloakClient.post()
                                .uri(REG_USER_URI, properties.getKeycloak().getRealm())
                                .contentType(MediaType.APPLICATION_JSON)
                                .headers(headers -> headers.setBearerAuth(token))
                                .bodyValue(request)
                                .exchangeToMono(handleResponse(Void.class)))
                .log(LOG_PREFIX + ":registration");
    }

    public Mono<KeycloakTokenResponse> auth(KeycloakUserTokenRequest request) {
        return keycloakClient.post()
                .uri(TOKEN_URI, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(handleResponse(KeycloakTokenResponse.class))
                .log(LOG_PREFIX + ":auth request");
    }

    public Mono<KeycloakTokenResponse> refresh(KeycloakUserRefreshTokenRequest request) {
        return keycloakClient.post()
                .uri(TOKEN_URI, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(handleResponse(KeycloakTokenResponse.class))
                .log(LOG_PREFIX + ":refresh token request");
    }

    public Mono<KeycloakUserInfoResponse> userInfo(String userId) {

        return adminToken()
                .flatMap(token -> keycloakClient.get()
                        .uri(USER_INFO_URI, properties.getKeycloak().getRealm(), userId)
                        .headers(headers -> headers.setBearerAuth(token))
                        .exchangeToMono(handleResponse(KeycloakUserInfoResponse.class)))
                .log(LOG_PREFIX + ":userInfo request");
    }

    private Mono<String> adminToken() {
        if (jwtHolder.isNotExpired()) {
            log.debug("use existing token...");
            return Mono.just(jwtHolder.accessToken());
        } else {
            var adminTokenRequest = KeycloakUserTokenRequest.password(ADMIN_CLIENT_ID,
                    properties.getKeycloak().getUsername(),
                    properties.getKeycloak().getPassword());

            log.debug("update token...");
            return keycloakClient.post()
                    .uri(ADMIN_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(convertToMap(adminTokenRequest))
                    .exchangeToMono(handleResponse(KeycloakTokenResponse.class))
                    .doOnNext(tokenResponse -> {
                        jwtHolder.update(tokenResponse.accessToken(), Instant.now(), Duration.ofSeconds(tokenResponse.expiresIn()));
                    })
                    .map(KeycloakTokenResponse::accessToken)
                    .log(LOG_PREFIX + ":admin token request");
        }
    }

    private <T> Function<ClientResponse, Mono<T>> handleResponse(Class<T> clazz) {
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

    private static class JwtHolder {
        private String token;
        private Instant expiresAt;
        private final Object lock = new Object();

        boolean isNotExpired() {
            return Objects.nonNull(expiresAt) &&
                    Instant.now().isBefore(expiresAt);
        }

        String accessToken() {
            return token;
        }

        void update(String tokenValue, Instant start, Duration duration) {
            synchronized (lock) {
                this.token = tokenValue;
                this.expiresAt = start.plus(duration);
            }
        }
    }
}
