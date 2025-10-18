package com.example.individualsapi.controller;

import com.example.individuals.api.AuthApi;
import com.example.individuals.dto.*;
import com.example.individualsapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;

    @Override
    public Mono<ResponseEntity<TokenResponse>> login(Mono<UserLoginRequest> userLoginRequest, ServerWebExchange exchange) {
        return userLoginRequest
                .flatMap(request -> userService.accessToken(request.getEmail(), request.getPassword()))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserInfoResponse>> me(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken)
                .map(token -> (JwtAuthenticationToken) token)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(token -> userService.currentUser(token.getSubject()))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> refreshToken(Mono<TokenRefreshRequest> tokenRefreshRequest, ServerWebExchange exchange) {
        return tokenRefreshRequest.map(TokenRefreshRequest::getRefreshToken)
                .flatMap(userService::refreshToken)
                .map(ResponseEntity::ok);
    }
}
