package com.example.controller;

import com.example.dto.*;
import com.example.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/registration")
    public Mono<TokenResponse> registration(@Valid @RequestBody UserRegistrationRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@RequestBody UserLoginRequest request) {
        return userService.accessToken(request.getEmail(), request.getPassword());
    }

    @PostMapping("/refresh-token")
    public Mono<TokenResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        return userService.refreshToken(request.getRefreshToken());
    }

    @GetMapping("/me")
    public Mono<UserInfoResponse> me(JwtAuthenticationToken jwtAuthenticationToken) {
        return userService.currentUser(jwtAuthenticationToken.getToken().getSubject());
    }
}
