package com.example.currencyrateservice.client;

import com.example.currate.api.CurrateApi;
import com.example.currencyrateservice.config.LoggingClientInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CurrateProperties.class)
public class CurrateApiConfig {

    private final CurrateProperties currate;

    @Bean
    CurrateApi currateApi() {
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
        List<MediaType> supportedMediaTypes = converter.getSupportedMediaTypes();
        List<MediaType> newSupportedMediaTypes = new ArrayList<>(supportedMediaTypes);
        newSupportedMediaTypes.add(MediaType.TEXT_HTML);
        converter.setSupportedMediaTypes(newSupportedMediaTypes);

        RestClient restClient = RestClient
                .builder()
                .baseUrl(currate.getBaseUrl())
                .requestInterceptor(new LoggingClientInterceptor())
                .configureMessageConverters(builder -> builder.withJsonConverter(converter))
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter)
                .customArgumentResolver(new CurrateArgumentResolver())
                .build()
                .createClient(CurrateApi.class);
    }
}
