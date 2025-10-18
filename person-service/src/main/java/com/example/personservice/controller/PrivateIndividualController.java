package com.example.personservice.controller;

import com.example.person.api.PrivateApi;
import com.example.personservice.service.IndividualService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Hidden
@RestController
@RequestMapping
@RequiredArgsConstructor
public class PrivateIndividualController implements PrivateApi {

    private final IndividualService individualService;

    @Override
    public ResponseEntity<Void> hardDelete(UUID id) {
        individualService.hardDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<Void> activateUser(UUID id) {
        individualService.activateUser(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
