package com.example.transactionservice.listener;

import com.example.transactionservice.config.KafkaTestConfig;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(KafkaTestConfig.class)
class TransactionListenerTest {

    @MockitoBean
    TransactionService transactionService;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @Autowired
    KafkaTemplate<String, Object> kafka;
    @Autowired
    Clock clock;

    @Test
    void shouldExecuteCompleteMethod() {
        TransactionCompletedEvent event = completedEvent();
        kafka.send("transaction.complete", event.transactionId().toString(), event);
        verify(transactionService, timeout(300)).complete(event);
    }

    @Test
    void shouldExecuteFailMethod() {
        TransactionCompletedEvent event = failedEvent();
        kafka.send("transaction.complete", event.transactionId().toString(), event);
        verify(transactionService, timeout(300)).fail(event);
    }

    private TransactionCompletedEvent completedEvent() {
        return new TransactionCompletedEvent(
                UUID.randomUUID(),
                "COMPLETED",
                null,
                BigDecimal.ONE,
                ZonedDateTime.now(clock)
        );
    }

    private TransactionCompletedEvent failedEvent() {
        return new TransactionCompletedEvent(
                UUID.randomUUID(),
                "FAILED",
                null,
                BigDecimal.ONE,
                ZonedDateTime.now(clock)
        );
    }
}