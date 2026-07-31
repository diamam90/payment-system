package com.example.individualsapi.IT;

import com.example.individuals.dto.ErrorResponse;
import com.example.individuals.dto.TokenResponse;
import com.example.individuals.dto.UserInfoResponse;
import com.example.individuals.dto.UserResponse;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.configuration.AppProperties;
import com.example.individualsapi.service.impl.PersonService;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@EnabledIfDockerAvailable
@AutoConfigureWebTestClient
public class PersonControllerIT extends BaseIntegrationTest {

    @Autowired
    WebTestClient client;
    @Autowired
    AppProperties properties;
    @MockitoSpyBean
    PersonService personService;
    @MockitoSpyBean
    KeycloakClient keycloakClient;

    private static final UUID individualId = UUID.fromString("00000000-0000-0000-0000-000011110000");

    @Test
    void shouldRegister() {
        // given
        var requestBody = userRequest();
        var adminToken = adminToken().block();
        // when
        TokenResponse tokenResponse = client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(TokenResponse.class)
                .getResponseBody().blockFirst();
        // then
        var userInfo = client.get()
                .uri("/api/v1/auth/me")
                .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(UserInfoResponse.class)
                .getResponseBody().blockFirst();
        verify(personService, never()).compensateCreation(any());

        deleteUser(userInfo.getId(), adminToken).block();
    }

    @Test
    void register_WhenKeycloakReturnError_ShouldCompensate() {
        // given
        var requestBody = userRequest();
        var adminToken = adminToken().block();
        // needed that keycloak returns 409 status
        var userId = createUser("user@aaa.ru", "34222", adminToken).block();
        // when
        var errorResponse = client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                // then
                .expectStatus().isEqualTo(409)
                .returnResult(ErrorResponse.class)
                .getResponseBody().blockFirst();

        assertThat(errorResponse)
                .hasNoNullFieldsOrProperties()
                .hasFieldOrPropertyWithValue("status", 409);

        verify(personService).compensateCreation(any());

        deleteUser(userId, adminToken).block();
    }

    @Test
    void registration_WhenRequestInvalid_ShouldReturn400() {
        // given
        var requestBody = invalidRequest();
        // when
        var errorResponse = client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .returnResult(ProblemDetail.class)
                .getResponseBody().blockFirst();

        verify(personService, never()).compensateCreation(any());
        verify(personService, never()).create(any());

        assertThat(errorResponse)
                .hasFieldOrPropertyWithValue("properties.error", "Bad Request")
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void registration_WhenPersonServiceIsUnavailable_ShouldReturn502() {
        var requestBody = userRequestUnavailable();
        // when
        ErrorResponse errorResponse = client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().is5xxServerError()
                .returnResult(ErrorResponse.class)
                .getResponseBody().blockFirst();
        // then
        assertThat(errorResponse)
                .hasFieldOrPropertyWithValue("status", 502)
                .hasFieldOrPropertyWithValue("error", "Service Person-service unavailable");

        verify(personService, never()).compensateCreation(any());
        verify(keycloakClient, never()).registration(any(), any());
    }

    @Test
    void shouldUpdate() {
        // given
        var requestBody = userRequest();
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();

        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        // when
        var response = client.post()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .returnResult(UserResponse.class)
                .getResponseBody().blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("id", individualId)
                .hasFieldOrPropertyWithValue("firstName", "alex")
                .hasFieldOrPropertyWithValue("email", "user@aaa.ru")
                .hasFieldOrPropertyWithValue("lastName", "Ivanov")
                .hasFieldOrPropertyWithValue("passportNumber", "12211")
                .hasFieldOrPropertyWithValue("phoneNumber", "880009000")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2020-05-05T10:00:00Z"))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2020-05-05T10:00:00Z"));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void update_WhenIndividualNotFound_ShouldReturn404() {
        // given

        var individualId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        var requestBody = userRequest();
        var adminToken = adminToken().block();
        var userId = createUser("user@aaa.ru", "34222", adminToken, individualId).block();

        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();
        // when
        var response = client.post()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNotFound()
                .returnResult(ErrorResponse.class)
                .getResponseBody().blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("error", "Individual with id %s not found".formatted(individualId))
                .hasFieldOrPropertyWithValue("status", 404);

        deleteUser(userId, adminToken).block();
    }

    @Test
    void shouldDeleteUser() {
        var adminToken = adminToken().block();
        createUser("user@aaa.ru", "34222", adminToken, individualId).block();

        var tokenResponse = userTokenResponse("user@aaa.ru", "34222").block();
        var userToken = tokenResponse.accessToken();

        client.delete()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isNoContent();
        verify(personService, never()).compensateDeletion(any());
        verify(keycloakClient).findByIndividualId(eq(individualId), any());
        verify(keycloakClient).deleteUser(any(), any());
    }

    @Test
    void findByEmail() {
        var adminToken = adminToken().block();

        var email = "test@email.test";
        var userId = createUser(email, "34222", adminToken).block();
        // when
        var response = client.get()
                .uri("/api/v1/individuals?email={email}", email)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .returnResult(UserResponse.class)
                .getResponseBody().blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("id", UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .hasFieldOrPropertyWithValue("firstName", "Fedor")
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("lastName", "Petrov")
                .hasFieldOrPropertyWithValue("passportNumber", "20200202")
                .hasFieldOrPropertyWithValue("phoneNumber", "+79009201")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2025-12-31T22:00:45.35+04:00")
                        .withZoneSameInstant(ZoneOffset.UTC))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-12-31T22:00:45.35+04:00")
                        .withZoneSameInstant(ZoneOffset.UTC))
                .hasFieldOrPropertyWithValue("status", "active")
                .hasFieldOrPropertyWithValue("address.id", UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .hasFieldOrPropertyWithValue("address.country", "russia")
                .hasFieldOrPropertyWithValue("address.address", "fake address")
                .hasFieldOrPropertyWithValue("address.zipCode", "228228")
                .hasFieldOrPropertyWithValue("address.city", "test city")
                .hasFieldOrPropertyWithValue("address.state", "test state");

        deleteUser(userId, adminToken).block();
    }

    @Test
    void findByEmail_WhenIndividualNotFound_ShouldReturn404() {
        var adminToken = adminToken().block();
        var email = "email@not.exist";
        var userId = createUser(email, "34222", adminToken).block();
        // when
        var response = client.get()
                .uri("/api/v1/individuals?email={email}", email)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound()
                .returnResult(ErrorResponse.class)
                .getResponseBody().blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("error", "Individual with email %s not found".formatted(email))
                .hasFieldOrPropertyWithValue("status", 404);

        deleteUser(userId, adminToken).block();
    }

    @Test
    void findById() {
        var adminToken = adminToken().block();
        var individualId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        var email = "user@aaa.ru";
        var userId = createUser(email, "34222", adminToken, individualId).block();
        var tokenResponse = userTokenResponse(email, "34222").block();
        var userToken = tokenResponse.accessToken();
        // when
        var response = client.get()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .returnResult(UserResponse.class)
                .getResponseBody().blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("id", individualId)
                .hasFieldOrPropertyWithValue("firstName", "Fedor")
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("lastName", "Petrov")
                .hasFieldOrPropertyWithValue("passportNumber", "20200202")
                .hasFieldOrPropertyWithValue("phoneNumber", "+79009201")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2025-12-31T22:00:45.35+04:00")
                        .withZoneSameInstant(ZoneOffset.UTC))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-12-31T22:00:45.35+04:00")
                        .withZoneSameInstant(ZoneOffset.UTC));

        deleteUser(userId, adminToken).block();
    }

    @Test
    void findByIdShouldReturn404() {
        var adminToken = adminToken().block();
        var individualId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        var email = "user@aaa.ru";
        var userId = createUser(email, "34222", adminToken, individualId).block();
        var tokenResponse = userTokenResponse(email, "34222").block();
        var userToken = tokenResponse.accessToken();

        // when
        var response = client.get()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isNotFound()
                .returnResult(ErrorResponse.class).getResponseBody().blockFirst();
        // then
        assertThat(response)
                .hasFieldOrPropertyWithValue("error", "Individual with id %s not found".formatted(individualId))
                .hasFieldOrPropertyWithValue("status", 404);

        deleteUser(userId, adminToken).block();
    }

    @Test
    void findById_WhenJwtIsAbsent_ShouldReturn403() {
        var individualId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        // when
        client.get()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .exchange()
                //then
                .expectStatus().isUnauthorized();
    }


    private String userRequest() {
        return """
                    {
                        "email": "user@aaa.ru",
                        "password": "34222",
                        "confirmPassword": "34222",
                        "secretKey": "super secret",
                        "firstName" : "alex",
                        "lastName": "Ivanov",
                        "passportNumber": "12211",
                        "phoneNumber": "880009000",
                        "verifiedAt": "2020-05-05T10:00:00Z",
                        "archivedAt": "2020-05-05T10:00:00Z"
                    }
                """;
    }

    private String userRequestUnavailable() {
        return """
                    {
                        "email": "person@service.unavailable",
                        "password": "34222",
                        "confirmPassword": "34222",
                        "secretKey": "super secret",
                        "firstName" : "alex",
                        "lastName": "Ivanov",
                        "passportNumber": "12211",
                        "phoneNumber": "880009000",
                        "verifiedAt": "2020-05-05T10:00:00Z",
                        "archivedAt": "2020-05-05T10:00:00Z"
                    }
                """;
    }

    private String invalidRequest() {
        // confirm password is missing
        return """
                    {
                        "email": "user@aaa.ru",
                        "password": "34222",
                        "secretKey": "super secret",
                        "firstName" : "alex",
                        "lastName": "Ivanov",
                        "passportNumber": "12211",
                        "phoneNumber": "880009000",
                        "verifiedAt": "2020-05-05T10:00:00Z",
                        "archivedAt": "2020-05-05T10:00:00Z"
                    }
                """;
    }
}
