package com.example.controller;

import com.example.fake.api.WebhookApi;
import com.example.fake.dto.StatusUpdate;
import com.example.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WebhookController implements WebhookApi {

    private final WebhookService webhookService;

    @Override
    public ResponseEntity<Void> updatePayout(StatusUpdate statusUpdate) {
        webhookService.updatePayout(statusUpdate);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> updateTransaction(StatusUpdate statusUpdate) {
        webhookService.updateTransaction(statusUpdate);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
