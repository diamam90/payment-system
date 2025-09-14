package com.example.service.impl;

import com.example.client.KeycloakClient;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.dto.keycloak.KeycloakUserInfoResponse;
import com.example.dto.keycloak.KeycloakUserRegistrationRequest;
import com.example.exception.BadRequestException;
import com.example.mapper.KeycloakMapper;
import com.example.service.TokenService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    TokenService tokenService;
    @Mock
    KeycloakClient client;
    @Spy
    KeycloakMapper mapper;
    @InjectMocks
    UserServiceImpl userService;
    @Mock
    MeterRegistry meterRegistry;

    @Test
    void shouldRegister() {
        var adminToken = adminToken();
        var keycloakRequest = new KeycloakUserRegistrationRequest("user1",
                List.of(new KeycloakUserRegistrationRequest.CredentialDto("password", "password user")), true);
        var userRegistrationRequest = registrationRequest();
        var tokenResponse = userToken();

        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken));
        when(client.registration(keycloakRequest, "admin access token"))
                .thenReturn(Mono.just(Mockito.mock(Void.class)));
        when(tokenService.accessToken("user1", "password user")).thenReturn(Mono.just(keycloakUserToken()));

        var actual = userService.register(userRegistrationRequest);

        StepVerifier.create(actual)
                .expectNext(tokenResponse)
                .verifyComplete();
    }

    @Test
    void registerWhenPasswordsNotEqualsShouldReturnError() {
        var request = registrationRequest();
        request.setConfirmPassword("not equals password");

        // TODO  почему нужно объявлять эти заглушки, и как этого избежать?
        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken()));
        when(tokenService.accessToken("user1", "password user")).thenReturn(Mono.just(keycloakUserToken()));

        var actual = userService.register(request);
        StepVerifier.create(actual)
                .expectError(BadRequestException.class)
                .verify();
    }


    @Test
    void shouldReturnCurrentUser() {
        var adminToken = adminToken();

        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken));
        when(client.userInfo("client256", "admin access token")).thenReturn(Mono.just(keycloakUserInfoResponse()));

        var actual = userService.currentUser("client256");

        StepVerifier.create(actual)
                .expectNext(userInfoResponse())
                .verifyComplete();
    }

    @Test
    void currentUserWhenUserInfoShouldReturnError() {
        var adminToken = adminToken();

        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken));
        when(client.userInfo("client256", "admin access token")).thenReturn(userInfoError());

        var actual = userService.currentUser("client256");

        StepVerifier.create(actual)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void currentUserWhenAdminTokenRequestsShouldReturnError() {
        when(tokenService.adminToken()).thenReturn(adminTokenError());

        var actual = userService.currentUser("1234-321");
        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    private UserInfoResponse userInfoResponse() {
        var userInfo = new UserInfoResponse();
        userInfo.setId("123-228");
        userInfo.setCreatedAt(OffsetDateTime.of(2025, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC));
        userInfo.setEmail("user@user.user");
        return userInfo;
    }

    private KeycloakUserInfoResponse keycloakUserInfoResponse() {
        return new KeycloakUserInfoResponse("123-228",
                "user@user.user",
                OffsetDateTime.parse("2025-05-05T00:00:00Z").toInstant().toEpochMilli());
    }

    private UserRegistrationRequest registrationRequest() {
        var request = new UserRegistrationRequest();
        request.setEmail("user1");
        request.setPassword("password user");
        request.setConfirmPassword("password user");
        return request;
    }

    private TokenResponse userToken() {
        var tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("user access token");
        tokenResponse.setRefreshToken("user refresh token");
        tokenResponse.setExpiresIn(33);
        tokenResponse.setTokenType("access_token");
        return tokenResponse;
    }

    private KeycloakTokenResponse adminToken() {
        return new KeycloakTokenResponse("admin access token",
                "admin refresh token",
                23,
                32,
                "access_token");
    }

    private KeycloakTokenResponse keycloakUserToken() {
        return new KeycloakTokenResponse("user access token",
                "user refresh token",
                33,
                34,
                "access_token");
    }

    private Mono<KeycloakTokenResponse> adminTokenError() {
        return Mono.error(() -> new WebClientResponseException(403, "invalid token request", null, null, null));
    }

    private Mono<KeycloakUserInfoResponse> userInfoError() {
        return Mono.error(() -> new WebClientResponseException(404, "user not found", null, null, null));
    }
}