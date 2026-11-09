package com.example.individualsapi.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@RequiredArgsConstructor
public class AdminTokenClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final AdminTokenHolder tokenHolder;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!tokenHolder.isExpired()) {
            request.getHeaders().setBearerAuth(tokenHolder.getAccessToken());
        }
        return execution.execute(request, body);
    }
}
