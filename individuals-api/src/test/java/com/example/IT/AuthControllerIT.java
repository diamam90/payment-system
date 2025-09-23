package com.example.IT;

import com.example.config.BaseIntegrationTest;
import com.example.dto.TokenResponse;
import com.example.dto.keycloak.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;


@AutoConfigureWebTestClient
public class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    WebTestClient client;

    @Test
    void shouldRegister() {

        // given
        var email = "use1@user.user";
        var password = "password1";

        // when
        TokenResponse tokenResponse = client.post()
                .uri("/api/v1/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest(email, password, password))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(TokenResponse.class)
                .getResponseBody().blockFirst();
        // then
        client.get()
                .uri("/api/v1/me")
                .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldLogin() {
        // given
        var email = "use2@user.user";
        var password = "password2";

        // получение токена админа
        adminToken()
                // Создание пользователя
                .flatMap(token -> createUser(email, password, token))
                .block();

        // when
        // логин
        var tokenResponse = client.post()
                .uri("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenResponse.class).getResponseBody().blockFirst();

        // then
        var refreshResponse = refreshUserToken(tokenResponse.getRefreshToken()).block();
        assertThat(refreshResponse).extracting("accessToken").isNotNull();
    }

    @Test
    void shouldRefreshToken() {
        // given
        var email = "use3@user.user";
        var password = "password3";
        // when
        var refreshToken = adminToken()
                .flatMap(token -> createUser(email, password, token))
                .then(getUserToken(email, password))
                .map(KeycloakTokenResponse::refreshToken)
                .block();

        var tokenResponse = client.post()
                .uri("/api/v1/refresh-token").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refreshTokenRequest(refreshToken))
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenResponse.class)
                .getResponseBody()
                .blockFirst();

        // then
        var refreshResponse = refreshUserToken(tokenResponse.getRefreshToken()).block();
        assertThat(refreshResponse).extracting("accessToken").isNotNull();

    }

    @Test
    void shouldReturnUserInfo() {
        //given
        var email = "use4@user.user";
        var password = "password4";

        var userToken = adminToken()
                .flatMap(token -> createUser(email, password, token))
                .then(getUserToken(email, password))
                .map(KeycloakTokenResponse::accessToken)
                .block();

        // when
        client.get()
                .uri("/api/v1/me")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("id").isNotEmpty()
                .jsonPath("email").isEqualTo(email);
    }

    private String loginRequest(String email, String password) {
        return """
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                """.formatted(email, password);
    }

    private String registrationRequest(String email, String password, String confirmPassword) {
        return """
                    {
                        "email": "%s",
                        "password": "%s",
                        "confirm_password": "%s"
                    }
                """.formatted(email, password, confirmPassword);
    }

    private String refreshTokenRequest(String refreshToken) {
        return """
                    {
                        "refresh_token": "%s"
                    }
                """.formatted(refreshToken);
    }
}
