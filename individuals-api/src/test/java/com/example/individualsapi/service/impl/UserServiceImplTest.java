package com.example.individualsapi.service.impl;

import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.config.WithIndividualIdUser;
import com.example.individualsapi.dto.keycloak.KeycloakUserInfoResponse;
import com.example.individualsapi.exception.BadRequestException;
import com.example.individualsapi.mapper.KeycloakMapper;
import com.example.individualsapi.mapper.PersonMapper;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import com.example.person.dto.IndividualResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.UUID;

import static com.example.individualsapi.stub.AuthDtoStub.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        UserServiceImpl.class,
        KeycloakMapper.class,
        PersonMapper.class
})
@EnableMethodSecurity
class UserServiceImplTest {

    @Autowired
    UserService userService;

    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    KeycloakClient client;
    @MockitoBean
    PersonService personService;

    private static final UUID individualId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Test
    void shouldRegister() {
        var keycloakRequest = keycloakUserRegistrationRequest(individualId);
        var userRegistrationRequest = registrationRequest();
        var tokenResponse = userToken();

        var individualResponse = new IndividualResponse();
        individualResponse.setId(individualId);

        when(personService.create(any())).thenReturn(individualResponse);
        when(client.registration(keycloakRequest, "adminToken")).thenReturn(Mono.empty());
        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken()));

        when(tokenService.accessToken("user1", "password user")).thenReturn(keycloakUserToken());

        var actual = userService.register(userRegistrationRequest);

        StepVerifier.create(actual).expectNext(tokenResponse).verifyComplete();
    }

    @Test
    void registerWhenPasswordsNotEqualsShouldReturnError() {
        var request = registrationRequest();
        request.setConfirmPassword("not equals password");
        when(tokenService.accessToken("user1", "password user")).thenReturn(keycloakUserToken());

        var actual = userService.register(request);

        StepVerifier.create(actual).expectError(BadRequestException.class).verify();
    }

    @Test
    void shouldReturnCurrentUser() {
        var token = "adminToken";
        when(client.userInfo("client256", token)).thenReturn(keycloakUserInfoResponse());
        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken()));

        var actual = userService.currentUser("client256");

        StepVerifier.create(actual).expectNext(userInfoResponse()).verifyComplete();
    }

    @Test
    void currentUserWhenUserInfoShouldReturnError() {
        var token = "adminToken";
        when(client.userInfo("client256", token)).thenReturn(userInfoError());
        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken()));

        var actual = userService.currentUser("client256");

        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void shouldReturnAccessToken() {
        var email = "email@email.email";
        var password = "password";

        when(tokenService.accessToken(email, password)).thenReturn(keycloakUserToken());

        var actual = userService.accessToken(email, password);

        StepVerifier.create(actual).expectNext(userToken()).verifyComplete();
    }

    @Test
    void accessTokenShouldReturnError() {
        var email = "email@email.email";
        var password = "password";

        when(tokenService.accessToken(email, password)).thenReturn(accessDenied());

        var actual = userService.accessToken(email, password);

        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @Test
    void shouldRefreshToken() {
        var refreshToken = "refresh token value";
        when(tokenService.refreshToken(refreshToken)).thenReturn(keycloakUserToken());

        var actual = userService.refreshToken(refreshToken);

        StepVerifier.create(actual).expectNext(userToken()).verifyComplete();
    }

    @Test
    void refreshTokenShouldReturnError() {
        var refreshToken = "refresh token value";
        when(tokenService.refreshToken(refreshToken)).thenReturn(accessDenied());

        var actual = userService.refreshToken(refreshToken);

        StepVerifier.create(actual).expectError(WebClientResponseException.class).verify();
    }

    @WithMockUser(authorities = "person_service_wr")
    @Test
    void shouldUpdateUser_WhenUserHasAdminRole() {
        var userRegistrationRequest = registrationRequest();
        var individualResponse = new IndividualResponse();

        when(personService.update(eq(individualId), any())).thenReturn(individualResponse);
        var actual = userService.updateUser(individualId, userRegistrationRequest);

        StepVerifier.create(actual).expectNext(individualResponse);
    }


    @Test
    void updateUser_ShouldThrowAuthenticationCredentialsNotFound() {
        var userRegistrationRequest = registrationRequest();

        assertThatThrownBy(() -> userService.updateUser(individualId, userRegistrationRequest).block())
                .isExactlyInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @WithIndividualIdUser("00000000-0000-0000-0000-000000000000")
    @Test
    void shouldUpdateUser_WhenUserHasSameIndividualId() {
        var userRegistrationRequest = registrationRequest();
        var individualResponse = new IndividualResponse();

        when(personService.update(eq(individualId), any())).thenReturn(individualResponse);
        var actual = userService.updateUser(individualId, userRegistrationRequest);

        StepVerifier.create(actual).expectNext(individualResponse);
    }

    @WithMockUser(authorities = "person_service_wr")
    @Test
    void shouldDeleteUser() {
        var keycloakResponse = new KeycloakUserInfoResponse("keycloakId", "user", 2342L, new HashMap<>());
        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken()));

        when(client.findByIndividualId(individualId, "adminToken")).thenReturn(Flux.just(keycloakResponse));
        when(client.deleteUser("keycloakId", "adminToken")).thenReturn(Mono.empty());

        userService.deleteUser(individualId).subscribe();
        verify(tokenService).adminToken();
        verify(client).findByIndividualId(any(), any());
        verify(personService, never()).compensateDeletion(any());
    }

    @WithMockUser(authorities = "person_service_wr")
    @Test
    void deleteUser_WhenKeycloakReturnException_ShouldCompensate() {
        var keycloakResponse = new KeycloakUserInfoResponse("keycloakId", "user", 2342L, new HashMap<>());
        when(tokenService.adminToken()).thenReturn(Mono.just(adminToken()));
        when(client.findByIndividualId(individualId, "adminToken")).thenReturn(Flux.just(keycloakResponse));
        when(client.deleteUser("keycloakId", "adminToken"))
                .thenReturn(Mono.error(new WebClientResponseException(500, "something went wrong", null, null, null)));

        assertThatThrownBy(() -> userService.deleteUser(individualId).block())
                .isExactlyInstanceOf(WebClientResponseException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.INTERNAL_SERVER_ERROR)
                .hasFieldOrPropertyWithValue("statusText", "something went wrong");

        verify(tokenService).adminToken();
        verify(client).findByIndividualId(any(), any());
        verify(personService).compensateDeletion(individualId);
    }

    @WithMockUser(authorities = "person_service_wr")
    @Test
    void shouldFindById() {
        var individualResponse = new IndividualResponse();

        when(personService.findById(individualId)).thenReturn(individualResponse);
        var actual = userService.findById(individualId);

        StepVerifier.create(actual).expectNext(individualResponse).expectComplete();
    }

    @WithMockUser(authorities = "person_service_wr")
    @Test
    void shouldFindByEmail() {
        var email = "example@sample.ru";
        var individualResponse = new IndividualResponse();
        when(personService.findByEmail(email)).thenReturn(individualResponse);

        var actual = userService.findByEmail(email);
        StepVerifier.create(actual).expectNext(individualResponse).expectComplete();
    }

    private String adminToken() {
        return "adminToken";
    }
}