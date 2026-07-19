package com.example.currencyrateservice.repository;

import com.example.currencyrateservice.entity.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProviderRepositoryTest {

    @Autowired
    ProviderRepository repository;

    @Test
    @Sql(value = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void findByProviderCodeIn_shouldReturnEmptyList() {
        List<Provider> result = repository.findByProviderCodeIn(List.of("cur", "tes"));
        assertThat(result).isEmpty();
    }

    @Sql(value = {"/sql/clean.sql", "/sql/find-provider-code-in.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    void findByProviderCodeIn_WhenThreeProvidersExist_shouldReturnTwoProvider() {
        List<Provider> result = repository.findByProviderCodeIn(List.of("cur", "tes", "css"));
        assertThat(result)
                .hasSize(2)
                .extracting("providerName").contains("test provider", "test provider 2");
    }
}