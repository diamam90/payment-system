package com.example.service;

import com.example.fake.dto.StatusUpdate;

public interface WebhookService {

    void updatePayout(StatusUpdate status);

    void updateTransaction(StatusUpdate status);
}
