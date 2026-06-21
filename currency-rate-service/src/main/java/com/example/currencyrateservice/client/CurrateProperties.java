package com.example.currencyrateservice.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "currency-rate-service.provider.currate")
public class CurrateProperties {
    private String baseUrl;
    private String apiKey;
    private String code;
    private String name;
    private Integer priority;
    private String description;
}
