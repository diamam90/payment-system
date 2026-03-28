package com.example.transactionservice.model;

import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.entity.TransactionType;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
public record TransactionFilter(
        UUID userUid,
        UUID walletUid,
        TransactionType type,
        TransactionStatus status,
        ZonedDateTime dateFrom,
        ZonedDateTime dateTo
) {
}
