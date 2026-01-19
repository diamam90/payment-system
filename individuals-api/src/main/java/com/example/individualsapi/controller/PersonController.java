package com.example.individualsapi.controller;

import com.example.individuals.api.PersonServiceApi;
import com.example.individuals.dto.TokenResponse;
import com.example.individuals.dto.UserRequest;
import com.example.individuals.dto.UserResponse;
import com.example.individualsapi.mapper.PersonMapper;
import com.example.individualsapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PersonController implements PersonServiceApi {

    private final UserService userService;
    private final PersonMapper personMapper;

    @Override

    public Mono<ResponseEntity<UserResponse>> updateIndividual(UUID individualId, Mono<UserRequest> userRequest, ServerWebExchange exchange) {
        return userRequest.flatMap(request -> userService.updateUser(individualId, request))
                .map(personMapper::toUserResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserResponse>> getIndividualById(UUID individualId, ServerWebExchange exchange) {
        return userService.findById(individualId)
                .map(personMapper::toUserResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteIndividual(UUID individualId, ServerWebExchange exchange) {
        return userService.deleteUser(individualId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<UserResponse>> findIndividualByEmail(String email, ServerWebExchange exchange) {
        return userService.findByEmail(email)
                .map(personMapper::toUserResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> createIndividual(Mono<UserRequest> userRequest, ServerWebExchange exchange) {
        return userRequest.flatMap(userService::register)
                .map(tokenResponse -> ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse));
    }
}
