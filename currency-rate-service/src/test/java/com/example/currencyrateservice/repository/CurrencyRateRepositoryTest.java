package com.example.currencyrateservice.repository;

import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.entity.Provider;
import com.example.currencyrateservice.model.CurrencyRateProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CurrencyRateRepositoryTest {

    @Autowired
    JdbcOperations jdbc;
    @Autowired
    CurrencyRateRepository repository;
    @Autowired
    ProviderRepository providerRepository;

    @Test
    @Sql(value = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void findBySourceAndDestinationCodeAndTimestamp_ShouldReturnEmptyList() {
        List<CurrencyRateProjection> result = repository.findBySourceAndDestinationCodeAndTimestamp(
                "ABC",
                "DBC",
                Instant.parse("2025-05-05T12:12:12+00:00")
        );
        assertThat(result).isEmpty();
    }

    @Sql(value = {"/sql/clean.sql", "/sql/currency-rate-one-result.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    void findBySourceAndDestinationCodeAndTimestamp_ShouldFindOnlyOneResult() {
        List<CurrencyRateProjection> result = repository.findBySourceAndDestinationCodeAndTimestamp(
                "ABC",
                "BCD",
                Instant.parse("2026-05-05T12:05:00+00:00")
        );
        assertThat(result)
                .hasSize(1)
                .element(0)
                .hasFieldOrPropertyWithValue("sourceCode", "ABC")
                .hasFieldOrPropertyWithValue("destinationCode", "BCD")
                .hasFieldOrPropertyWithValue("rateBeginTime", Instant.parse("2026-05-05T12:00:00Z"))
                .hasFieldOrPropertyWithValue("rateEndTime", Instant.parse("2026-05-05T12:15:00Z"))
                .hasFieldOrPropertyWithValue("rate", BigDecimal.valueOf(0.0001))
                .hasFieldOrPropertyWithValue("providerCode", "asc")
                .hasFieldOrPropertyWithValue("providerPriority", 1);
    }

    @Sql(value = {"/sql/clean.sql","/sql/currency-rate-two-providers.sql" },executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    void findBySourceAndDestinationCodeAndTimestamp_ShouldFindTwoResultWithAnotherProvider() {
        List<CurrencyRateProjection> result = repository.findBySourceAndDestinationCodeAndTimestamp(
                "ABC",
                "BCD",
                Instant.parse("2026-05-05T12:05:00+00:00")
        );
        assertThat(result)
                .hasSize(2)
                .extracting("providerCode")
                .contains("asc", "ddd");
    }

    @Sql(value = {"/sql/clean.sql", "/sql/currency-rate-save-all.sql"},executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    void shouldSaveAll() {
        Provider provider = providerRepository.findByProviderCodeIn(List.of("asc")).stream().findAny().orElseThrow();

        CurrencyRate currencyRate1 = new CurrencyRate();
        currencyRate1.setProvider(provider);
        currencyRate1.setSourceCode("ABC");
        currencyRate1.setDestinationCode("BCD");
        currencyRate1.setRate(BigDecimal.valueOf(2.032));
        currencyRate1.setRateBeginTime(Instant.parse("2025-05-05T12:00:00+00:00"));
        currencyRate1.setRateEndTime(Instant.parse("2025-05-05T13:00:00+00:00"));

        CurrencyRate currencyRate2 = new CurrencyRate();
        currencyRate2.setProvider(provider);
        currencyRate2.setSourceCode("BCD");
        currencyRate2.setDestinationCode("ABC");
        currencyRate2.setRate(BigDecimal.valueOf(0.02));
        currencyRate2.setRateBeginTime(Instant.parse("2025-05-05T12:00:00+00:00"));
        currencyRate2.setRateEndTime(Instant.parse("2025-05-05T13:00:00+00:00"));

        repository.saveAllAndFlush(List.of(currencyRate1, currencyRate2));

        List<CurrencyRate> query = jdbc.query("SELECT * FROM currency.currency_rates", new CurrencyRateRowMapper());
        assertThat(query).hasSize(2);
    }

    private class CurrencyRateRowMapper implements RowMapper<CurrencyRate> {
        @Nullable
        @Override
        public CurrencyRate mapRow(ResultSet rs, int rowNum) throws SQLException {
            CurrencyRate currencyRate = new CurrencyRate();
            currencyRate.setId(rs.getLong("id"));
            currencyRate.setSourceCode(rs.getString("source_code"));
            currencyRate.setDestinationCode(rs.getString("destination_code"));
            currencyRate.setRateBeginTime(rs.getTimestamp("rate_begin").toInstant());
            currencyRate.setRateEndTime(rs.getTimestamp("rate_end").toInstant());
            currencyRate.setRate(rs.getBigDecimal("rate"));
            currencyRate.setProviderCode(rs.getString("provider_code"));
            currencyRate.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            currencyRate.setModifiedAt(rs.getTimestamp("modified_at").toInstant());
            return currencyRate;
        }
    }
}