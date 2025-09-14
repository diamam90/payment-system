package com.example.configuration;

import com.example.filter.CountAndTimerWebFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http, MeterRegistry registry) {

        http.authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/v1/api/registration").permitAll()
                        .pathMatchers(HttpMethod.POST, "/v1/api/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/v1/api/refresh-token").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .addFilterBefore(new CountAndTimerWebFilter(registry), AUTHENTICATION)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2ResourceServer(oAuth2ResourceServerSpec ->
                        oAuth2ResourceServerSpec.jwt(Customizer.withDefaults()));
        return http.build();
    }


}
