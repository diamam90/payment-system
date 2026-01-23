package com.example.transactionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
public class SecurityConfig {

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String TRANSACTION_SERVICE_ROLE = "transaction-service";
    private static final String ROLES = "roles";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                        authorize -> {
                            applyUnsecuredPath(authorize);
                            applySecuredPath(authorize);
                            authorize.anyRequest().authenticated();
                        }
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(converter())
                ))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> converter() {
        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authorityConverter());
        return jwtConverter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> authorityConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional.ofNullable(jwt.getClaimAsMap(RESOURCE_ACCESS))
                    .map(access -> access.get(TRANSACTION_SERVICE_ROLE))
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

    private void applyUnsecuredPath
            (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers("/actuator/health").permitAll();
        registry.requestMatchers("/actuator/prometheus").permitAll();
    }

    private void applySecuredPath
            (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers("/api/v1/transactions/**").hasAuthority("transaction_service_wr");
        registry.requestMatchers("/api/v1/wallets/**").hasAuthority("transaction_service_wr");
    }
}
