package com.example.personservice.IT;

import com.example.personservice.config.AppContainers;
import com.example.person.dto.ErrorResponse;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import com.example.personservice.entity.Status;
import com.example.personservice.stub.request.IndividualRequestStub;
import com.example.personservice.stub.request.KeycloakRequestStub;
import com.example.personservice.util.JdbcUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.personservice.util.KeycloakUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(AppContainers.class)
@Testcontainers(disabledWithoutDocker = true)
public class IndividualControllerIT {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    JdbcUtils jdbcUtils;

    @AfterEach
    void truncate() {
        jdbcUtils.truncateCascadeTable("person.individuals");
        jdbcUtils.truncateCascadeTable("person.users");
    }

    @Test
    void shouldCreateIndividual() {
        // given
        IndividualRequest request = IndividualRequestStub.create();

        // when
        ResponseEntity<IndividualResponse> response = restTemplate.postForEntity("/api/v1/individuals", request, IndividualResponse.class);

        var id = response.getBody().getId();
        var individualParams = jdbcUtils.getIndividualParams(id);
        var userParams = jdbcUtils.getUserParams((UUID) individualParams.get("user_id"));
        var addressParams = jdbcUtils.getAddressParams((UUID) userParams.get("address_id"));

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertThat(response.getBody())
                .isNotNull()
                .hasFieldOrPropertyWithValue("email", "ya@mail.g")
                .hasFieldOrPropertyWithValue("firstName", "ffffirst")
                .hasFieldOrPropertyWithValue("lastName", "llllast")
                .hasFieldOrPropertyWithValue("secretKey", "secret")
                .hasFieldOrPropertyWithValue("passportNumber", "1111")
                .hasFieldOrPropertyWithValue("phoneNumber", "121212")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2022-02-02T02:02:02Z"))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2022-02-02T03:03:03Z"))
                .hasFieldOrPropertyWithValue("status", null)
                .hasFieldOrPropertyWithValue("address.country", "Hungary")
                .hasFieldOrPropertyWithValue("address.address", "h address")
                .hasFieldOrPropertyWithValue("address.zipCode", "111 111")
                .hasFieldOrPropertyWithValue("address.city", "h city")
                .hasFieldOrPropertyWithValue("address.state", "h state");

        assertThat(individualParams)
                .isNotEmpty()
                .containsEntry("individual_id", id)
                .containsEntry("passport_number", "1111")
                .containsEntry("phone_number", "121212")
                .containsEntry("verified_at", Timestamp.valueOf("2022-02-02 02:02:02"))
                .containsEntry("archived_at", Timestamp.valueOf("2022-02-02 03:03:03"))
                .containsEntry("status", "active");

        assertThat(userParams)
                .isNotEmpty()
                .containsEntry("secret_key", "secret")
                .containsEntry("email", "ya@mail.g")
                .containsEntry("first_name", "ffffirst")
                .containsEntry("last_name", "llllast")
                .hasEntrySatisfying("created", Objects::nonNull)
                .hasEntrySatisfying("updated", Objects::nonNull);

        assertThat(addressParams)
                .isNotEmpty()
                .containsEntry("address", "h address")
                .containsEntry("zip_code", "111 111")
                .containsEntry("city", "h city")
                .containsEntry("state", "h state")
                .hasEntrySatisfying("archived", Objects::nonNull)
                .hasEntrySatisfying("created", Objects::nonNull)
                .hasEntrySatisfying("updated", Objects::nonNull)
                .hasEntrySatisfying("country_id", Objects::nonNull);
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void shouldUpdateIndividual() {
        // given
        var request = IndividualRequestStub.update();
        var id = jdbcUtils.getIndividualIdByPassport("1331 4429");
        var userRequest = KeycloakRequestStub.user_1(id);
        var userId = createKeycloakUser(userRequest);
        var tokenResponse = accessToken(userRequest);

        var entity = createEntityWithBearerToken(request, tokenResponse.getToken());
        // when
        ResponseEntity<IndividualResponse> response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.POST, entity, IndividualResponse.class, id);

        var individualParams = jdbcUtils.getIndividualParams(id);
        var userParams = jdbcUtils.getUserParams((UUID) individualParams.get("user_id"));
        var addressParams = jdbcUtils.getAddressParams((UUID) userParams.get("address_id"));

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody()).isNotNull()
                .hasFieldOrPropertyWithValue("email", "ya@mail.g")
                .hasFieldOrPropertyWithValue("firstName", "ffffirst")
                .hasFieldOrPropertyWithValue("lastName", "llllast")
                .hasFieldOrPropertyWithValue("secretKey", "secret")
                .hasFieldOrPropertyWithValue("passportNumber", "1111")
                .hasFieldOrPropertyWithValue("phoneNumber", "121212")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2022-02-02T02:02:02Z"))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2022-02-02T03:03:03Z"))
                .hasFieldOrPropertyWithValue("status", null)
                .hasFieldOrPropertyWithValue("address.country", "Hungary")
                .hasFieldOrPropertyWithValue("address.address", "h address")
                .hasFieldOrPropertyWithValue("address.zipCode", "111 111")
                .hasFieldOrPropertyWithValue("address.city", "h city")
                .hasFieldOrPropertyWithValue("address.state", "h state");

        assertThat(individualParams)
                .isNotEmpty()
                .containsEntry("individual_id", id)
                .containsEntry("passport_number", "1111")
                .containsEntry("phone_number", "121212")
                .containsEntry("verified_at", Timestamp.valueOf("2022-02-02 02:02:02"))
                .containsEntry("archived_at", Timestamp.valueOf("2022-02-02 03:03:03"))
                .containsEntry("status", null);

        assertThat(userParams)
                .isNotEmpty()
                .containsEntry("secret_key", "secret")
                .containsEntry("email", "ya@mail.g")
                .containsEntry("first_name", "ffffirst")
                .containsEntry("last_name", "llllast")
                .hasEntrySatisfying("created", Objects::nonNull)
                .hasEntrySatisfying("updated", Objects::nonNull);

        assertThat(addressParams)
                .isNotEmpty()
                .containsEntry("address", "h address")
                .containsEntry("zip_code", "111 111")
                .containsEntry("city", "h city")
                .containsEntry("state", "h state")
                .hasEntrySatisfying("archived", Objects::nonNull)
                .hasEntrySatisfying("created", Objects::nonNull)
                .hasEntrySatisfying("updated", Objects::nonNull)
                .hasEntrySatisfying("country_id", Objects::nonNull);

        deleteKeycloakUser(userId);
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void update_whenIndividualIdNotProvided_shouldReturn403() {
        // given
        var id = jdbcUtils.getIndividualIdByPassport("1331 4429");
        var request = IndividualRequestStub.update();
        var userRequest = KeycloakRequestStub.user_1(null);
        var userId = createKeycloakUser(userRequest);
        var tokenResponse = accessToken(userRequest);
        var entity = createEntityWithBearerToken(request, tokenResponse.getToken());
        // when
        ResponseEntity<IndividualResponse> response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.POST, entity, IndividualResponse.class, id);
        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        deleteKeycloakUser(userId);
    }

    @Test
    void update_whenIdNotFound_shouldReturn404() {
        // given
        var id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        var request = IndividualRequestStub.update();
        var userRequest = KeycloakRequestStub.user_1(id);
        var userId = createKeycloakUser(userRequest);
        var tokenResponse = accessToken(userRequest);
        var entity = createEntityWithBearerToken(request, tokenResponse.getToken());
        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.POST, entity, ErrorResponse.class, id);
        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThat(response.getBody())
                .hasFieldOrPropertyWithValue("error", "Individual with id [33333333-3333-3333-3333-333333333333] not found")
                .hasFieldOrPropertyWithValue("status", 404);

        deleteKeycloakUser(userId);
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void findByEmail() {
        // given
        var email = "email@email.email";
        var id = jdbcUtils.getIndividualIdByEmail(email);
        var userRequest = KeycloakRequestStub.user_2(id, email);
        var userId = createKeycloakUser(userRequest);
        var accessToken = accessToken(userRequest);
        var entity = createEntityWithBearerToken(null, accessToken.getToken());
        // when
        ResponseEntity<IndividualResponse> response = restTemplate.exchange("/api/v1/individuals?email={email}", HttpMethod.GET, entity, IndividualResponse.class, email);
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertThat(response.getBody()).isNotNull()
                .hasFieldOrPropertyWithValue("email", "email@email.email")
                .hasFieldOrPropertyWithValue("passportNumber", "1331 4429")
                .hasFieldOrPropertyWithValue("phoneNumber", "8800")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2025-05-05T18:00:00Z"))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-06-06T10:00:00Z"))
                .hasFieldOrPropertyWithValue("firstName", "firstnamov")
                .hasFieldOrPropertyWithValue("lastName", null)
                .hasFieldOrPropertyWithValue("secretKey", "asdv232")
                .hasFieldOrPropertyWithValue("address.address", "test address. 23")
                .hasFieldOrPropertyWithValue("address.country", "Albania")
                .hasFieldOrPropertyWithValue("address.zipCode", "400400")
                .hasFieldOrPropertyWithValue("address.city", "VOLGOGRAD");

        deleteKeycloakUser(userId);
    }

    @Test
    void findByEmail_WhenNotExists_shouldReturn404() {
        // given
        var email = "email@thatnot.exists";
        var userRequest = KeycloakRequestStub.user_2(null, email);
        var userId = createKeycloakUser(userRequest);
        var accessToken = accessToken(userRequest);
        var entity = createEntityWithBearerToken(null, accessToken.getToken());
        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange("/api/v1/individuals?email={email}", HttpMethod.GET, entity, ErrorResponse.class, email);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThat(response.getBody())
                .hasFieldOrPropertyWithValue("status", 404)
                .hasFieldOrPropertyWithValue("error", "Individual with email [%s] not found".formatted(email));

        deleteKeycloakUser(userId);
    }

    @Test
    void findByEmail_WhenRequestEmailIsNotEqualKeycloakEmail_shouldReturn403() {
        // given
        var email = "email@thatnot.exists";
        var userRequest = KeycloakRequestStub.user_2(null, email + "w6");
        var userId = createKeycloakUser(userRequest);
        var accessToken = accessToken(userRequest);
        var entity = createEntityWithBearerToken(null, accessToken.getToken());
        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange("/api/v1/individuals?email={email}", HttpMethod.GET, entity, ErrorResponse.class, email);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        deleteKeycloakUser(userId);
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void findById() {
        var id = jdbcUtils.getIndividualIdByPassport("1331 4429");
        var user = KeycloakRequestStub.user_1(id);
        var userId = createKeycloakUser(user);
        var accessToken = accessToken(user);

        var entity = createEntityWithBearerToken(null, accessToken.getToken());
        ResponseEntity<IndividualResponse> response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.GET,
                entity, IndividualResponse.class, id);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertThat(response.getBody()).isNotNull()
                .hasFieldOrPropertyWithValue("email", "email@email.email")
                .hasFieldOrPropertyWithValue("passportNumber", "1331 4429")
                .hasFieldOrPropertyWithValue("phoneNumber", "8800")
                .hasFieldOrPropertyWithValue("verifiedAt", ZonedDateTime.parse("2025-05-05T18:00:00Z"))
                .hasFieldOrPropertyWithValue("archivedAt", ZonedDateTime.parse("2025-06-06T10:00:00Z"))
                .hasFieldOrPropertyWithValue("firstName", "firstnamov")
                .hasFieldOrPropertyWithValue("lastName", null)
                .hasFieldOrPropertyWithValue("secretKey", "asdv232")
                .hasFieldOrPropertyWithValue("address.address", "test address. 23")
                .hasFieldOrPropertyWithValue("address.country", "Albania")
                .hasFieldOrPropertyWithValue("address.zipCode", "400400")
                .hasFieldOrPropertyWithValue("address.city", "VOLGOGRAD");

        deleteKeycloakUser(userId);
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void findById_whenIndividualIdIsNotEqualKeycloakIndividualId_shouldReturn403() {
        var keycloakIndividualId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        var dbIndividualId = jdbcUtils.getIndividualIdByPassport("1331 4429");

        var user = KeycloakRequestStub.user_1(keycloakIndividualId);
        var userId = createKeycloakUser(user);
        var accessToken = accessToken(user);

        var entity = createEntityWithBearerToken(null, accessToken.getToken());
        ResponseEntity<ErrorResponse> response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.GET,
                entity, ErrorResponse.class, dbIndividualId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        deleteKeycloakUser(userId);
    }

    @Test
    void findById_WhenNotExists_shouldReturn404() {
        var id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        var user = KeycloakRequestStub.user_1(id);
        String keycloakUser = createKeycloakUser(user);
        var accessToken = accessToken(user);
        var entity = createEntityWithBearerToken(null, accessToken.getToken());
        var response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.GET, entity,
                ErrorResponse.class, id);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThat(response.getBody())
                .hasFieldOrPropertyWithValue("status", 404)
                .hasFieldOrPropertyWithValue("error", "Individual with id [33333333-3333-3333-3333-333333333333] not found");

        deleteKeycloakUser(keycloakUser);
    }

    @Sql("/query/individual/lastnamov.sql")
    @Test
    void softDelete() {
        var id = jdbcUtils.getIndividualIdByPassport("1331 4429");

        var individualParams = jdbcUtils.getIndividualParams(id);
        assertEquals("active", individualParams.get("status"));

        var user = KeycloakRequestStub.user_1(id);
        var userId = createKeycloakUser(user);
        var accessToken = accessToken(user);

        var entity = createEntityWithBearerToken(null, accessToken.getToken());

        var response = restTemplate.exchange("/api/v1/individuals/{id}", HttpMethod.DELETE,
                entity, Void.class, id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        individualParams = jdbcUtils.getIndividualParams(id);

        assertEquals(Status.INACTIVE.getStatusCode(), individualParams.get("status"));

        deleteKeycloakUser(userId);
    }

    private HttpEntity<?> createEntityWithBearerToken(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
