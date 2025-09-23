package com.example.service.impl;

import com.example.client.KeycloakClient;
import com.example.exception.BadRequestException;
import com.example.mapper.KeycloakMapper;
import com.example.service.TokenService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.example.stub.AuthDtoStub.*;
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
        var keycloakRequest = keycloakUserRegistrationRequest();
        var userRegistrationRequest = registrationRequest();
        var tokenResponse = userToken();

        when(client.registration(keycloakRequest)).thenReturn(Mono.empty());
        when(tokenService.accessToken("user1", "password user")).thenReturn(keycloakUserToken());

        var actual = userService.register(userRegistrationRequest);

        StepVerifier.create(actual).expectNext(tokenResponse).verifyComplete();
    }

    @Test
    void registerWhenPasswordsNotEqualsShouldReturnError() {
        var keycloakRequest = keycloakUserRegistrationRequest();
        var request = registrationRequest();
        request.setConfirmPassword("not equals password");

        when(client.registration(keycloakRequest)).thenReturn(Mono.empty());
        when(tokenService.accessToken("user1", "password user")).thenReturn(keycloakUserToken());

        var actual = userService.register(request);

        StepVerifier.create(actual).expectError(BadRequestException.class).verify();
    }

    @Test
    void shouldReturnCurrentUser() {
        when(client.userInfo("client256")).thenReturn(keycloakUserInfoResponse());

        var actual = userService.currentUser("client256");

        StepVerifier.create(actual).expectNext(userInfoResponse()).verifyComplete();
    }

    @Test
    void currentUserWhenUserInfoShouldReturnError() {
        when(client.userInfo("client256")).thenReturn(userInfoError());

        var actual = userService.currentUser("client256");

        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void shouldReturnAccessToken() {
        var email = "email@email.email";
        var password = "password";

        when(tokenService.accessToken(email, password)).thenReturn(keycloakUserToken());

        var actual = userService.accessToken(email, password);

        StepVerifier.create(actual).expectNext(userToken()).verifyComplete();
    }

    @Test
    void accessTokenShouldReturnError() {
        var email = "email@email.email";
        var password = "password";

        when(tokenService.accessToken(email, password)).thenReturn(accessDenied());

        var actual = userService.accessToken(email, password);

        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void shouldRefreshToken() {
        var refreshToken = "refresh token value";
        when(tokenService.refreshToken(refreshToken)).thenReturn(keycloakUserToken());

        var actual = userService.refreshToken(refreshToken);

        StepVerifier.create(actual).expectNext(userToken()).verifyComplete();
    }

    @Test
    void refreshTokenShouldReturnError() {
        var refreshToken = "refresh token value";
        when(tokenService.refreshToken(refreshToken)).thenReturn(accessDenied());

        var actual = userService.refreshToken(refreshToken);

        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }
}