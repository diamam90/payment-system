package com.example.personservice;

import com.example.personservice.config.DatabaseConfig;
import com.example.personservice.config.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Import({DatabaseConfig.class, SecurityTestConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class PersonServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}