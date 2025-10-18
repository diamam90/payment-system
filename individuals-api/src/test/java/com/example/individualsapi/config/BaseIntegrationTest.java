package com.example.individualsapi.config;

import com.example.individualsapi.dto.keycloak.KeycloakTokenResponse;
import com.example.individualsapi.dto.keycloak.KeycloakUserInfoResponse;
import jakarta.annotation.Nullable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SpringBootTest
public abstract class BaseIntegrationTest {

    public static final String CLIENT_ID = "individuals-api";
    public static final String CLIENT_SECRET = "**********";
    protected static final String ADMIN = "payment_admin";
    protected static final String PASSWORD = "payment_admin";

    private static final String ADMIN_TOKEN_PATH = "/realms/payment/protocol/openid-connect/token";
    private static final String CREATE_USER_PATH = "/admin/realms/payment/users";
    private static final String USER_ID_PATH = "/admin/realms/payment/users/{userId}";
    private static final String USER_TOKEN_PATH = "/realms/payment/protocol/openid-connect/token";
    protected static final String JWK_PATH = "/realms/payment/protocol/openid-connect/certs";

    private static final String URL_TEMPLATE = "http://%s:%s%s";

    static dasniko.testcontainers.keycloak.KeycloakContainer keycloak =
            new dasniko.testcontainers.keycloak.KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                    .withRealmImportFile("realm-config.json");

    static WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock:3.13.2")
            .withMappingFromResource("createIndividual", BaseIntegrationTest.class, "/mapping/create-individual.json")
            .withMappingFromResource("updateIndividual", BaseIntegrationTest.class, "/mapping/update-individual.json")
            .withMappingFromResource("findByEmailIndividual", BaseIntegrationTest.class, "/mapping/find-by-email-individual.json")
            .withMappingFromResource("findByIdIndividual", BaseIntegrationTest.class, "/mapping/find-by-id-individual.json")
            .withMappingFromResource("findByIdIndividual404", BaseIntegrationTest.class, "/mapping/find-by-id-individual-404.json")
            .withMappingFromResource("updateIndividual404", BaseIntegrationTest.class, "/mapping/update-individual-404.json")
            .withMappingFromResource("findByEmailIndividual404", BaseIntegrationTest.class, "/mapping/find-by-email-individual-404.json")
            .withMappingFromResource("hardDeleteIndividual", BaseIntegrationTest.class, "/mapping/hard-delete-individual.json")
            .withMappingFromResource("activateIndividual", BaseIntegrationTest.class, "/mapping/activate-individual.json")
            .withMappingFromResource("softDeleteIndividual", BaseIntegrationTest.class, "/mapping/soft-delete-individual.json");

    static {
        wiremock.start();
        keycloak.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String host = keycloak.getHost();
        int port = keycloak.getHttpPort();

        String baseUrl = "http://%s:%s".formatted(host, port);
        String jwkUri = baseUrl + JWK_PATH;

        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> jwkUri);
        registry.add("individuals-api.keycloak.baseUrl", () -> baseUrl);
        registry.add("individuals-api.keycloak.username", () -> ADMIN);
        registry.add("individuals-api.keycloak.password", () -> PASSWORD);
        registry.add("individuals-api.keycloak.clientId", () -> CLIENT_ID);
        registry.add("individuals-api.keycloak.clientSecret", () -> CLIENT_SECRET);
        registry.add("individuals-api.person.base-url", wiremock::getBaseUrl);
    }

    protected Mono<String> adminToken() {
        String url = URL_TEMPLATE.formatted(keycloak.getHost(), keycloak.getHttpPort(), ADMIN_TOKEN_PATH);

        var body = BodyInserters.fromFormData("client_id", CLIENT_ID)
                .with("grant_type", "password")
                .with("username", ADMIN)
                .with("password", PASSWORD)
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
        String url = URL_TEMPLATE.formatted(keycloak.getHost(), keycloak.getHttpPort(), CREATE_USER_PATH);
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
        String url = URL_TEMPLATE.formatted(keycloak.getHost(), keycloak.getHttpPort(), USER_ID_PATH);
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
        String url = URL_TEMPLATE.formatted(keycloak.getHost(), keycloak.getHttpPort(), USER_ID_PATH);
        return WebClient.create()
                .get()
                .uri(url, keycloakUserId)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .retrieve()
                .bodyToMono(KeycloakUserInfoResponse.class)
                .log("getting user");
    }

    protected Mono<KeycloakTokenResponse> userTokenResponse(String email, String password) {
        String url = URL_TEMPLATE.formatted(keycloak.getHost(), keycloak.getHttpPort(), USER_TOKEN_PATH);

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
        String url = URL_TEMPLATE.formatted(keycloak.getHost(), keycloak.getHttpPort(), USER_TOKEN_PATH);
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
