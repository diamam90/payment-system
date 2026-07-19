package com.example.currencyrateservice.job;

import com.example.currencyrateservice.client.ProviderClient;
import com.example.currencyrateservice.entity.CurrencyRate;
import com.example.currencyrateservice.entity.Provider;
import com.example.currencyrateservice.mapper.CurrencyRateMapper;
import com.example.currencyrateservice.model.CurrencyPair;
import com.example.currencyrateservice.model.ProviderMetadata;
import com.example.currencyrateservice.service.CurrencyRateService;
import com.example.currencyrateservice.service.CurrencyService;
import com.example.currencyrateservice.service.ProviderService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
public class CurrencyRateUpdater {

    private final CurrencyRateService currencyRateService;
    private final ProviderService providerService;
    private final CurrencyService currencyService;
    private final Map<String, ProviderClient> providerClientByCode;

    private final Clock clock;
    private final CurrencyRateMapper currencyRateMapper;

    public CurrencyRateUpdater(
            CurrencyRateService currencyRateService,
            ProviderService providerService,
            CurrencyService currencyService,
            List<ProviderClient> providerClients,
            CurrencyRateMapper currencyRateMapper,
            Clock clock
    ) {
        this.currencyRateService = currencyRateService;
        this.providerService = providerService;
        this.currencyRateMapper = currencyRateMapper;
        this.currencyService = currencyService;
        this.providerClientByCode = clientByCode(providerClients);
        this.clock = clock;
    }

    @Value("${CURRENCY_RATE_SERVICE_ACTIVE_DURATION:20m}")
    private Duration ACTIVE_DURATION;

    @Timed("update.rate.job")
    @SchedulerLock(name = "currency_rate_updater", lockAtMostFor = "PT10M")
    @Scheduled(cron = "${CURRENCY-RATE-SERVICE_UPDATE-RATE-CRON:-}")
    public void doJob() {
        LockAssert.assertLocked();
        StopWatch watch = new StopWatch();
        watch.start();
        log.info("Start updating currency job...");
        List<Provider> providers = providerService.getByProviderCodes(providerClientByCode.keySet());
        Map<String, Provider> providerByCode = providerByCode(providers);
        Map<ProviderClient, Provider> providerMap = saveNewProvidersAndMap(providerClientByCode, providerByCode);

        List<String> codes = currencyService.getAllCurrencyCodes();
        List<String> codePairs = getCurrencyPairs(codes);

        Instant beginTime = Instant.now(clock);
        Instant endTime = beginTime.plus(ACTIVE_DURATION);
        List<CurrencyRate> result = providerMap.entrySet()
                .stream()
                .map(entry ->
                        collectRateFromProvider(entry, codePairs, beginTime, endTime))
                .filter(currencyRates -> !currencyRates.isEmpty())
                .flatMap(List::stream)
                .toList();

        if (!result.isEmpty()) {
            currencyRateService.saveCurrencyRates(result);
            log.info("{} currency rates updated", result.size());
        }
        watch.stop();
        log.info("End updating currency job, duration: {} sec", watch.getTotalTimeSeconds());
    }

    private Map<String, ProviderClient> clientByCode(Collection<ProviderClient> clients) {
        return clients.stream()
                .collect(Collectors.toMap(
                                client -> client.getMetadata().code(),
                                client -> client
                        )
                );
    }

    private Map<String, Provider> providerByCode(Collection<Provider> providers) {
        return providers.stream()
                .collect(Collectors.toMap(Provider::getProviderCode, provider -> provider));
    }

    private Map<ProviderClient, Provider> saveNewProvidersAndMap(
            Map<String, ProviderClient> providerClientByCode,
            Map<String, Provider> providerByCode
    ) {
        List<Provider> newProviders = new ArrayList<>();
        Map<ProviderClient, Provider> providerMap = providerClientByCode.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        entry -> {
                            if (!providerByCode.containsKey(entry.getKey())) {
                                Provider newProvider = createProvider(entry.getValue().getMetadata());
                                newProviders.add(newProvider);
                                return newProvider;
                            } else {
                                return providerByCode.get(entry.getKey());
                            }
                        })
                );

        if (!newProviders.isEmpty()) {
            providerService.saveAll(newProviders);
        }

        return providerMap;
    }

    private List<String> getCurrencyPairs(List<String> codes) {
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < codes.size() - 1; i++) {
            for (int j = i + 1; j < codes.size(); j++) {
                String pair = codes.get(i) + codes.get(j);
                String anotherPair = codes.get(j) + codes.get(i);
                pairs.add(pair);
                pairs.add(anotherPair);
            }
        }

        return pairs;
    }

    private Provider createProvider(ProviderMetadata metadata) {
        Provider provider = new Provider();
        provider.setProviderCode(metadata.code());
        provider.setProviderName(metadata.name());
        provider.setPriority(metadata.priority());
        provider.setActive(true);
        provider.setDescription(metadata.description());
        provider.setCreatedAt(Instant.now());
        return provider;
    }

    private List<CurrencyRate> collectRateFromProvider(
            Map.Entry<ProviderClient, Provider> entry,
            List<String> codePairs,
            Instant beginTime,
            Instant endTime
    ) {
        ProviderClient client = entry.getKey();
        Provider provider = entry.getValue();
        log.debug("Trying to update currency rates for provider: {}", provider.getProviderName());
        try {
            List<String> clientCurrencyPairs = client.getActiveCurrencies();
            List<String> currencyPairs = new ArrayList<>(codePairs);
            currencyPairs.retainAll(clientCurrencyPairs);
            Map<String, BigDecimal> currencyRates = client.getCurrencyRates(currencyPairs);
            log.debug("Provider [{}] return {} currency rates", provider.getProviderName(), currencyRates.size());
            return currencyRates.entrySet()
                    .stream()
                    .map(entrySet -> {
                        CurrencyPair currencyPair = new CurrencyPair(entrySet.getKey());
                        return currencyRateMapper.create(currencyPair, entrySet.getValue(), beginTime, endTime, provider);
                    })
                    .toList();
        } catch (Exception ex) {
            log.error("Cannot update currency rates for provider: [{}], exception: {}", provider.getProviderName(), ex.getMessage());
            return List.of();
        }
    }
}
