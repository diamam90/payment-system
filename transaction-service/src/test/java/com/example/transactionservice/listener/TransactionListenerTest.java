package com.example.transactionservice.listener;

import com.example.transactionservice.config.KafkaTestConfig;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.service.TransactionService;
import com.example.transactionservice.stub.TransactionCompletedEventStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;

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
        TransactionCompletedEvent event = TransactionCompletedEventStub.completedEvent(clock);
        kafka.send("transaction.complete", event.transactionId().toString(), event);
        verify(transactionService, timeout(300)).complete(event);
    }

    @Test
    void shouldExecuteFailMethod() {
        TransactionCompletedEvent event = TransactionCompletedEventStub.failedEvent(clock);
        kafka.send("transaction.complete", event.transactionId().toString(), event);
        verify(transactionService, timeout(300)).fail(event);
    }
}