package com.example.individualsapi.controller;

import com.example.individuals.dto.TokenResponse;
import com.example.individuals.dto.UserRequest;
import com.example.individualsapi.config.AppTestConfig;
import com.example.individualsapi.configuration.AdminTokenHolder;
import com.example.individualsapi.configuration.SecurityConfig;
import com.example.individualsapi.exception.AccessDeniedException;
import com.example.individualsapi.exception.BadRequestException;
import com.example.individualsapi.exception.ExternalService;
import com.example.individualsapi.exception.ExternalServiceUnavailableException;
import com.example.individualsapi.mapper.KeycloakMapper;
import com.example.individualsapi.mapper.PersonMapper;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import com.example.person.dto.IndividualResponse;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@Import({
        SecurityConfig.class,
        AppTestConfig.class,
        KeycloakMapper.class,
        PersonMapper.class
})
@WebFluxTest(controllers = PersonController.class)
class PersonControllerTest {

    @MockitoBean
    UserService userService;
    @MockitoBean
    ReactiveJwtDecoder decoder;
    @Autowired
    WebTestClient client;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    AdminTokenHolder tokenHolder;

    private static final UUID individualId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Test
    void apiV1IndividualsPost() {
        when(userService.register(userRequest()))
                .thenReturn(Mono.just(tokenResponse()));

        client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regRequestJson())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().json(tokenResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void apiV1IndividualsPostShouldReturn400() {
        when(userService.register(userRequest()))
                .thenThrow(new BadRequestException("Invalid request"));

        client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regRequestJson())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json("""
                        {
                            "error": "Invalid request",
                            "status": 400
                        }
                        """, JsonCompareMode.STRICT);
    }

    @Test
    void apiV1IndividualsPostShouldReturn502() {
        when(userService.register(userRequest()))
                .thenThrow(new ExternalServiceUnavailableException(ExternalService.PERSON_SERVICE));

        client.post()
                .uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regRequestJson())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().value(new IsEqual<>(HttpStatus.BAD_GATEWAY.value()))
                .expectBody().json("""
                            {
                                "error": "Service Person-service is unavailable",
                                "status": 502
                            }
                        """, JsonCompareMode.STRICT);
    }

    @ParameterizedTest
    @MethodSource("invalidRegRequestJson")
    void apiV1IndividualsIndividualIdPostShouldReturn400(String requestJson) {
        client.post().uri("/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void apiV1IndividualsIndividualIdPost() {
        when(userService.updateUser(individualId, userRequest())).thenReturn(Mono.just(individualResponse()));
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));

        client.post()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> httpHeaders.setBearerAuth("superSecretToken"))
                .bodyValue(regRequestJson())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(userResponseJson(), JsonCompareMode.STRICT);
    }


    @Test
    void apiV1IndividualsIndividualIdGet() {
        when(userService.findById(individualId)).thenReturn(Mono.just(individualResponse()));
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));

        client.get()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth("superSecretToken"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(userResponseJson(), JsonCompareMode.STRICT);
    }

    @Test
    void apiV1IndividualsIndividualIdDelete() {
        when(userService.deleteUser(individualId)).thenReturn(Mono.empty());
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));

        client.delete()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth("superSecretToken"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void IndividualIdDeleteReturn404() {
        when(userService.deleteUser(individualId)).thenThrow(new BadRequestException("Bad request"));
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));

        client.delete()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth("superSecretToken"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json("""
                            {
                                "error": "Bad request",
                                "status": 400
                            }
                        """);
    }

    @Test
    void IndividualIdDeleteReturn403() {
        when(userService.deleteUser(individualId)).thenThrow(new AccessDeniedException(ExternalService.PERSON_SERVICE, 403));
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));

        client.delete()
                .uri("/api/v1/individuals/{individualId}", individualId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth("superSecretToken"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().json("""
                            {
                                "error": "Access denied",
                                "status": 403
                            }
                        """);
    }

    @Test
    void apiV1IndividualsGet() {
        var email = "email@email.email";
        when(userService.findByEmail(email)).thenReturn(Mono.just(individualResponse()));
        when(decoder.decode("superSecretToken")).thenReturn(Mono.just(jwt()));

        client.get()
                .uri("/api/v1/individuals?email={email}", email)
                .headers(httpHeaders -> httpHeaders.setBearerAuth("superSecretToken"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(userResponseJson(), JsonCompareMode.STRICT);
    }


    private String regRequestJson() {
        return """
                {
                      "email": "admin228@aaa.ru",
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

    private String userResponseJson() {
        return """
                {
                      "id": "00000000-0000-0000-0000-000000000000",
                      "email": "admin228@aaa.ru",
                      "secretKey": "super secret",
                      "firstName" : "alex",
                      "lastName": "Ivanov",
                      "passportNumber": "12211",
                      "phoneNumber": "880009000",
                      "verifiedAt": "2020-05-05T10:00:00Z",
                      "archivedAt": "2020-05-05T10:00:00Z",
                      "address": null,
                      "status": null
                  }
                """;
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

    private static Stream<String> invalidRegRequestJson() {
        // invalid email format
        var r1 = """
                {
                    "email": "admin228aaa.ru",
                    "password": "34222",
                    "confirmPassword": "342224",
                    "secretKey": "super secret",
                    "firstName" : "alex",
                    "lastName": "Ivanov",
                    "passportNumber": "12211",
                    "phoneNumber": "880009000",
                    "verifiedAt": "2020-05-05T10:00:00Z",
                    "archivedAt": "2020-05-05T10:00:00Z"
                }
                """;
        // confirm password is absent
        var r2 = """
                {
                    "email": "admin228@aaa.ru",
                    "password": "34222"
                    "secretKey": "super secret",
                    "firstName" : "alex",
                    "lastName": "Ivanov",
                    "passportNumber": "12211",
                    "phoneNumber": "880009000",
                    "verifiedAt": "2020-05-05T10:00:00Z",
                    "archivedAt": "2020-05-05T10:00:00Z"
                }
                """;
        //  password is absent
        var r3 = """
                {
                    "email": "admin228@aaa.ru",
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
        return Stream.of(r1, r2, r3);
    }

    private UserRequest userRequest() {
        var request = new UserRequest();
        request.setEmail("admin228@aaa.ru");
        request.setPassword("34222");
        request.setConfirmPassword("34222");
        request.secretKey("super secret");
        request.firstName("alex");
        request.lastName("Ivanov");
        request.passportNumber("12211");
        request.phoneNumber("880009000");
        request.verifiedAt(ZonedDateTime.parse("2020-05-05T10:00:00Z"));
        request.archivedAt(ZonedDateTime.parse("2020-05-05T10:00:00Z"));
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


    private IndividualResponse individualResponse() {
        var individual = new IndividualResponse();
        individual.setId(individualId);
        individual.setEmail("admin228@aaa.ru");
        individual.setSecretKey("super secret");
        individual.setFirstName("alex");
        individual.setLastName("Ivanov");
        individual.setPassportNumber("12211");
        individual.setPhoneNumber("880009000");
        individual.setVerifiedAt(ZonedDateTime.parse("2020-05-05T10:00:00Z"));
        individual.setArchivedAt(ZonedDateTime.parse("2020-05-05T10:00:00Z"));
        return individual;
    }


    private Jwt jwt() {
        return Jwt.withTokenValue("access token value")
                .subject("user-id-228")
                .header("name", 12)
                .build();
    }
}