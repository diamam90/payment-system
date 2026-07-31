package com.example.individualsapi.client;

import com.example.individualsapi.configuration.AppProperties;
import com.example.individualsapi.dto.keycloak.*;
import com.example.individualsapi.exception.AccessDeniedException;
import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.exception.ExternalServiceUnavailableException;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class KeycloakClient {

    private final WebClient keycloakClient;
    private final AppProperties properties;

    public KeycloakClient(AppProperties properties, ObservationRegistry registry) {
        this.properties = properties;
        var baseUrl = properties.getKeycloak().getBaseUrl();
        log.debug("keycloak base url: {}", baseUrl);
        this.keycloakClient = WebClient.builder()
                .baseUrl(baseUrl)
                .observationRegistry(registry)
                .build();
    }

    private static final String USERS_URI = "/admin/realms/{realm}/users";
    private static final String USER_URI = "/admin/realms/{realm}/users/{userId}";
    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String ADMIN_TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";

    @NewSpan("keycloak_client.register")
    public Mono<Void> registration(KeycloakUserRegistrationRequest request, String adminToken) {
        return keycloakClient.post()
                .uri(USERS_URI, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .bodyValue(request)
                .exchangeToMono(handleMonoResponse(Void.class));
    }

    @NewSpan("keycloak_client.delete_user")
    public Mono<Void> deleteUser(String keycloakUserId, String adminToken) {
        return keycloakClient.delete()
                .uri(USER_URI, properties.getKeycloak().getRealm(), keycloakUserId)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchangeToMono(handleMonoResponse(Void.class));
    }

    @NewSpan("keycloak_client.auth")
    public Mono<KeycloakTokenResponse> auth(KeycloakUserTokenRequest request) {
        return keycloakClient.post()
                .uri(TOKEN_URI, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(handleMonoResponse(KeycloakTokenResponse.class));
    }

    @NewSpan("keycloak_client.refresh")
    public Mono<KeycloakTokenResponse> refresh(KeycloakUserRefreshTokenRequest request) {
        return keycloakClient.post()
                .uri(TOKEN_URI, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(request))
                .exchangeToMono(handleMonoResponse(KeycloakTokenResponse.class));
    }

    @NewSpan("keycloak_client.user_info")
    public Mono<KeycloakUserInfoResponse> userInfo(String userId, String token) {
        return keycloakClient.get()
                .uri(USER_URI, properties.getKeycloak().getRealm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .exchangeToMono(handleMonoResponse(KeycloakUserInfoResponse.class));
    }

    @NewSpan("keycloak_client.admin_token")
    public Mono<KeycloakTokenResponse> adminToken(KeycloakUserTokenRequest adminTokenRequest) {
        return keycloakClient.post()
                .uri(ADMIN_TOKEN_URI, properties.getKeycloak().getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(convertToMap(adminTokenRequest))
                .exchangeToMono(handleMonoResponse(KeycloakTokenResponse.class));
    }

    @NewSpan("keycloak_client.find_by_individual_id")
    public Flux<KeycloakUserInfoResponse> findByIndividualId(UUID individualId, String adminToken) {
        return keycloakClient.get()
                .uri(USERS_URI + "?q=individualId:{individualId}", properties.getKeycloak().getRealm(), individualId)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchangeToFlux(handleFluxResponse(KeycloakUserInfoResponse.class));
    }

    private <T> Function<ClientResponse, Mono<T>> handleMonoResponse(Class<T> clazz) {
        return response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(clazz);
            } else {
                return response.createError();
            }
        };
    }

    private <T> Function<ClientResponse, Flux<T>> handleFluxResponse(Class<T> clazz) {
        return response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToFlux(clazz);
            }

            if (response.statusCode().equals(HttpStatus.UNAUTHORIZED) || response.statusCode().equals(HttpStatus.FORBIDDEN)) {
                return Flux.error(new AccessDeniedException(ExternalService.KEYCLOAK, response.statusCode().value()));
            }

            return Flux.error(new ExternalServiceUnavailableException(ExternalService.KEYCLOAK));
        };
    }

    private MultiValueMap<String, String> convertToMap(KeycloakUserTokenRequest request) {
        var map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", request.clientId());
        map.add("client_secret", request.clientSecret());
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
        map.add("client_secret", request.clientSecret());

        return map;
    }
}
