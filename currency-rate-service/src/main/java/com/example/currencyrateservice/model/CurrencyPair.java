package com.example.currencyrateservice.model;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class CurrencyPair {

    private final String source;
    private final String destination;
    private static final Pattern VALID_CURRENCY_PAIR_PATTERN = Pattern.compile("[A-Z]{6}");

    public CurrencyPair(String codePair) {
        if (!VALID_CURRENCY_PAIR_PATTERN.matcher(codePair).find()) {
            throw new IllegalArgumentException(
                    String.format(
                            "CodePair %s does not match pattern %s",
                            codePair,
                            VALID_CURRENCY_PAIR_PATTERN.pattern()
                    )
            );
        }
        this.source = codePair.substring(0, 3);
        this.destination = codePair.substring(3);
    }
}
