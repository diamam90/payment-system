package com.example.config;

import com.example.fake.api.PayoutApi;
import com.example.fake.api.TransactionApi;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FakePaymentProviderProperties.class)
public class FakePaymentProviderConfig {

    private final FakePaymentProviderProperties providerProperties;

    @Bean
    PayoutApi payoutApi(@Qualifier("fakeProviderRestClient") RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter)
                .build()
                .createClient(PayoutApi.class);
    }

    @Bean
    TransactionApi transactionApi(@Qualifier("fakeProviderRestClient") RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter)
                .build()
                .createClient(TransactionApi.class);
    }

    @Bean
    RestClient fakeProviderRestClient(ObservationRegistry registry) {
        return RestClient.builder()
                .baseUrl(providerProperties.getBaseUrl())
                .observationRegistry(registry)
                .requestInterceptor(
                        new BasicAuthenticationInterceptor(
                                providerProperties.getMerchantId(),
                                providerProperties.getMerchantPassword()
                        ))
                .build();
    }
}
