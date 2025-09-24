package com.example.annotation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetricNames {
    HTTP_LOGIN("http_reactive_login"),
    HTTP_REGISTRATION("http_reactive_registration"),
    HTTP_REQUEST("http_reactive_all_requests"),
    HTTP_TIMER_REQUEST("http_timer_request");

    private final String code;
}
