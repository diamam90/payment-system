package com.example.config;

import com.example.dto.keycloak.KeycloakTokenResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.ADMIN_CLI_CLIENT;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIntegrationTest {

    @Container
    protected static final dasniko.testcontainers.keycloak.KeycloakContainer container =
            new dasniko.testcontainers.keycloak.KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                    .withRealmImportFile("realm-config.json");

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


    protected Mono<String> adminToken() {
        URI tokenUrl;
        try {
            tokenUrl = new URI("http", null, container.getHost(), container.getHttpPort(), "/realms/master/protocol/openid-connect/token", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        var body = BodyInserters.fromFormData("client_id", ADMIN_CLI_CLIENT)
                .with("grant_type", "password")
                .with("username", container.getAdminUsername())
                .with("password", container.getAdminPassword());

        return WebClient.create(tokenUrl.toString())
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .map(KeycloakTokenResponse::accessToken)
                .log("admin token");
    }

    protected Mono<Void> createUser(String email, String password, String adminToken) {
        URI url;
        try {
            url = new URI("http", null, container.getHost(), container.getHttpPort(), "/admin/realms/payment/users", null, null);
        } catch (URISyntaxException e) {
            return Mono.error(new RuntimeException());
        }
        return WebClient.create(url.toString())
                .post()
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createUserRequest(email, password))
                .retrieve()
                .toBodilessEntity()
                .log("creating user")
                .flatMap(_ -> Mono.empty());
    }

    protected Mono<KeycloakTokenResponse> getUserToken(String email, String password) {
        URI tokenUrl;
        try {
            tokenUrl = new URI("http", null, container.getHost(), container.getHttpPort(), "/realms/payment/protocol/openid-connect/token", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        var body = BodyInserters.fromFormData("client_id", "individuals-api")
                .with("grant_type", "password")
                .with("username", email)
                .with("password", password);

        return WebClient.create(tokenUrl.toString())
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .log("user token");
    }

    protected Mono<KeycloakTokenResponse> refreshUserToken(String refreshToken) {
        URI tokenUrl;
        try {
            tokenUrl = new URI("http", null, container.getHost(), container.getHttpPort(), "/realms/payment/protocol/openid-connect/token", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        var body = BodyInserters.fromFormData("client_id", "individuals-api")
                .with("grant_type", "refresh_token")
                .with("refresh_token", refreshToken);

        return WebClient.create(tokenUrl.toString())
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .log("user token");
    }

    private String createUserRequest(String email, String password) {
        return """
                {
                    "username": "%s",
                    "credentials": [{
                        "type": "password",
                        "value": "%s"
                    }],
                    "enabled": true
                }
                """.formatted(email, password);
    }
}
