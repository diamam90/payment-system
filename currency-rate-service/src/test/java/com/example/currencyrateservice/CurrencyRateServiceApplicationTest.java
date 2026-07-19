package com.example.currencyrateservice;

import com.example.currencyrateservice.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestSecurityConfig.class)
class CurrencyRateServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}