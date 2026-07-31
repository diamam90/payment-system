package com.example.currencyrateservice.model;

import java.util.List;

public record CurrateCurrencyResponse(
        Integer status,
        String message,
        List<String> data
) {
}
