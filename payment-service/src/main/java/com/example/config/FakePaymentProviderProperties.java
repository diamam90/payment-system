package com.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "payment-service.fake-payment-provider")
public class FakePaymentProviderProperties {

    private String baseUrl;

    private String merchantId;

    private String merchantPassword;

}
