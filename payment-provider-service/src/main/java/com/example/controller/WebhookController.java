package com.example.controller;

import com.example.fake.api.WebhookApi;
import com.example.fake.dto.StatusUpdate;
import com.example.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController implements WebhookApi {

    private final WebhookService webhookService;

    @Override
    public ResponseEntity<Void> updatePayout(StatusUpdate request) {
        webhookService.updatePayout(request);
        log.info("Выплата с идентификатором [{}] обновила статус: {}", request.getId(), request.getStatus());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> updateTransaction(StatusUpdate request) {
        webhookService.updateTransaction(request);
        log.info("Пополнение с идентификатором [{}] обновило статус: {}", request.getId(), request.getStatus());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
