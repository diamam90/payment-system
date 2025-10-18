package com.example.individualsapi.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


@Slf4j
@AllArgsConstructor
public class BearerTokenInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate input) {
        ReactiveSecurityContextHolder.getContext()
                .filter(c -> c.getAuthentication() != null)
                .map(SecurityContext::getAuthentication)
                .map(auth -> (JwtAuthenticationToken) auth)
                .map(token -> input.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .log("BEARER TOKEN");

    }
}
