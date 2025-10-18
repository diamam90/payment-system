package com.example.individualsapi.configuration;

import com.example.individualsapi.service.TokenService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

@RequiredArgsConstructor
public class AdminTokenInterceptor implements RequestInterceptor {

    private final TokenService tokenService;

    @Override
    public void apply(RequestTemplate template) {
        tokenService.adminToken().log("BEARER TOKEN INTERCEPTOR")
                .subscribe(token -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
