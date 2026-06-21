package com.example.currencyrateservice.service;

import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.exception.ObjectNotFoundException;
import com.example.currencyrateservice.model.CurrencyRateProjection;
import com.example.currencyrateservice.repository.CurrencyRateRepository;
import com.example.currencyrateservice.service.impl.CurrencyRateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceImplTest {

    @Mock
    CurrencyRateRepository repo;

    @InjectMocks
    CurrencyRateServiceImpl service;

    @Test
    void shouldSaveCurrencyRates() {
        CurrencyRate currencyRate1 = new CurrencyRate();
        CurrencyRate currencyRate2 = new CurrencyRate();

        List<CurrencyRate> currencyRateList = List.of(currencyRate1, currencyRate2);
        service.saveCurrencyRates(currencyRateList);
        verify(repo).saveAll(currencyRateList);
    }

    @Test
    void getByCurrencyPairAndTimestamp_When3CurrencyRatesFound_shouldReturnOneCurrencyRateOrderByProviderPriority() {
        // given
        String from = "USD";
        String to = "RUB";
        ZonedDateTime timestamp = ZonedDateTime.parse("2025-05-05T12:00:00+00:00");
        List<CurrencyRateProjection> projections = projections();

        // when
        when(repo.findBySourceAndDestinationCodeAndTimestamp(from, to, Instant.parse("2025-05-05T12:00:00+00:00")))
                .thenReturn(projections);
        CurrencyRateProjection result = service.getByCurrencyPairAndTimestamp(from, to, timestamp);
        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("id", 1L)
                .hasFieldOrPropertyWithValue("providerCode", "aaa")
                .hasFieldOrPropertyWithValue("providerPriority", 1);
    }

    @Test
    void getByCurrencyPairAndTimestamp_WhenRepoReturnsEmptyList_shouldThrowEx() {
        // given
        String from = "USD";
        String to = "RUB";
        ZonedDateTime timestamp = ZonedDateTime.parse("2025-05-05T12:00:00+00:00");
        // when
        when(repo.findBySourceAndDestinationCodeAndTimestamp(from, to, Instant.parse("2025-05-05T12:00:00+00:00")))
                .thenReturn(Collections.emptyList());
        // then
        assertThatThrownBy(() -> service.getByCurrencyPairAndTimestamp(from, to, timestamp))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("CurrencyRate not found by attribute sourceCode: USD, targetCode: RUB, dateTime: 2025-05-05T12:00Z");
    }

    private List<CurrencyRateProjection> projections() {
        CurrencyRateProjection projection1 = new CurrencyRateProjection() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getSourceCode() {
                return "USD";
            }

            @Override
            public String getDestinationCode() {
                return "RUB";
            }

            @Override
            public Instant getRateBeginTime() {
                return Instant.parse("2025-05-05T11:00:00+00:00");
            }

            @Override
            public Instant getRateEndTime() {
                return Instant.parse("2025-05-05T13:00:00+00:00");

            }

            @Override
            public BigDecimal getRate() {
                return BigDecimal.ONE;
            }

            @Override
            public String getProviderCode() {
                return "aaa";
            }

            @Override
            public Integer getProviderPriority() {
                return 1;
            }
        };

        CurrencyRateProjection projection2 = new CurrencyRateProjection() {
            @Override
            public Long getId() {
                return 2L;
            }

            @Override
            public String getSourceCode() {
                return "USD";
            }

            @Override
            public String getDestinationCode() {
                return "RUB";
            }

            @Override
            public Instant getRateBeginTime() {
                return Instant.parse("2025-05-05T11:00:00+00:00");
            }

            @Override
            public Instant getRateEndTime() {
                return Instant.parse("2025-05-05T13:00:00+00:00");

            }

            @Override
            public BigDecimal getRate() {
                return BigDecimal.ONE;
            }

            @Override
            public String getProviderCode() {
                return "bbb";
            }

            @Override
            public Integer getProviderPriority() {
                return 5;
            }
        };

        CurrencyRateProjection projection3 = new CurrencyRateProjection() {
            @Override
            public Long getId() {
                return 3L;
            }

            @Override
            public String getSourceCode() {
                return "USD";
            }

            @Override
            public String getDestinationCode() {
                return "RUB";
            }

            @Override
            public Instant getRateBeginTime() {
                return Instant.parse("2025-05-05T11:00:00+00:00");
            }

            @Override
            public Instant getRateEndTime() {
                return Instant.parse("2025-05-05T13:00:00+00:00");

            }

            @Override
            public BigDecimal getRate() {
                return BigDecimal.ONE;
            }

            @Override
            public String getProviderCode() {
                return "aaa";
            }

            @Override
            public Integer getProviderPriority() {
                return 10;
            }
        };

        return List.of(projection1, projection2, projection3);
    }
}