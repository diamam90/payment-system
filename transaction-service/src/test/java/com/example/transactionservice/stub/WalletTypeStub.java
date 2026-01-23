package com.example.transactionservice.stub;

import com.example.transactionservice.entity.WalletType;

import java.time.Instant;
import java.util.Set;

public class WalletTypeStub {

    public static WalletType rub_wallet() {
        WalletType type = new WalletType();
        type.setName("my wallet");
        type.setStatus("active");
        type.setCurrencyCode("RUB");
        type.setUserType("individual");
        type.setArchivedAt(Instant.parse("2030-12-12T23:59:59+00:00"));
        type.setCreator("admin");
        type.setWallets(Set.of());

        return type;
    }

    public static WalletType usd_wallet() {
        WalletType type = new WalletType();
        type.setName("unknown wallet");
        type.setStatus("active");
        type.setCurrencyCode("USD");
        type.setUserType("individual");
        type.setArchivedAt(Instant.parse("2040-01-01T00:00:00+00:00"));
        type.setCreator("admin");
        type.setWallets(Set.of());

        return type;
    }
}
