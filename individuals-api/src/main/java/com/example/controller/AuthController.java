package com.example.controller;

import com.example.dto.*;
import com.example.mapper.KeycloakMapper;
import com.example.service.TokenService;
import com.example.service.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;
    private final UserService userService;
    private final MeterRegistry registry;
    private final KeycloakMapper keycloakMapper;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/registration")
    public Mono<TokenResponse> registration(@RequestBody UserRegistrationRequest request) {
        return userService.register(request)
                .doOnSuccess((_) -> registration(true))
                .doOnError(_ -> registration(false));
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@RequestBody UserLoginRequest request) {
        return tokenService.accessToken(request.getEmail(), request.getPassword())
                .map(keycloakMapper::tokenResponse)
                .doOnSuccess((_) -> login(true))
                .doOnError(_ -> login(false));
    }

    @PostMapping("/refresh-token")
    public Mono<TokenResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        return tokenService.refreshToken(request.getRefreshToken())
                .map(keycloakMapper::tokenResponse);
    }

    @GetMapping("/me")
    public Mono<UserInfoResponse> me(Authentication authentication) {
        var jwt = (Jwt) authentication.getPrincipal();
        var userId = jwt.getSubject();
        return userService.currentUser(userId);
    }


    private void registration(boolean success) {
        Counter counter = registry.counter("auth_registration_total", "status", success ? "success" : "fail");
        counter.increment();
    }

    private void login(boolean success) {
        Counter counter = registry.counter("auth_login_total", "status", success ? "success" : "fail");
        counter.increment();
    }
}
