package com.example.service.impl;

import com.example.client.KeycloakClient;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.exception.BadRequestException;
import com.example.mapper.KeycloakMapper;
import com.example.service.TokenService;
import com.example.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.example.dto.keycloak.KeycloakUserRegistrationRequest.withPasswordType;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final TokenService tokenService;
    private final KeycloakClient client;
    private final KeycloakMapper mapper;

    @Override
    public Mono<TokenResponse> register(UserRegistrationRequest request) {
        return validateRequest(request)
                .then(getAdminAccessToken())
                .flatMap(token -> registrationClient(token, request))
                .then(getTokenResponse(request))
                .log("user service registration");
    }

    @Override
    public Mono<UserInfoResponse> currentUser(String userId) {
        return getAdminAccessToken()
                .flatMap(token -> client.userInfo(userId, token).map(mapper::userInfoResponse))
                .log("user service get current user");
    }

    private Mono<String> getAdminAccessToken() {
        return tokenService.adminToken().map(KeycloakTokenResponse::accessToken);
    }

    private Mono<Void> registrationClient(String adminToken, UserRegistrationRequest request) {
        var keycloakRequest = withPasswordType(request.getEmail(), request.getPassword());
        return client.registration(keycloakRequest, adminToken);
    }

    private Mono<TokenResponse> getTokenResponse(UserRegistrationRequest request) {
        return tokenService.accessToken(request.getEmail(), request.getPassword())
                .map(mapper::tokenResponse);
    }

    private Mono<Void> validateRequest(UserRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new BadRequestException("Password and confirm password is not equal"));
        }
        return Mono.empty();
    }
}
