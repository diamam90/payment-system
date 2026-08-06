package com.example.service.impl;

import com.example.fake.dto.StatusUpdate;
import com.example.repository.WebhookRepository;
import com.example.service.PayoutService;
import com.example.service.TransactionService;
import com.example.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookRepository webhookRepository;
    private final PayoutService payoutService;
    private final TransactionService transactionService;

    @Override
    public void updatePayout(StatusUpdate status) {
//        payoutService.update(status);
    }

    @Override
    public void updateTransaction(StatusUpdate status) {
//        transactionService.update(status);
    }
}
