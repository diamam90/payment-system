package com.example.individualsapi.configuration;

import com.example.currency.api.CurrencyApi;
import com.example.currency.api.CurrencyRateApi;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class HttpServiceConfig {

    private final AppProperties properties;
    private final AdminTokenHolder tokenHolder;
    private final ObservationRegistry observationRegistry;

    @Bean
    CurrencyApi currencyApi(HttpServiceProxyFactory currateHttpServiceProxyFactory) {
        return currateHttpServiceProxyFactory.createClient(CurrencyApi.class);
    }

    @Bean
    CurrencyRateApi currencyRateApi(HttpServiceProxyFactory currateHttpServiceProxyFactory) {
        return currateHttpServiceProxyFactory.createClient(CurrencyRateApi.class);
    }

    @Bean
    WebClient currateWebClient() {
        return WebClient
                .builder()
                .observationRegistry(observationRegistry)
                .baseUrl(properties.getCurrencyRate().getBaseUrl())
                .filter(new AdminTokenFilterFunction(tokenHolder))
                .build();
    }

    @Bean
    HttpServiceProxyFactory currateHttpServiceProxyFactory(WebClient currateWebClient) {
        WebClientAdapter webClientAdapter = WebClientAdapter.create(currateWebClient);
        return HttpServiceProxyFactory.builderFor(webClientAdapter).build();
    }
}
