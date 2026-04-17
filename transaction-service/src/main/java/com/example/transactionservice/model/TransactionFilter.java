package com.example.transactionservice.model;

import com.example.transaction.dto.Params;
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

    public static TransactionFilter fromRequest(Params params) {
        TransactionType type = params.getType() != null ? TransactionType.valueOf(params.getType().name()) : null;
        TransactionStatus status = params.getStatus() != null ? TransactionStatus.valueOf(params.getStatus().name()) : null;
        return new TransactionFilter(
                params.getUserUid(),
                params.getWalletUid(),
                type,
                status,
                params.getDateFrom(),
                params.getDateTo());
    }
}
