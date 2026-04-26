package com.example.transactionservice.stub;

import java.math.BigDecimal;
import java.util.UUID;

public class InitStringRequestStub {

    public static String depositInitRequest(UUID walletId) {
        return //language=JSON
                """
                        {
                          "type": "DEPOSIT",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "%s",
                          "amount": 250
                        }
                        """.formatted(walletId);
    }

    public static String transferInitRequest(BigDecimal amount) {
        return //language=JSON
                """
                        {
                          "type": "TRANSFER",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "fa65903f-2441-4ada-81fa-d8cd6a2e00a1",
                          "amount": "%s",
                          "targetWalletUid": "fa65903f-2441-4ada-81fa-d8cd6a2e00a2",
                          "targetUserUid": "00000000-0000-0000-0000-000000000003"
                        }
                        """.formatted(amount);
    }

    public static String withdrawalInitRequest(BigDecimal amount) {
        return //language=JSON
                """
                        {
                          "type": "WITHDRAWAL",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "fa65903f-2441-4ada-81fa-d8cd6a2e00a1",
                          "amount": "%s"
                        }
                        """.formatted(amount);
    }
}
