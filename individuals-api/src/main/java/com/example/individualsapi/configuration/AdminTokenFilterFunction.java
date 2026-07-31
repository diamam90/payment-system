package com.example.individualsapi.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AdminTokenFilterFunction implements ExchangeFilterFunction {

    private final AdminTokenHolder tokenHolder;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (!tokenHolder.isExpired()) {
            return next.exchange(
                    ClientRequest.from(request)
                            .headers(h -> h.setBearerAuth(tokenHolder.getAccessToken()))
                            .build()
            );
        }
        return next.exchange(request);
    }
}
