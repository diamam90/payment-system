package com.example.annotation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetricTags {

    URI("uri"),
    STATUS("status");

    private final String code;
}
