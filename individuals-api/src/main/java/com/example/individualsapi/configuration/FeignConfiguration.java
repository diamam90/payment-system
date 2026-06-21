package com.example.individualsapi.configuration;

import com.example.currency.api.CurrencyRateApiClient;
import com.example.person.api.IndividualsApiClient;
import com.example.person.api.PrivateApiClient;
import com.example.transaction.api.TransactionApiClient;
import com.example.transaction.api.WalletApiClient;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@RequiredArgsConstructor
@Import(FeignClientsConfiguration.class)
public class FeignConfiguration {

    private final AppProperties properties;
    private final AdminTokenHolder tokenHolder;

    @Bean
    PrivateApiClient privateApiClient(Contract contract,
                                      Encoder encoder,
                                      Decoder decoder,
                                      MicrometerObservationCapability capability) {
        return Feign.builder()
                .contract(contract)
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)
                .requestInterceptor(new AdminTokenInterceptor(tokenHolder))
                .target(PrivateApiClient.class, properties.getPerson().getBaseUrl());
    }

    @Bean
    IndividualsApiClient individualsApiClient(Contract contract,
                                              Encoder encoder,
                                              Decoder decoder,
                                              MicrometerObservationCapability capability) {
        return Feign.builder()
                .contract(contract)
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)
                .requestInterceptor(new AdminTokenInterceptor(tokenHolder))
                .target(IndividualsApiClient.class, properties.getPerson().getBaseUrl());
    }

    @Bean
    WalletApiClient walletApiClient(Contract contract,
                                    Encoder encoder,
                                    Decoder decoder,
                                    MicrometerObservationCapability capability) {
        return Feign.builder()
                .contract(contract)
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)
                .requestInterceptor(new AdminTokenInterceptor(tokenHolder))
                .target(WalletApiClient.class, properties.getTransaction().getBaseUrl());
    }

    @Bean
    TransactionApiClient transactionApiClient(Contract contract,
                                              Encoder encoder,
                                              Decoder decoder,
                                              MicrometerObservationCapability capability) {
        return Feign.builder()
                .contract(contract)
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)
                .requestInterceptor(new AdminTokenInterceptor(tokenHolder))
                .target(TransactionApiClient.class, properties.getTransaction().getBaseUrl());
    }

    @Bean
    CurrencyRateApiClient currencyRateApiClient(
            Contract contract,
            Encoder encoder,
            Decoder decoder,
            MicrometerObservationCapability capability
    ) {
        return Feign.builder()
                .contract(contract)
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(capability)
                .requestInterceptor(new AdminTokenInterceptor(tokenHolder))
                .target(CurrencyRateApiClient.class, properties.getCurrencyRate().getBaseUrl());
    }

    @Bean
    HttpMessageConverters converters() {
        return new HttpMessageConverters();
    }

    @Bean
    MicrometerObservationCapability micrometerObservationCapability(ObservationRegistry registry) {
        return new MicrometerObservationCapability(registry, new CustomFeignObservationConvention());
    }
}
