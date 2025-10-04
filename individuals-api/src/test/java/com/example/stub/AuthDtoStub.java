package com.example.stub;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.dto.keycloak.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class AuthDtoStub {

    public static String clientId = "clientId";


    public static UserInfoResponse userInfoResponse() {
        var userInfo = new UserInfoResponse();
        userInfo.setId("123-228");
        userInfo.setCreatedAt(ZonedDateTime.of(2025, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC));
        userInfo.setEmail("user@user.user");
        return userInfo;
    }

    public static UserRegistrationRequest registrationRequest() {
        var request = new UserRegistrationRequest();
        request.setEmail("user1");
        request.setPassword("password user");
        request.setConfirmPassword("password user");
        return request;
    }

    public static TokenResponse userToken() {
        var tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("user access token");
        tokenResponse.setRefreshToken("user refresh token");
        tokenResponse.setExpiresIn(33);
        tokenResponse.setTokenType("access_token");
        return tokenResponse;
    }

    public static KeycloakUserTokenRequest keycloakUserTokenRequest() {
        return new KeycloakUserTokenRequest(clientId,
                "password",
                "ya@ya.ru",
                "pass");
    }

    public static KeycloakUserRefreshTokenRequest keycloakUserRefreshTokenRequest() {
        return new KeycloakUserRefreshTokenRequest("refresh_token", "token value", clientId);
    }

    public static KeycloakUserRegistrationRequest keycloakUserRegistrationRequest() {
        return new KeycloakUserRegistrationRequest("user1",
                List.of(new KeycloakUserRegistrationRequest.CredentialDto("password", "password user")),
                true);
    }

    public static Mono<KeycloakTokenResponse> keycloakUserToken() {
        return Mono.just(new KeycloakTokenResponse("user access token",
                "user refresh token",
                33,
                34,
                "access_token"));
    }

    public static Mono<KeycloakTokenResponse> accessDenied() {
        return Mono.error(() -> new WebClientResponseException(403, "invalid credentials", null, null, null));
    }

    public static Mono<KeycloakUserInfoResponse> keycloakUserInfoResponse() {
        return Mono.just(new KeycloakUserInfoResponse("123-228",
                "user@user.user",
                OffsetDateTime.parse("2025-05-05T00:00:00Z").toInstant().toEpochMilli()));
    }

    public static Mono<KeycloakUserInfoResponse> userInfoError() {
        return Mono.error(() -> new WebClientResponseException(404, "user not found", null, null, null));
    }
}
