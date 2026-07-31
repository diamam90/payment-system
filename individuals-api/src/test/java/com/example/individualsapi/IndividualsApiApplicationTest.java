package com.example.individualsapi;

import com.example.individualsapi.config.MockServiceConfig;
import com.example.individualsapi.config.SecurityTestConfig;
import com.example.individualsapi.config.AppPropertiesTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({SecurityTestConfig.class, MockServiceConfig.class, AppPropertiesTestConfig.class})
class IndividualsApiApplicationTest {

    @Test
    void contextLoad() {

    }
}