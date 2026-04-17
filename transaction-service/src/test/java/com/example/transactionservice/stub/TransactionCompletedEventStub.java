package com.example.transactionservice.stub;

import com.example.transactionservice.model.kafka.TransactionCompletedEvent;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

public class TransactionCompletedEventStub {

    public static TransactionCompletedEvent completedEvent(Clock clock) {
        return new TransactionCompletedEvent(
                UUID.randomUUID(),
                "COMPLETED",
                null,
                BigDecimal.ONE,
                ZonedDateTime.now(clock)
        );
    }

    public static TransactionCompletedEvent failedEvent(Clock clock) {
        return new TransactionCompletedEvent(
                UUID.randomUUID(),
                "FAILED",
                null,
                BigDecimal.ONE,
                ZonedDateTime.now(clock)
        );
    }
}
