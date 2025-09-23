package com.example.service.impl;

import com.example.client.KeycloakClient;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.exception.BadRequestException;
import com.example.mapper.KeycloakMapper;
import com.example.service.TokenService;
import com.example.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.example.dto.keycloak.KeycloakUserRegistrationRequest.withPasswordType;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final TokenService tokenService;
    private final KeycloakClient client;
    private final KeycloakMapper mapper;

    private static final String LOG_PREFIX = UserServiceImpl.class.getSimpleName();

    @Override
    public Mono<TokenResponse> register(UserRegistrationRequest request) {
        return validateRequest(request)
                .then(registrationClient(request))
                .then(accessToken(request.getEmail(), request.getPassword()))
                .log(LOG_PREFIX + ":registration");
    }

    @Override
    public Mono<UserInfoResponse> currentUser(String userId) {
        return client.userInfo(userId)
                .map(mapper::userInfoResponse)
                .log(LOG_PREFIX + ":current user");
    }

    @Override
    public Mono<TokenResponse> accessToken(String email, String password) {
        return tokenService.accessToken(email, password)
                .map(mapper::tokenResponse);
    }

    @Override
    public Mono<TokenResponse> refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken)
                .map(mapper::tokenResponse);
    }

    private Mono<Void> registrationClient(UserRegistrationRequest request) {
        var keycloakRequest = withPasswordType(request.getEmail(), request.getPassword());
        return client.registration(keycloakRequest);
    }


    private Mono<UserRegistrationRequest> validateRequest(UserRegistrationRequest request) {
        return Mono.just(request).map(req -> {
            if (!req.getPassword().equals(req.getConfirmPassword())) {
                throw new BadRequestException("Password and confirm password is not equal");
            } else return req;
        });
    }
}
