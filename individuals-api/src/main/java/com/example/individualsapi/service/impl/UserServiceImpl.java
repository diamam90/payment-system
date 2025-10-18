package com.example.individualsapi.service.impl;

import com.example.individuals.dto.TokenResponse;
import com.example.individuals.dto.UserInfoResponse;
import com.example.individuals.dto.UserRequest;
import com.example.individualsapi.annotation.MetricNames;
import com.example.individualsapi.annotation.RequestCounter;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.dto.keycloak.KeycloakUserInfoResponse;
import com.example.individualsapi.exception.BadRequestException;
import com.example.individualsapi.mapper.KeycloakMapper;
import com.example.individualsapi.mapper.PersonMapper;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import com.example.person.dto.IndividualResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.example.individualsapi.dto.keycloak.KeycloakUserRegistrationRequest.withPasswordType;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final TokenService tokenService;
    private final KeycloakClient keycloakClient;
    private final KeycloakMapper keycloakMapper;
    private final PersonMapper personMapper;

    private final PersonService personService;

    @RequestCounter(metric = MetricNames.HTTP_REGISTRATION)
    @Override
    public Mono<TokenResponse> register(UserRequest request) {
        return validateRequest(request)
                .map(personMapper::individualRequest)
                .map(personService::create)
                .map(IndividualResponse::getId)
                .flatMap(
                        id ->
                                registrationClient(request, id)
                                        .onErrorMap(ex -> {
                                            log.error("Exception {} has been thrown while register user in keycloak", ex.getMessage());
                                            personService.compensateCreation(id);
                                            return ex;
                                        }).doOnNext(_ -> log.info("Registered user with individual id {}  in keycloak", id))
                )
                .then(accessToken(request.getEmail(), request.getPassword()));
    }

    @Override
    public Mono<UserInfoResponse> currentUser(String keycloakUserId) {
        return tokenService.adminToken()
                .flatMap(token -> keycloakClient.userInfo(keycloakUserId, token))
                .map(keycloakMapper::userInfoResponse)
                .doOnSuccess(info -> log.debug("Received user info {}", info));
    }

    @Override
    public Mono<IndividualResponse> updateUser(UUID id, UserRequest request) {
        var individualRequest = personMapper.individualRequest(request);
        return Mono.just(personService.update(id, individualRequest));

    }

    @Override
    public Mono<Void> deleteUser(UUID individualId) {
        return Mono.justOrEmpty(personService.deleteById(individualId))
                .then(tokenService.adminToken()
                        .flatMap(token -> keycloakClient.findByIndividualId(individualId, token)
                                .next()
                                .map(KeycloakUserInfoResponse::id)
                                .flatMap(id -> keycloakClient.deleteUser(id, token))
                                .onErrorMap(ex -> {
                                    log.error("Exception {} has been thrown while deletion user in keycloak", ex.getMessage());
                                    personService.compensateDeletion(individualId);
                                    return ex;
                                })
                        )
                );
    }

    @Override
    public Mono<IndividualResponse> findByEmail(String email) {
        return Mono.just(personService.findByEmail(email));
    }

    @Override
    public Mono<IndividualResponse> findById(UUID id) {
        return Mono.just(personService.findById(id));
    }

    @RequestCounter(metric = MetricNames.HTTP_LOGIN)
    @Override
    public Mono<TokenResponse> accessToken(String email, String password) {
        return tokenService.accessToken(email, password)
                .map(keycloakMapper::tokenResponse);
    }

    @Override
    public Mono<TokenResponse> refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken)
                .map(keycloakMapper::tokenResponse);
    }

    private Mono<Void> registrationClient(UserRequest request, UUID individualId) {
        var keycloakRequest = withPasswordType(request.getEmail(), request.getPassword(), individualId);
        return tokenService.adminToken()
                .flatMap(token -> keycloakClient.registration(keycloakRequest, token));
    }

    private Mono<UserRequest> validateRequest(UserRequest request) {
        return Mono.just(request).map(req -> {
            if (!req.getPassword().equals(req.getConfirmPassword())) {
                throw new BadRequestException("Password and confirm password is not equal");
            } else return req;
        });
    }
}
