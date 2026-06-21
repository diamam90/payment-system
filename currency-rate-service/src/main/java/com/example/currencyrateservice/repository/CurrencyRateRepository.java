package com.example.currencyrateservice.repository;

import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.model.CurrencyRateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    @Query("""
                    SELECT currencyRate.id as id,
                           currencyRate.sourceCode as sourceCode,
                           currencyRate.destinationCode as destinationCode,
                           currencyRate.rateBeginTime as rateBeginTime,
                           currencyRate.rateEndTime as rateEndTime,
                           currencyRate.rate as rate,
                           provider.providerCode as providerCode,
                           provider.priority as providerPriority
                    FROM CurrencyRate currencyRate
                    JOIN currencyRate.provider provider
                    WHERE currencyRate.sourceCode = :source
                        AND currencyRate.destinationCode = :destination
                        AND :timestamp BETWEEN currencyRate.rateBeginTime AND currencyRate.rateEndTime
                    ORDER by provider.priority
            """)
    List<CurrencyRateProjection> findBySourceAndDestinationCodeAndTimestamp(String source, String destination, Instant timestamp);
}
