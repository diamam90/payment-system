package com.example.individualsapi.IT;

import com.example.individuals.dto.TokenResponse;
import com.example.individualsapi.config.BaseIntegrationTest;
import com.example.individualsapi.dto.keycloak.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@AutoConfigureWebTestClient
public class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    WebTestClient client;

    private static final String EMAIL = "test@user.test";
    private static final String PASSWORD = "user_test_password";

    @Test
    void keycloakFunctionTest() {
        // given
        String adminToken = adminToken().block();
        String userId = createUser(EMAIL, PASSWORD, adminToken).block();
        var userInfo = getUserInfo(userId, adminToken).block();
        assertThat(userInfo).isNotNull();
        // when
        deleteUser(userId, adminToken).block();
        // then
        assertThatThrownBy(() -> getUserInfo(userId, adminToken).block())
                .isExactlyInstanceOf(WebClientResponseException.NotFound.class);
    }

    @Test
    void shouldLogin() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser(EMAIL, PASSWORD, adminToken).block();

        // when
        var tokenResponse = client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest(EMAIL, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenResponse.class).getResponseBody().blockFirst();

        // then
        var refreshResponse = refreshUserToken(tokenResponse.getRefreshToken()).block();
        assertThat(refreshResponse).extracting("accessToken").isNotNull();
        deleteUser(userId, adminToken).block();
    }

    @Test
    void shouldRefreshToken() {
        // given
        var adminToken = adminToken().block();
        var userId = createUser(EMAIL, PASSWORD, adminToken).block();
        // when
        var refreshToken = userTokenResponse(EMAIL, PASSWORD)
                .map(KeycloakTokenResponse::refreshToken)
                .block();

        var tokenResponse = client.post()
                .uri("/api/v1/auth/refresh-token").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refreshTokenRequest(refreshToken))
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenResponse.class)
                .getResponseBody()
                .blockFirst();

        // then
        var refreshResponse = refreshUserToken(tokenResponse.getRefreshToken()).block();
        assertThat(refreshResponse).extracting("accessToken").isNotNull();

        deleteUser(userId, adminToken).block();
    }

    @Test
    void shouldReturnUserInfo() {
        //given
        var adminToken = adminToken().block();
        var userId = createUser(EMAIL, PASSWORD, adminToken).block();
        var userToken = userTokenResponse(EMAIL, PASSWORD)
                .map(KeycloakTokenResponse::accessToken)
                .block();

        // when
        client.get()
                .uri("/api/v1/auth/me")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("id").isNotEmpty()
                .jsonPath("email").isEqualTo(EMAIL);

        deleteUser(userId, adminToken).block();
    }

    private String loginRequest(String email, String password) {
        return """
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                """.formatted(email, password);
    }

    private String refreshTokenRequest(String refreshToken) {
        return """
                    {
                        "refresh_token": "%s"
                    }
                """.formatted(refreshToken);
    }
}
