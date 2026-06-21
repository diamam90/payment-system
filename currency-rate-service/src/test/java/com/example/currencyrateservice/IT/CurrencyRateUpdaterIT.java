package com.example.currencyrateservice.IT;

import com.example.currencyrateservice.config.TestSecurityConfig;
import com.example.currencyrateservice.config.WireMockConfig;
import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.job.CurrencyRateUpdater;
import com.example.currencyrateservice.service.CurrencyRateService;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import({WireMockConfig.class, TestSecurityConfig.class})
class CurrencyRateUpdaterIT {

    @MockitoSpyBean
    CurrencyRateUpdater currencyRateUpdater;
    @MockitoSpyBean
    CurrencyRateService currencyRateService;
    @Captor
    ArgumentCaptor<List<CurrencyRate>> currencyRateCaptor;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void shouldExecuteJobAndSave4CurrencyRates() {
        verify(currencyRateUpdater, timeout(3000)).doJob();
        verify(currencyRateService, timeout(3000)).saveCurrencyRates(currencyRateCaptor.capture());
        Map<String, Object> shedlockParams = jdbc.queryForMap("SELECT * FROM shedlock");
        assertThat(shedlockParams)
                .containsEntry("name", "currency_rate_updater")
                .containsKey("locked_by")
                .containsKey("locked_at")
                .containsKey("lock_until");
        assertThat(currencyRateCaptor.getValue())
                .hasSize(4)
                .extracting("sourceCode", "destinationCode", "rate", "providerCode")
                .containsAll(expectedResults());
    }

    private List<Tuple> expectedResults() {
        return List.of(
                new Tuple("RUB", "USD", BigDecimal.valueOf(30), "tes"),
                new Tuple("USD", "RUB", BigDecimal.valueOf(0.033), "tes"),
                new Tuple("USD", "EUR", BigDecimal.valueOf(1), "tes"),
                new Tuple("EUR", "USD", BigDecimal.valueOf(1), "tes")
        );
    }

    @DynamicPropertySource
    public static void props(DynamicPropertyRegistry registry) {
        registry.add("CURRENCY-RATE-SERVICE_UPDATE-RATE-CRON", () -> "*/2 * * * * *");
    }
}