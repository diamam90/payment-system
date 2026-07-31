package com.example.currencyrateservice.model;

public record ProviderMetadata(
        String code,
        String name,
        Integer priority,
        String description
) {
}
