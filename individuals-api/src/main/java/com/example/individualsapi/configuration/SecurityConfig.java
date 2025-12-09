package com.example.individualsapi.configuration;

import com.example.individualsapi.filter.AdminTokenReceiverFilter;
import com.example.individualsapi.filter.CountAndTimerWebFilter;
import com.example.individualsapi.service.TokenService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHORIZATION;

@Slf4j
@Configuration
@EnableReactiveMethodSecurity
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String INDIVIDUALS_API_ROLES = "individuals-api";
    private static final String ROLES = "roles";

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http,
                                                       MeterRegistry registry,
                                                       TokenService tokenService,
                                                       AdminTokenHolder tokenHolder) {

        return http.authorizeExchange(exchange -> {
                    applyPublicRoutes(exchange);
                    applyProtectedRoutes(exchange);
                })
                .addFilterBefore(new CountAndTimerWebFilter(registry), AUTHENTICATION)
                .addFilterAfter(new AdminTokenReceiverFilter(tokenService, tokenHolder), AUTHORIZATION)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2ResourceServer(oAuth2ResourceServer ->
                        oAuth2ResourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
                .build();
    }

    private void applyPublicRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchange) {
        exchange
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/refresh-token").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/individuals").permitAll()
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll();
    }

    private void applyProtectedRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchange) {
        exchange.anyExchange().authenticated();
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();
        var jwtAuthorityConverter = new ReactiveJwtGrantedAuthoritiesConverterAdapter(authorityConverter());
        converter.setJwtGrantedAuthoritiesConverter(jwtAuthorityConverter);

        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> authorityConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional.ofNullable(jwt.getClaimAsMap(RESOURCE_ACCESS))
                    .map(access -> access.get(INDIVIDUALS_API_ROLES))
                    .map(access -> (Map<?, ?>) access)
                    .map(access -> access.get(ROLES))
                    .map(roles -> (List<?>) roles)
                    .ifPresent(roles -> roles.stream()
                            .map(role -> (String) role)
                            .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)))
                    );

            return authorities;
        };
    }
}
