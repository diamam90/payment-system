package com.example.personservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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
        return (Converter<Jwt, JwtAuthenticationToken>) jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> realmMap) {
                if (realmMap.get("roles") instanceof List<?> roles) {
                    roles.stream().filter(role -> role instanceof String)
                            .forEach(role -> authorities.add(new SimpleGrantedAuthority((String) role)));
                }
            }
            authorities.add(new SimpleGrantedAuthority(jwt.getClaimAsString("scope")));
            String principal = jwt.getClaimAsString("sub");
            return new JwtAuthenticationToken(jwt, authorities, principal);
        };
    }

    private void applyUnsecuredPath
            (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers(HttpMethod.POST, "/api/v1/individuals").permitAll();
    }

    private void applySecuredPath
            (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers("/private/**").hasAuthority("payment_system_admin");
    }
}
