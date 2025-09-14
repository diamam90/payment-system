package com.example.controller;

import com.example.IndividualsApiApplication;
import com.example.client.KeycloakClient;
import com.example.configuration.AppProperties;
import com.example.configuration.WebConfig;
import com.example.dto.TokenRefreshRequest;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.mapper.KeycloakMapper;
import com.example.service.TokenService;
import com.example.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = IndividualsApiApplication.class)
class AuthControllerTest {

    @Autowired
    ApplicationContext ctx;

    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    KeycloakClient keycloakClient;
    @MockitoBean
    ReactiveJwtDecoder decoder;
    @MockitoBean
    WebConfig config;

    @Autowired
    KeycloakMapper mapper;
    WebTestClient client;

    @BeforeEach
    void setup() {
        client = WebTestClient
                .bindToApplicationContext(ctx)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void registration() {

        when(userService.register(regRequest()))
                .thenReturn(Mono.just(tokenResponse()));

        client.post()
                .uri("/v1/api/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regRequestJson())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().json(tokenResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void login() {
        when(tokenService.accessToken("user1@user.user", "password"))
                .thenReturn(Mono.just(keycloakTokenResponse()));

        client.post()
                .uri("/v1/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequestJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(tokenResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void refreshToken() {
        when(tokenService.refreshToken("refresh token value")).thenReturn(Mono.just(keycloakTokenResponse()));

        client.post()
                .uri("/v1/api/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "refresh_token": "refresh token value"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(tokenResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void me() {
        when(userService.currentUser("user-id-228")).thenReturn(Mono.just(userInfoResponse()));
        when(decoder.decode("azaza")).thenReturn(Mono.just(jwt()));
        client.get()
                .uri("/v1/api/me")
                .headers(headers -> headers.setBearerAuth("azaza"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("email").isEqualTo("user1@user.user");
        // TODO десериализация OffsetDateTime to number?
//                .json(userInfoResponseJson(), JsonCompareMode.STRICT); deserialize?
    }

    @Test
    void meWithoutTokenReturns401() {
        when(userService.currentUser("user-id-228")).thenReturn(Mono.just(userInfoResponse()));
        client.get()
                .uri("/v1/api/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }


    private String regRequestJson() {
        return """
                {
                    "email": "user1@user.user",
                    "password": "password",
                    "confirm_password": "password"
                }
                """;
    }

    private String tokenResponseJson() {
        return """
                    {
                        "access_token": "access token value",
                        "refresh_token": "refresh token value",
                        "expires_in": 24,
                        "token_type": "access_token"
                    }
                """;
    }

    private String loginRequestJson() {
        return """
                {
                    "email": "user1@user.user",
                    "password": "password"
                }
                """;
    }

    private String userInfoResponseJson() {
        return """
                {
                    "email": "user1@user.user",
                    "id":"user-id-228",
                    "created_at": "2025-05-05T00:00:00Z",
                    "roles": []
                }
                """;
    }

    private UserRegistrationRequest regRequest() {
        var request = new UserRegistrationRequest();
        request.setEmail("user1@user.user");
        request.setPassword("password");
        request.setConfirmPassword("password");

        return request;
    }

    private TokenRefreshRequest tokenRefreshRequest() {
        var request = new TokenRefreshRequest();
        request.setRefreshToken("refresh token value");
        return request;
    }

    private TokenResponse tokenResponse() {
        var token = new TokenResponse();
        token.setTokenType("access_token");
        token.setAccessToken("access token value");
        token.setRefreshToken("refresh token value");
        token.setExpiresIn(24);

        return token;
    }

    private KeycloakTokenResponse keycloakTokenResponse() {
        return new KeycloakTokenResponse("access token value",
                "refresh token value",
                24,
                25,
                "access_token");
    }

    private UserInfoResponse userInfoResponse() {
        var response = new UserInfoResponse();
        response.setEmail("user1@user.user");
        response.setId("user-id-228");
        response.setCreatedAt(OffsetDateTime.of(2025, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC));
        return response;
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("access token value")
                .subject("user-id-228")
                .header("name", 12)
                .build();
    }
}