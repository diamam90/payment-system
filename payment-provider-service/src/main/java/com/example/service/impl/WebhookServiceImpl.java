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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookRepository webhookRepository;
    private final TransactionService transactionService;
    private final PayoutService payoutService;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    @Override
    public void updatePayout(StatusUpdate request) {
        Payout payout = payoutService.findByIdPessimisticWrite(request.getId())
                .orElseThrow(() -> new BadRequestException(
                        String.format("Выплата с идентификатором [%s] не найдена", request.getId()))
                );
        Status requestStatus = Status.fromRequest(request.getStatus());

        if (updatePayoutStatus(payout, requestStatus)) {
            Webhook webhook = createWebhook(EventType.PAYOUT, request);
            webhookRepository.save(webhook);
        }
    }

    @Override
    public void updateTransaction(StatusUpdate request) {
        Transaction transaction = transactionService.findByIdPessimisticWrite(request.getId())
                .orElseThrow(() -> new BadRequestException(
                        String.format("Пополнение с идентификатором [%s] не найдено", request.getId()))
                );
        Status requestStatus = Status.fromRequest(request.getStatus());

        if (updateTransactionStatus(transaction, requestStatus)) {
            Webhook webhook = createWebhook(EventType.TRANSACTION, request);
            webhookRepository.save(webhook);
        }
    }

    private boolean updatePayoutStatus(Payout payout, Status requestStatus) {
        rwl.readLock().lock();
        Status payoutStatus = payout.getStatus();

        if (payout.isFinalStatus()) {
            if (payoutStatus.equals(requestStatus)) {
                log.info("Выплата уже находится в статусе {}", payoutStatus);
                rwl.readLock().unlock();
                return false;
            }
            rwl.readLock().unlock();
            throw new BadRequestException(
                    String.format("Не подходящий статус %s для выплаты с ID: %s ", requestStatus, payout.getId())
            );
        }

        try {
            rwl.readLock().unlock();
            rwl.writeLock().lock();

            if (payout.isFinalStatus()) {
                if (payout.getStatus().equals(requestStatus)) {
                    log.info("Выплата уже находится в статусе {}", payout.getStatus());
                    return false;
                }

                throw new BadRequestException(
                        String.format("Не подходящий статус %s для выплаты с ID: %s ", requestStatus, payout.getId())
                );
            } else {
                payout.setStatus(requestStatus);
                return true;
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private boolean updateTransactionStatus(Transaction transaction, Status requestStatus) {
        rwl.readLock().lock();
        Status transactionStatus = transaction.getStatus();

        if (transaction.isFinalStatus()) {
            rwl.readLock().unlock();
            if (transactionStatus.equals(requestStatus)) {
                log.info("Пополнение уже находится в статусе {}", transactionStatus);
                return false;
            }

            throw new BadRequestException(
                    String.format("Не подходящий статус %s для пополнения с ID: %s ", requestStatus, transaction.getId())
            );
        }
        try {
            rwl.readLock().unlock();
            rwl.writeLock().lock();

            if (transaction.isFinalStatus()) {
                if (transaction.getStatus().equals(requestStatus)) {
                    log.info("Пополнение уже находится в статусе {}", transaction.getStatus());
                    return false;
                }

                throw new BadRequestException(
                        String.format("Не подходящий статус %s для пополнения с ID: %s ", requestStatus, transaction.getId())
                );
            } else {
                transaction.setStatus(requestStatus);
                return true;
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private Webhook createWebhook(EventType type, StatusUpdate request) {
        Webhook webhook = new Webhook();
        webhook.setEventType(type);
        webhook.setEntityId(request.getId());
        webhook.setPayload(getPayload(request));
        webhook.setReceivedAt(LocalDateTime.now());
        return webhook;
    }

    private String getPayload(StatusUpdate status) {
        return objectMapper.writeValueAsString(status);
    }
}