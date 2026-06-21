package com.example.currencyrateservice.client;

import com.example.currencyrateservice.config.LoggingClientInterceptor;
import com.example.currencyrateservice.exception.BadRequestException;
import com.example.currencyrateservice.model.CurrateCurrencyRateResponse;
import com.example.currencyrateservice.model.CurrateCurrencyResponse;
import com.example.currencyrateservice.model.ProviderMetadata;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(CurrateProperties.class)
public class CurrateClient implements ProviderClient {

    private final RestClient currateClient;
    private final CurrateProperties currate;

    public CurrateClient(CurrateProperties currate) {
        this.currate = currate;
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.TEXT_HTML));
        this.currateClient = RestClient.builder()
                .baseUrl(currate.getBaseUrl())
                .messageConverters(List.of(converter))
                .requestInterceptor(new LoggingClientInterceptor())
                .build();
    }

    @Override
    public List<String> getActiveCurrencies() {
        List<String> availablePairs = currencyList().data();
        List<String> extendedClientCurrencyPairs = new ArrayList<>(availablePairs.size() * 2);
        availablePairs.forEach(pair -> {
            String srcCode = pair.substring(0, 3);
            String destCode = pair.substring(3);
            extendedClientCurrencyPairs.add(srcCode + destCode);
            extendedClientCurrencyPairs.add(destCode + srcCode);
        });

        return extendedClientCurrencyPairs;
    }

    @Override
    public Map<String, BigDecimal> getCurrencyRates(List<String> pairs) {
        CurrateCurrencyRateResponse response = rates(pairs);
        if (!response.status().equals(200)) {
            throw new BadRequestException(response.message());
        }
        return response.data();
    }

    private CurrateCurrencyRateResponse rates(List<String> pairs) {
        var queryParams = new HashMap<String, Object>();
        if (!pairs.isEmpty()) {
            queryParams.put("pairs", String.join(",", pairs));
        }
        queryParams.put("method", "rates");
        queryParams.put("apiKey", currate.getApiKey());
        return currateClient.get()
                .uri("/api/?get={method}&pairs={pairs}&key={apiKey}", queryParams)
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .body(CurrateCurrencyRateResponse.class);
    }

    private CurrateCurrencyResponse currencyList() {
        var queryParams = new HashMap<String, Object>();
        queryParams.put("method", "currency_list");
        queryParams.put("apiKey", currate.getApiKey());

        return currateClient.get()
                .uri("/api/?get={method}&key={apiKey}", queryParams)
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .body(CurrateCurrencyResponse.class);
    }

    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
                currate.getCode(),
                currate.getName(),
                currate.getPriority(),
                currate.getDescription()
        );
    }
}
