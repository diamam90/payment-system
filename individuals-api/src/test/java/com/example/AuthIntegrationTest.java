package com.example;

import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.service.TokenService;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.ADMIN_CLI_CLIENT;

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureWebTestClient
@SpringBootTest(classes = IndividualsApiApplication.class)
public class AuthIntegrationTest {

    @Autowired
    WebTestClient client;

    static KeycloakContainer container =
            new KeycloakContainer("quay.io/keycloak/keycloak:26.2");

    @Autowired
    private TokenService tokenService;

    static {
        container.withRealmImportFile("realm-config.json");
        container.start();
    }

    @ParameterizedTest
    @ValueSource(strings = "user1@user.user")
    void shouldRegister(String email) {

        var request = """
                    {
                        "email":"%s",
                        "password": "pass",
                        "confirm_password": "pass"
                    }
                """.formatted(email);

        client.post()
                .uri("/v1/api/registration").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("access_token").isNotEmpty()
                .jsonPath("refresh_token").isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = "user2@user.user")
    void shouldLogin(String email) {
        // получение токена админа
        adminToken()
                // Создание пользователя
                .flatMap(token -> createUser(email, token))
                .block();

        // логин
        client.post()
                .uri("/v1/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                            {
                                "email": "%s",
                                "password": "34645"
                            }
                        """.formatted(email))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("access_token").isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = "user3@user.user")
    void shouldRefreshToken(String email) {
        var refreshToken = adminToken()
                .flatMap(token -> createUser(email, token))
                .then(userTokenResponse(email)
                        .map(KeycloakTokenResponse::refreshToken))
                .block();

        client.post()
                .uri("/v1/api/refresh-token").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                            {
                                "refresh_token": "%s"
                            }
                        """.formatted(refreshToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("access_token").isNotEmpty()
                .jsonPath("refresh_token").isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = "user4@user.user")
    void shouldReturnUserInfo(String email) {
        var userToken = adminToken()
                .flatMap(token -> createUser(email, token))
                .then(userTokenResponse(email)
                        .map(KeycloakTokenResponse::accessToken))
                .block();

        client.get()
                .uri("/v1/api/me")
                .headers(headers -> headers.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("id").isNotEmpty()
                .jsonPath("email").isEqualTo(email);
    }


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String adminPassword = container.getAdminPassword();
        String adminUsername = container.getAdminUsername();
        String host = container.getHost();
        int port = container.getHttpPort();

        URI jwkUri;
        URI baseUrl;

        try {
            jwkUri = new URI("http", null, host, port, "/realms/payment/protocol/openid-connect/certs", null, null);
            baseUrl = new URI("http", null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot set URI, message", e);
        }

        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> jwkUri);
        registry.add("individuals-api.keycloak.baseUrl", () -> baseUrl);
        registry.add("individuals-api.keycloak.username", () -> adminUsername);
        registry.add("individuals-api.keycloak.password", () -> adminPassword);
        registry.add("individuals-api.keycloak.clientId", () -> "individuals-api");
    }


    private Mono<String> adminToken() {
        URI tokenUrl;
        try {
            tokenUrl = new URI("http", null, container.getHost(), container.getHttpPort(), "/realms/master/protocol/openid-connect/token", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", ADMIN_CLI_CLIENT);
        params.add("grant_type", "password");
        params.add("username", container.getAdminUsername());
        params.add("password", container.getAdminPassword());


        return WebClient.create(tokenUrl.toString())
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .map(KeycloakTokenResponse::accessToken)
                .log("admin token");
    }

    private Mono<Void> createUser(String email, String adminToken) {
        URI url = null;
        try {
            url = new URI("http", null, container.getHost(), container.getHttpPort(), "/admin/realms/payment/users", null, null);
        } catch (URISyntaxException e) {
            return Mono.error(new RuntimeException());
        }
        return WebClient.create(url.toString())
                .post()
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("""
                        {
                            "username":"%s",
                            "credentials": [{
                                "type": "password",
                                "value": "34645"
                            }],
                            "enabled": true
                        }
                        """.formatted(email)))
                .retrieve()
                .toBodilessEntity()
                .log("creating user")
                .flatMap(_ -> Mono.empty());
    }

    private Mono<KeycloakTokenResponse> userTokenResponse(String email) {
        URI tokenUrl;
        try {
            tokenUrl = new URI("http", null, container.getHost(), container.getHttpPort(), "/realms/payment/protocol/openid-connect/token", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", "individuals-api");
        params.add("grant_type", "password");
        params.add("username", email);
        params.add("password", "34645");

        return WebClient.create(tokenUrl.toString())
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .log("user token");
    }
}
