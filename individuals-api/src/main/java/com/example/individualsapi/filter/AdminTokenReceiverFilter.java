package com.example.individualsapi.filter;

import com.example.individualsapi.configuration.AdminTokenHolder;
import com.example.individualsapi.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AdminTokenReceiverFilter implements WebFilter {

    private final TokenService tokenService;
    private final AdminTokenHolder tokenHolder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!tokenHolder.isExpired()) {
            return chain.filter(exchange);
        } else {
            return tokenService.adminToken()
                    .onErrorComplete()
                    .then(chain.filter(exchange));
        }
    }
}
