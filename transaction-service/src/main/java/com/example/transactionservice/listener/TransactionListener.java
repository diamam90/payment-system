package com.example.transactionservice.listener;

import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionListener {

    private final TransactionService transactionService;

    @KafkaListener(topics = "${transaction-service.kafka.topic.in}")
    public void on(TransactionCompletedEvent event) {
        log.debug("Received event {} from kafka", event);
        if (TransactionStatus.COMPLETED.name().equals(event.status())) {
            transactionService.complete(event);
            return;
        }

        if (TransactionStatus.FAILED.name().equals(event.status())) {
            transactionService.fail(event);
            return;
        }
        log.info("Event {} has incompatible status", event);
    }
}
