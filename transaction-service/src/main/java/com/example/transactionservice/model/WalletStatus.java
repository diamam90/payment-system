package com.example.transactionservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletStatus {

    ACTIVE("active"),
    INACTIVE("inactive");

    private final String code;
}
