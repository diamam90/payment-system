package com.example.personservice;

import com.example.personservice.config.AppContainers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ImportTestcontainers(AppContainers.class)
@Testcontainers(disabledWithoutDocker = true)
class PersonServiceApplicationTest {

    @Test
    void contextLoads() {
    }

}