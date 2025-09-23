package com.example.configuration;

import com.example.filter.CountAndTimerWebFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http, MeterRegistry registry) {

        return http.authorizeExchange(exchange -> {
                    applyPublicRoutes(exchange);
                    applyProtectedRoutes(exchange);
                })
                .addFilterBefore(new CountAndTimerWebFilter(registry), AUTHENTICATION)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2ResourceServer(oAuth2ResourceServer ->
                        oAuth2ResourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
                .build();
    }

    private void applyPublicRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchange) {
        exchange.pathMatchers(HttpMethod.POST, "/api/v1/registration").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/login").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/refresh-token").permitAll()
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll();
    }

    private void applyProtectedRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchange) {
        exchange.anyExchange().authenticated();
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();
        return converter;
    }
}
