package com.example.individualsapi.configuration;

import com.example.currency.api.CurrencyApi;
import com.example.currency.api.CurrencyRateApi;
import com.example.payment.api.PaymentApi;
import com.example.payment.api.PaymentMethodApi;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
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
    RestClient currateRestClient() {
        return RestClient
                .builder()
                .observationRegistry(observationRegistry)
                .baseUrl(properties.getCurrencyRate().getBaseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .requestInterceptor(new AdminTokenClientHttpRequestInterceptor(tokenHolder))
                .build();
    }

    @Bean
    HttpServiceProxyFactory currateHttpServiceProxyFactory(RestClient currateRestClient) {
        RestClientAdapter restClientAdapter = RestClientAdapter.create(currateRestClient);
        return HttpServiceProxyFactory.builderFor(restClientAdapter).build();
    }

    @Bean
    RestClient paymentRestClient() {
        return RestClient
                .builder()
                .observationRegistry(observationRegistry)
                .baseUrl(properties.getPayment().getBaseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .requestInterceptor(new AdminTokenClientHttpRequestInterceptor(tokenHolder))
                .build();
    }

    @Bean
    HttpServiceProxyFactory paymentHttpServiceProxyFactory(RestClient paymentRestClient) {
        RestClientAdapter restClientAdapter = RestClientAdapter.create(paymentRestClient);
        return HttpServiceProxyFactory.builderFor(restClientAdapter)
                .customArgumentResolver(new PaymentMethodFilterArgumentResolver())
                .build();
    }

    @Bean
    PaymentApi paymentApi(HttpServiceProxyFactory paymentHttpServiceProxyFactory) {
        return paymentHttpServiceProxyFactory.createClient(PaymentApi.class);
    }

    @Bean
    PaymentMethodApi paymentMethodApi(HttpServiceProxyFactory paymentHttpServiceProxyFactory) {
        return paymentHttpServiceProxyFactory.createClient(PaymentMethodApi.class);
    }
}
