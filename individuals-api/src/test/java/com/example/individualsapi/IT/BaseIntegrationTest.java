package com.example.individualsapi.IT;

import com.example.individualsapi.config.AppPropertiesTestConfig;
import com.example.individualsapi.config.MockServiceConfig;
import com.example.individualsapi.config.SecurityTestConfig;
import com.example.individualsapi.dto.keycloak.KeycloakTokenResponse;
import com.example.individualsapi.dto.keycloak.KeycloakUserInfoResponse;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SpringBootTest
@EnabledIfDockerAvailable
@Import({MockServiceConfig.class, SecurityTestConfig.class, AppPropertiesTestConfig.class})
public abstract class BaseIntegrationTest {

    public static final String CLIENT_ID = "individuals-api";
    public static final String CLIENT_SECRET = "**********";

    private static final String ADMIN_TOKEN_PATH = "/realms/payment/protocol/openid-connect/token";
    private static final String CREATE_USER_PATH = "/admin/realms/payment/users";
    private static final String USER_ID_PATH = "/admin/realms/payment/users/{userId}";
    private static final String USER_TOKEN_PATH = "/realms/payment/protocol/openid-connect/token";

    @Autowired
    SecurityTestConfig securityTestConfig;

    protected Mono<String> adminToken() {
        String url = securityTestConfig.getBaseUrl() + ADMIN_TOKEN_PATH;

        var body = BodyInserters.fromFormData("client_id", CLIENT_ID)
                .with("grant_type", "client_credentials")
                .with("client_secret", CLIENT_SECRET);

        return WebClient.create(url)
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .map(KeycloakTokenResponse::accessToken)
                .log("admin token");
    }

    // return userId
    protected Mono<String> createUser(String email, String password, String adminToken, @Nullable UUID individualId) {
        String url = securityTestConfig.getBaseUrl() + CREATE_USER_PATH;
        return WebClient.create()
                .post()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createUserRequest(email, password, individualId))
                .retrieve()
                .toBodilessEntity()
                .log("creating user")
                .map(ResponseEntity::getHeaders)
                .map(headers -> headers.getFirst("Location"))
                .map(this::extractKeycloakId);
    }

    protected Mono<String> createUser(String email, String password, String adminToken) {
        return createUser(email, password, adminToken, null);
    }

    protected Mono<Void> deleteUser(String keycloakId, String adminToken) {
        String url = securityTestConfig.getBaseUrl() + USER_ID_PATH;
        return WebClient.create()
                .delete()
                .uri(url, keycloakId)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .retrieve()
                .toBodilessEntity()
                .log("deleting user")
                .flatMap(_ -> Mono.empty());
    }

    protected Mono<KeycloakUserInfoResponse> getUserInfo(String keycloakUserId, String adminToken) {
        String url = securityTestConfig.getBaseUrl() + USER_ID_PATH;
        return WebClient.create()
                .get()
                .uri(url, keycloakUserId)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .retrieve()
                .bodyToMono(KeycloakUserInfoResponse.class)
                .log("getting user");
    }

    protected Mono<KeycloakTokenResponse> userTokenResponse(String email, String password) {
        String url = securityTestConfig.getBaseUrl() + USER_TOKEN_PATH;

        var body = BodyInserters.fromFormData("client_id", CLIENT_ID)
                .with("grant_type", "password")
                .with("username", email)
                .with("password", password)
                .with("client_secret", CLIENT_SECRET);

        return WebClient.create(url)
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .log("user token");
    }

    protected Mono<KeycloakTokenResponse> refreshUserToken(String refreshToken) {
        String url = securityTestConfig.getBaseUrl() + USER_TOKEN_PATH;
        var body = BodyInserters.fromFormData("client_id", CLIENT_ID)
                .with("grant_type", "refresh_token")
                .with("refresh_token", refreshToken)
                .with("client_secret", CLIENT_SECRET);

        return WebClient.create(url)
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class);
    }

    private String createUserRequest(String email, String password, UUID individualId) {
        if (individualId == null) {
            return // language=JSON
                    """
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

        return // language=JSON
                """
                        {
                            "username": "%s",
                            "credentials": [{
                                "type": "password",
                                "value": "%s"
                            }],
                            "enabled": true,
                            "attributes":{
                                    "individualId":"%s"
                            }
                        }
                        """.formatted(email, password, individualId);
    }

    private String extractKeycloakId(String location) {
        String[] data = location.split("/");
        return data[data.length - 1];
    }
}
