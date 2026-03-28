package com.example.transactionservice.model.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionCompletedEvent(
        UUID transactionId,
        String status,
        String failureReason,
        BigDecimal amount,
        ZonedDateTime timestamp
) {
}
