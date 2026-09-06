package com.example.service.impl;

import com.example.entity.*;
import com.example.exception.BadRequestException;
import com.example.fake.dto.StatusUpdate;
import com.example.repository.WebhookRepository;
import com.example.service.PayoutService;
import com.example.service.TransactionService;
import com.example.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookRepository webhookRepository;
    private final TransactionService transactionService;
    private final PayoutService payoutService;
    private final ObjectMapper objectMapper;

    @Override
    public void updatePayout(StatusUpdate request) {
        validateRequest(request);

        Payout payout = payoutService.findById(request.getId())
                .orElseThrow(() -> new BadRequestException(
                        String.format("Выплата с идентификатором [%s] не найдена", request.getId()))
                );

        Status payoutStatus = payout.getStatus();
        Status requestStatus = Status.valueOf(request.getStatus());

        if (payout.isFinalStatus()) {
            if (payoutStatus.equals(requestStatus)) return;

            throw new BadRequestException(
                    String.format("Не подходящий статус %s для выплаты с ID: %s ", requestStatus, request.getId())
            );
        }

        payout.setStatus(requestStatus);

        Webhook webhook = new Webhook();
        webhook.setEntityId(request.getId());
        webhook.setEventType(EventType.PAYOUT);
        webhook.setReceivedAt(LocalDateTime.now());
        webhook.setPayload(getPayload(request));
        webhookRepository.save(webhook);

        log.info("Выплата с идентификатором [{}] обновила статус: {}", request.getId(), requestStatus);
    }

    @Override
    public void updateTransaction(StatusUpdate request) {
        validateRequest(request);

        Transaction transaction = transactionService.findById(request.getId())
                .orElseThrow(() -> new BadRequestException(
                        String.format("Пополнение с идентификатором [%s] не найдено", request.getId()))
                );

        Status payoutStatus = transaction.getStatus();
        Status requestStatus = Status.valueOf(request.getStatus());

        if (transaction.isFinalStatus()) {
            if (payoutStatus.equals(requestStatus)) return;

            throw new BadRequestException(
                    String.format("Не подходящий статус %s для пополнения с ID: %s ", requestStatus, request.getId())
            );
        }

        transaction.setStatus(requestStatus);

        Webhook webhook = new Webhook();
        webhook.setEventType(EventType.TRANSACTION);
        webhook.setEntityId(request.getId());
        webhook.setPayload(getPayload(request));
        webhook.setReceivedAt(LocalDateTime.now());
        webhookRepository.save(webhook);
        log.info("Пополнение с идентификатором [{}] обновило статус: {}", request.getId(), requestStatus);
    }

    private String getPayload(StatusUpdate status) {
        return objectMapper.writeValueAsString(status);
    }

    private void validateRequest(StatusUpdate request) {
        if (request.getId() < 0) {
            throw new BadRequestException("Id must be positive");
        }
        try {
            Status.valueOf(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Incorrect status: " + request.getStatus());
        }
    }
}
