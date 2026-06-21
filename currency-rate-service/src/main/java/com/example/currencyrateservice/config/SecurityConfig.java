package com.example.currencyrateservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String CURRENCY_RATE_SERVICE_ROLE = "currency-rate-service";
    private static final String ROLES = "roles";

    private static final String ACCEPTED_AUTHORITY = "currency_rate_service_wr";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize ->
                        authorize.requestMatchers("/actuator/**").permitAll()
                                .anyRequest().hasAuthority(ACCEPTED_AUTHORITY)
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter())))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    Converter<Jwt, ? extends AbstractAuthenticationToken> converter() {
        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authorityConverter());
        return jwtConverter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> authorityConverter() {
        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional.ofNullable(jwt.getClaimAsMap(RESOURCE_ACCESS))
                    .map(access -> access.get(CURRENCY_RATE_SERVICE_ROLE))
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
