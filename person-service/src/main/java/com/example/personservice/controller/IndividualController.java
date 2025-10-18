package com.example.personservice.controller;

import com.example.personservice.mapper.IndividualMapper;
import com.example.person.api.IndividualsApi;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import com.example.personservice.service.IndividualService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class IndividualController implements IndividualsApi {

    private final IndividualService individualService;
    private final IndividualMapper individualMapper;

    @Override
    public ResponseEntity<IndividualResponse> findBy(String email) {
        var result = individualService.findByEmail(email);
        var dto = individualMapper.toDto(result);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @Override
    public ResponseEntity<IndividualResponse> findById(UUID id) {
        var result = individualService.findById(id);
        var dto = individualMapper.toDto(result);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @Override
    public ResponseEntity<IndividualResponse> update(UUID id, IndividualRequest individualUpdatingRequest) {
        var result = individualService.update(id, individualUpdatingRequest);
        var dto = individualMapper.toDto(result);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @Override
    public ResponseEntity<IndividualResponse> create(IndividualRequest individualCreatingRequest) {
        var result = individualService.create(individualCreatingRequest);
        var dto = individualMapper.toDto(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<Void> deleteById(UUID id) {
        individualService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
