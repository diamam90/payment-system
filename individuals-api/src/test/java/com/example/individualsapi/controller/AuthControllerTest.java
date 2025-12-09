package com.example.individualsapi.controller;

import com.example.individuals.dto.TokenResponse;
import com.example.individuals.dto.UserInfoResponse;
import com.example.individualsapi.config.AppTestConfig;
import com.example.individualsapi.configuration.AdminTokenHolder;
import com.example.individualsapi.configuration.SecurityConfig;
import com.example.individualsapi.mapper.KeycloakMapper;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;

@Import({SecurityConfig.class,
        AppTestConfig.class,
        KeycloakMapper.class})
@WebFluxTest(controllers = AuthController.class)
class AuthControllerTest {

    @MockitoBean
    UserService userService;
    @MockitoBean
    ReactiveJwtDecoder decoder;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    AdminTokenHolder tokenHolder;

    @Autowired
    WebTestClient client;


    @Test
    void login() {
        when(userService.accessToken("user1@user.user", "password"))
                .thenReturn(Mono.just(tokenResponse()));

        client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequestJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(tokenResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void refreshToken() {
        when(userService.refreshToken("refresh token value")).thenReturn(Mono.just(tokenResponse()));

        client.post()
                .uri("/api/v1/auth/refresh-token")
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
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));
        client.get()
                .uri("/api/v1/auth/me")
                .headers(headers -> headers.setBearerAuth("superSecretToken"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("email").isEqualTo("user1@user.user")
                .json(userInfoResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void meWithoutTokenReturns401() {
        when(userService.currentUser("user-id-228")).thenReturn(Mono.just(userInfoResponse()));
        client.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
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
                    "roles": [],
                    "individualId": "00000000-0000-0000-0000-000000000003"
                }
                """;
    }



    private TokenResponse tokenResponse() {
        var token = new TokenResponse();
        token.setTokenType("access_token");
        token.setAccessToken("access token value");
        token.setRefreshToken("refresh token value");
        token.setExpiresIn(24);

        return token;
    }

    private UserInfoResponse userInfoResponse() {
        var response = new UserInfoResponse();
        response.setEmail("user1@user.user");
        response.setId("user-id-228");
        response.setCreatedAt(ZonedDateTime.of(2025, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC));
        response.setIndividualId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        return response;
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("access token value")
                .subject("user-id-228")
                .header("name", 12)
                .build();
    }
}