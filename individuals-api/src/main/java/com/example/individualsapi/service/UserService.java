package com.example.individualsapi.service;

import com.example.individuals.dto.TokenResponse;
import com.example.individuals.dto.UserInfoResponse;
import com.example.individuals.dto.UserRequest;
import com.example.person.dto.IndividualResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {

    Mono<TokenResponse> register(UserRequest request);

    Mono<UserInfoResponse> currentUser(String keycloakUserId);

    Mono<TokenResponse> accessToken(String email, String password);

    Mono<TokenResponse> refreshToken(String refreshToken);

    Mono<IndividualResponse> updateUser(UUID individualId,UserRequest request);

    Mono<Void> deleteUser(UUID individualId);

    Mono<IndividualResponse> findById(UUID individualId);

    Mono<IndividualResponse> findByEmail(String email);
}
