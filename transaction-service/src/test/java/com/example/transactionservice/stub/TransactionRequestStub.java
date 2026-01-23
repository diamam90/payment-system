package com.example.transactionservice.stub;

import com.example.transaction.dto.*;
import com.example.transactionservice.entity.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionRequestStub {

    public static DepositInitRequest depositInit() {
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return new DepositInitRequest(TransactionType.DEPOSIT.name(), userId, walletId, BigDecimal.valueOf(250));
    }

    public static TransferInitRequest transferInit() {
        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");

        var targetUserId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        var targetWalletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a2");

        var request = new TransferInitRequest();
        request.setUserUid(userId);
        request.setWalletUid(walletId);
        request.setTargetUserUid(targetUserId);
        request.setTargetWalletUid(targetWalletId);
        request.setAmount(BigDecimal.valueOf(50));

        return request;
    }

    public static WithdrawalInitRequest withdrawalInit() {
        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        var amount = BigDecimal.valueOf(60.666);
        return new WithdrawalInitRequest(TransactionType.WITHDRAWAL.name(), userId, walletId, amount);
    }

    public static DepositConfirmRequest depositConfirm() {
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var amount = BigDecimal.valueOf(100.012);

        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setAmount(amount);
        request.setWalletUid(walletId);
        request.setUserUid(userId);
        request.setComment("Test Comment");

        return request;
    }

    public static WithdrawalConfirmRequest withdrawalConfirm() {
        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");
        var amount = BigDecimal.valueOf(60.666);
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setUserUid(userId);
        request.setAmount(amount);
        request.setWalletUid(walletId);
        request.setComment("Test comment");

        return request;
    }

    public static TransferConfirmRequest transferConfirm() {
        var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var walletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1");

        var targetUserId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        var targetWalletId = UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a2");

        TransferConfirmRequest request = new TransferConfirmRequest();
        request.setUserUid(userId);
        request.setWalletUid(walletId);
        request.setTargetUserUid(targetUserId);
        request.setTargetWalletUid(targetWalletId);
        request.setAmount(BigDecimal.valueOf(50));

        return request;
    }

}
