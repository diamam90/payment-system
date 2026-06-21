package com.example.currencyrateservice.service.impl;

import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.exception.ObjectNotFoundException;
import com.example.currencyrateservice.model.CurrencyRateProjection;
import com.example.currencyrateservice.repository.CurrencyRateRepository;
import com.example.currencyrateservice.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final CurrencyRateRepository currencyRateRepository;

    @Override
    public void saveCurrencyRates(List<CurrencyRate> currencyRate) {
        currencyRateRepository.saveAll(currencyRate);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyRateProjection getByCurrencyPairAndTimestamp(String sourceCode, String targetCode, ZonedDateTime dateTime) {
        List<CurrencyRateProjection> currencyRates = currencyRateRepository
                .findBySourceAndDestinationCodeAndTimestamp(sourceCode, targetCode, dateTime.toInstant());

        return currencyRates.stream()
                .min(Comparator.comparing(CurrencyRateProjection::getProviderPriority))
                .orElseThrow(() -> new ObjectNotFoundException(
                        String.format(
                                "%s not found by attribute sourceCode: %s, targetCode: %s, dateTime: %s",
                                CurrencyRate.class.getSimpleName(),
                                sourceCode,
                                targetCode,
                                dateTime
                        )
                ));
    }
}
