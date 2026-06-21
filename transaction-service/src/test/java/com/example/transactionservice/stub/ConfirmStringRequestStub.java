package com.example.transactionservice.stub;

import java.math.BigDecimal;
import java.util.UUID;

public class ConfirmStringRequestStub {

    public static String depositConfirmRequest(UUID walletId) {
        return //language=JSON
                """
                        {
                          "type": "DEPOSIT",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "%s",
                          "amount": 250,
                          "comment": "На еду"
                        }
                        """.formatted(walletId);
    }


    public static String withdrawalConfirmRequest(BigDecimal amount) {
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


    public static String transferConfirmRequest(BigDecimal amount, BigDecimal rate) {
        return //language=JSON
                """
                        {
                          "type": "TRANSFER",
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "walletUid": "fa65903f-2441-4ada-81fa-d8cd6a2e00a1",
                          "amount": "%s",
                          "rate": "%s",
                          "targetWalletUid": "fa65903f-2441-4ada-81fa-d8cd6a2e00a2",
                          "targetUserUid": "00000000-0000-0000-0000-000000000003",
                          "comment": "Не трать все сразу"
                        }
                        """.formatted(amount, rate);
    }
}
