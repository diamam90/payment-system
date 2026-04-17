package com.example.transactionservice;

import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.config.KafkaTestConfig;
import com.example.transactionservice.config.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import({
        SecurityTestConfig.class,
        KafkaTestConfig.class
})
class TransactionServiceApplicationTest {

    @Test
    void contextLoads() {

    }
}