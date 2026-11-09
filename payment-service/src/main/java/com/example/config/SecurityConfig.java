package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
public class SecurityConfig {

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String PAYMENT_SERVICE_ROLES = "payment-service";
    private static final String ROLES = "roles";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.securityMatcher("/api/v1/**", "/private/api/v1/payments/**")
                .authorizeHttpRequests(req -> req.anyRequest().hasAuthority("payment_service_wr"))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authorityConverter());
        return jwtAuthenticationConverter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> authorityConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional.ofNullable(jwt.getClaimAsMap(RESOURCE_ACCESS))
                    .map(access -> access.get(PAYMENT_SERVICE_ROLES))
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
