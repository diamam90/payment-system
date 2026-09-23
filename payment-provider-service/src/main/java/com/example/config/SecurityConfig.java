package com.example.config;

import com.example.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final WebhookClientProperties webhookClientProperties;

    @Order(2)
    @Bean
    SecurityFilterChain transactionFilterChain(HttpSecurity http, AuthenticationManager transactionAuthenticationManager) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/v1/transactions/**").authenticated()
                        .requestMatchers("/api/v1/payouts/**").authenticated()
                        .requestMatchers("/actuator/**").permitAll())
                .authenticationManager(transactionAuthenticationManager)
                .build();
    }

    @Bean
    AuthenticationManager transactionAuthenticationManager(MerchantService merchantService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(merchantService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Order(1)
    @Bean
    SecurityFilterChain webhookFilterChain(HttpSecurity http, AuthenticationManager webhookAuthenticationManager) {
        return http
                .securityMatcher("/webhook/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .anyRequest().authenticated())
                .authenticationManager(webhookAuthenticationManager)
                .build();
    }

    @Bean
    AuthenticationManager webhookAuthenticationManager() {
        log.debug("Инициализируем вебхук клиентов, количество: {}", webhookClientProperties.webhookClients.size());
        List<UserDetails> users = webhookClientProperties.webhookClients.entrySet()
                .stream()
                .map(entry -> User
                        .withUsername(entry.getKey())
                        .password(entry.getValue())
                        .build()
                )
                .toList();

        UserDetailsService userDetailsService = new InMemoryUserDetailsManager(users);
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
