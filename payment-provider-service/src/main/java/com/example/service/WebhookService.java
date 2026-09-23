package com.example.service;

import com.example.fake.dto.StatusUpdate;

public interface WebhookService {

    void updatePayout(StatusUpdate request);

    void updateTransaction(StatusUpdate request);
}
