package com.example.controller;

import com.example.config.AppTestConfig;
import com.example.configuration.SecurityConfig;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.mapper.KeycloakMapper;
import com.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

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
    @Autowired
    WebTestClient client;

    @Test
    void registration() {
        when(userService.register(regRequest()))
                .thenReturn(Mono.just(tokenResponse()));

        client.post()
                .uri("/api/v1/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regRequestJson())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().json(tokenResponseJson(), JsonCompareMode.STRICT);
    }

    @ParameterizedTest
    @MethodSource("invalidRegRequestJson")
    void registrationWithInvalidRequestShouldReturn400(String requestJson) {
        client.post().uri("/api/v1/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void login() {
        when(userService.accessToken("user1@user.user", "password"))
                .thenReturn(Mono.just(tokenResponse()));

        client.post()
                .uri("/api/v1/login")
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
                .uri("/api/v1/refresh-token")
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
                .uri("/api/v1/me")
                .headers(headers -> headers.setBearerAuth("azaza"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("email").isEqualTo("user1@user.user")
                .json(userInfoResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void meWithoutTokenReturns401() {
        when(userService.currentUser("user-id-228")).thenReturn(Mono.just(userInfoResponse()));
        client.get()
                .uri("/api/v1/me")
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

    private static Stream<String> invalidRegRequestJson() {
        var r1 = """
                {
                    "password": "password",
                    "confirm_password": "password"
                }
                """;
        var r2 = """
                {
                    "email": "user1@user.user",
                    "confirm_password": "password"
                }
                """;
        var r3 = """
                {
                    "email": "user1@user.user",
                    "password": "password"
                }
                """;
        return Stream.of(r1, r2, r3);
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
        return response;
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("access token value")
                .subject("user-id-228")
                .header("name", 12)
                .build();
    }
}