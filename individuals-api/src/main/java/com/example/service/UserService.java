package com.example.service;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<TokenResponse> register(UserRegistrationRequest request);

    Mono<UserInfoResponse> currentUser(String userId);

    Mono<TokenResponse> accessToken(String email,String password);

    Mono<TokenResponse> refreshToken(String refreshToken);
}
