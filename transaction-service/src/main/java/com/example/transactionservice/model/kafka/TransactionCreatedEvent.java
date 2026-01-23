package com.example.transactionservice.model.kafka;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
public record TransactionCreatedEvent(
        UUID transactionId,
        UUID userId,
        UUID walletId,
        BigDecimal amount,
        String currency,
        String destination,
        ZonedDateTime timestamp,
        String type
) {
}
