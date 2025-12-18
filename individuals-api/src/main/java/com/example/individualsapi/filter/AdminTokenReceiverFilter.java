package com.example.individualsapi.filter;

import com.example.individualsapi.configuration.AdminTokenHolder;
import com.example.individualsapi.service.TokenService;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class AdminTokenReceiverFilter implements WebFilter {

    private final TokenService tokenService;
    private final AdminTokenHolder tokenHolder;

    @NewSpan("individuals_api_admin_filter")
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!tokenHolder.isExpired()) {
            return chain.filter(exchange);
        } else {
            return tokenService.adminToken()
                    .doOnError(ex -> log.error("Cannot update admin token, ex: {}", ex.getMessage()))
                    .onErrorComplete()
                    .then(chain.filter(exchange))
                    .contextCapture();
        }
    }
}
