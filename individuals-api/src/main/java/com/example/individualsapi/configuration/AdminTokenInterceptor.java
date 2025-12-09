package com.example.individualsapi.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

@RequiredArgsConstructor
public class AdminTokenInterceptor implements RequestInterceptor {

    private final AdminTokenHolder tokenHolder;

    @Override
    public void apply(RequestTemplate template) {
        if (!tokenHolder.isExpired()) {
             template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHolder.getAccessToken());
        }
    }
}
