package com.example.individualsapi.stub;

import com.example.individuals.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.UUID;

public class TransactionStub {

    private static final String DEPOSIT_TYPE = "DEPOSIT";
    private static final String TRANSFER_TYPE = "TRANSFER";
    private static final String WITHDRAWAL_TYPE = "WITHDRAWAL";

    public static class IndividualService {

        public static DepositInitRequest depositInitRequest(UUID userId, UUID walletId) {
            DepositInitRequest request = new DepositInitRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType(DEPOSIT_TYPE);
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            return request;
        }

        public static WithdrawalInitRequest withdrawalInitRequest(UUID userId, UUID walletId) {
            WithdrawalInitRequest request = new WithdrawalInitRequest();
            request.setAmount(BigDecimal.valueOf(50));
            request.setType(WITHDRAWAL_TYPE);
            request.setUserUid(userId);
            request.setWalletUid(walletId);

            return request;
        }

        public static TransferInitRequest transferInitRequest(UUID sourceUserId, UUID sourceWalletId, UUID targetUserId, UUID targetWalletId) {
            TransferInitRequest request = new TransferInitRequest();
            request.setAmount(BigDecimal.valueOf(100));
            request.setUserUid(sourceUserId);
            request.setWalletUid(sourceWalletId);
            request.setType(TRANSFER_TYPE);
            request.setTargetUserUid(targetUserId);
            request.setTargetWalletUid(targetWalletId);

            return request;
        }

        public static TransactionInitResponse depositInitResponse(UUID walletId) {
            TransactionInitResponse response = new TransactionInitResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN.setScale(2, RoundingMode.HALF_EVEN));
            response.setFee(BigDecimal.valueOf(1.1d).setScale(2, RoundingMode.HALF_EVEN));

            return response;
        }

        public static TransactionInitResponse withdrawalInitResponse(UUID walletId) {
            TransactionInitResponse response = new TransactionInitResponse();
            response.setAmount(BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_EVEN));
            response.setFee(BigDecimal.valueOf(0.56).setScale(2, RoundingMode.HALF_EVEN));
            response.setWalletUid(walletId);

            return response;
        }

        public static TransactionInitResponse transferInitResponse(UUID sourceWalletId, UUID targetWalletId) {
            TransactionInitResponse response = new TransactionInitResponse();
            response.setWalletUid(sourceWalletId);
            response.setTargetWalletUid(targetWalletId);
            response.setAmount(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_EVEN));
            response.setFee(BigDecimal.valueOf(7.62).setScale(2, RoundingMode.HALF_EVEN));

            return response;
        }

        public static TransactionConfirmRequest depositConfirmRequest(UUID userId, UUID walletId) {
            DepositConfirmRequest request = new DepositConfirmRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType(DEPOSIT_TYPE);
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            request.setComment("confirm comment");
            return request;
        }

        public static TransactionConfirmRequest withdrawalConfirmRequest(UUID userId, UUID walletId) {
            WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
            request.setWalletUid(userId);
            request.setType(WITHDRAWAL_TYPE);
            request.setUserUid(walletId);
            request.setAmount(BigDecimal.valueOf(228));
            request.setComment("COM 1");

            return request;
        }

        public static TransactionConfirmRequest transferConfirmRequest(UUID sourceUserId, UUID sourceWalletId, UUID targetUserId, UUID targetWalletId) {
            TransferConfirmRequest request = new TransferConfirmRequest();
            request.setUserUid(sourceUserId);
            request.setWalletUid(sourceWalletId);
            request.setTargetUserUid(targetUserId);
            request.setTargetWalletUid(targetWalletId);
            request.setAmount(BigDecimal.valueOf(1000.05));
            request.setType(TRANSFER_TYPE);
            return request;
        }

        public static TransactionConfirmResponse depositConfirmResponse(UUID userId, UUID walletId) {
            TransactionConfirmResponse response = new TransactionConfirmResponse();
            response.setUserUid(userId);
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN.setScale(2, RoundingMode.HALF_EVEN));
            response.setFee(BigDecimal.valueOf(1.1d).setScale(2, RoundingMode.HALF_EVEN));
            response.setStatus("PENDING");
            response.setComment("confirm comment");
            response.setType(DEPOSIT_TYPE);

            return response;
        }

        public static TransactionConfirmResponse withdrawalConfirmResponse(UUID userId, UUID walletId) {
            TransactionConfirmResponse response = new TransactionConfirmResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.valueOf(228).setScale(2, RoundingMode.HALF_EVEN));
            response.setFee(BigDecimal.valueOf(1.1d).setScale(2, RoundingMode.HALF_EVEN));
            response.setStatus("SUCCESS");
            response.setComment("confirm comment");
            response.setType(DEPOSIT_TYPE);
            response.setUserUid(userId);

            return response;
        }

        public static TransactionConfirmResponse transferConfirmResponse(UUID sourceUserId, UUID sourceWalletId, UUID targetWalletId) {
            TransactionConfirmResponse response = new TransactionConfirmResponse();
            response.setUid(UUID.fromString("66666666-6666-6666-6666-666666666666"));
            response.setUserUid(sourceUserId);
            response.setWalletUid(sourceWalletId);
            response.setTargetWalletUid(targetWalletId);
            response.setAmount(BigDecimal.valueOf(1000.05).setScale(2, RoundingMode.HALF_EVEN));
            response.setFee(BigDecimal.valueOf(0.22).setScale(2, RoundingMode.HALF_EVEN));
            response.setStatus("CONFIRM");

            return response;
        }

        public static TransactionFilterRequest transactionFilterRequest(UUID userId, UUID walletId) {
            TransactionFilterRequest filter = new TransactionFilterRequest();
            Params params = new Params();
            params.setStatus(Params.StatusEnum.COMPLETED);
            params.setDateFrom(ZonedDateTime.parse("2025-04-04T00:00:00+00:00"));
            params.setDateTo(ZonedDateTime.parse("2025-05-05T00:00:00+00:00"));
            params.setUserUid(userId);
            params.setWalletUid(walletId);
            filter.setFilter(params);
            Pageable pageable = new Pageable();
            pageable.setPage(5);
            pageable.setSize(10);
            filter.setPageable(pageable);

            return filter;
        }

        public static TransactionStatusResponse transactionStatusResponse(UUID transactionId) {
            TransactionStatusResponse response = new TransactionStatusResponse();
            response.setStatus("active");
            response.setUid(transactionId);
            response.setType("FAILED");
            response.setFailureReason("External service error");
            return response;
        }
    }

    public static class TransactionService {

        public static com.example.transaction.dto.TransactionInitRequest depositInitRequest(UUID userId, UUID walletId) {
            com.example.transaction.dto.DepositInitRequest request = new com.example.transaction.dto.DepositInitRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType(DEPOSIT_TYPE);
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            return request;
        }

        public static com.example.transaction.dto.WithdrawalInitRequest withdrawalInitRequest(UUID userId, UUID walletId) {
            com.example.transaction.dto.WithdrawalInitRequest request = new com.example.transaction.dto.WithdrawalInitRequest();
            request.setAmount(BigDecimal.valueOf(50));
            request.setType(WITHDRAWAL_TYPE);
            request.setUserUid(userId);
            request.setWalletUid(walletId);

            return request;
        }

        public static com.example.transaction.dto.TransferInitRequest transferInitRequest(UUID sourceUserId, UUID sourceWalletId, UUID targetUserId, UUID targetWalletId, BigDecimal rate) {
            com.example.transaction.dto.TransferInitRequest request = new com.example.transaction.dto.TransferInitRequest();
            request.setAmount(BigDecimal.valueOf(100));
            request.setUserUid(sourceUserId);
            request.setWalletUid(sourceWalletId);
            request.setType(TRANSFER_TYPE);
            request.setTargetUserUid(targetUserId);
            request.setTargetWalletUid(targetWalletId);
            request.setRate(rate);

            return request;
        }

        public static com.example.transaction.dto.TransactionInitResponse depositInitResponse(UUID walletId) {
            com.example.transaction.dto.TransactionInitResponse response = new com.example.transaction.dto.TransactionInitResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN);
            response.setFee(BigDecimal.valueOf(1.1d));

            return response;
        }

        public static com.example.transaction.dto.TransactionInitResponse withdrawalInitResponse(UUID walletId) {
            com.example.transaction.dto.TransactionInitResponse response = new com.example.transaction.dto.TransactionInitResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.valueOf(50));
            response.setFee(BigDecimal.valueOf(0.56));

            return response;
        }

        public static com.example.transaction.dto.TransactionInitResponse transferInitResponse(UUID sourceWalletId, UUID targetWalletId) {
            com.example.transaction.dto.TransactionInitResponse response = new com.example.transaction.dto.TransactionInitResponse();
            response.setWalletUid(sourceWalletId);
            response.setTargetWalletUid(targetWalletId);
            response.setAmount(BigDecimal.valueOf(100));
            response.setFee(BigDecimal.valueOf(7.62));

            return response;
        }

        public static com.example.transaction.dto.TransactionConfirmRequest depositConfirmRequest(UUID userId, UUID walletId) {
            com.example.transaction.dto.DepositConfirmRequest request = new com.example.transaction.dto.DepositConfirmRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType(DEPOSIT_TYPE);
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            request.setComment("confirm comment");
            return request;
        }

        public static com.example.transaction.dto.TransactionConfirmRequest withdrawalConfirmRequest(UUID userId, UUID walletId) {
            com.example.transaction.dto.WithdrawalConfirmRequest request = new com.example.transaction.dto.WithdrawalConfirmRequest();
            request.setWalletUid(userId);
            request.setType(WITHDRAWAL_TYPE);
            request.setUserUid(walletId);
            request.setAmount(BigDecimal.valueOf(228));
            request.setComment("COM 1");

            return request;
        }

        public static com.example.transaction.dto.TransactionConfirmRequest transferConfirmRequest(UUID sourceUserId, UUID sourceWalletId, UUID targetUserId, UUID targetWalletId, BigDecimal rate) {
            com.example.transaction.dto.TransferConfirmRequest request = new com.example.transaction.dto.TransferConfirmRequest();
            request.setUserUid(sourceUserId);
            request.setWalletUid(sourceWalletId);
            request.setTargetUserUid(targetUserId);
            request.setTargetWalletUid(targetWalletId);
            request.setAmount(BigDecimal.valueOf(1000.05));
            request.setType(TRANSFER_TYPE);
            request.setRate(rate);
            return request;
        }


        public static com.example.transaction.dto.TransactionConfirmResponse depositConfirmResponse(UUID userId, UUID walletId) {
            com.example.transaction.dto.TransactionConfirmResponse response = new com.example.transaction.dto.TransactionConfirmResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN);
            response.setFee(BigDecimal.valueOf(1.1d));
            response.setStatus("PENDING");
            response.setComment("confirm comment");
            response.setType(DEPOSIT_TYPE);
            response.setUserUid(userId);
            response.setWalletUid(walletId);

            return response;
        }

        public static com.example.transaction.dto.TransactionConfirmResponse withdrawalConfirmResponse(UUID userId, UUID walletId) {
            com.example.transaction.dto.TransactionConfirmResponse response = new com.example.transaction.dto.TransactionConfirmResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.valueOf(228));
            response.setFee(BigDecimal.valueOf(1.1d));
            response.setStatus("SUCCESS");
            response.setComment("confirm comment");
            response.setType(DEPOSIT_TYPE);
            response.setUserUid(userId);

            return response;
        }

        public static com.example.transaction.dto.TransactionConfirmResponse transferConfirmResponse(UUID sourceUserId, UUID sourceWalletId, UUID targetWalletId) {
            com.example.transaction.dto.TransactionConfirmResponse response = new com.example.transaction.dto.TransactionConfirmResponse();
            response.setUid(UUID.fromString("66666666-6666-6666-6666-666666666666"));
            response.setUserUid(sourceUserId);
            response.setWalletUid(sourceWalletId);
            response.setTargetWalletUid(targetWalletId);
            response.setAmount(BigDecimal.valueOf(1000.05));
            response.setFee(BigDecimal.valueOf(0.22));
            response.setStatus("CONFIRM");

            return response;
        }

        public static com.example.transaction.dto.TransactionFilterRequest transactionFilterRequest(UUID userId, UUID walletId) {
            com.example.transaction.dto.TransactionFilterRequest filter = new com.example.transaction.dto.TransactionFilterRequest();
            com.example.transaction.dto.Params params = new com.example.transaction.dto.Params();
            params.setStatus(com.example.transaction.dto.Params.StatusEnum.COMPLETED);
            params.setDateFrom(ZonedDateTime.parse("2025-04-04T00:00:00+00:00"));
            params.setDateTo(ZonedDateTime.parse("2025-05-05T00:00:00+00:00"));
            params.setUserUid(userId);
            params.setWalletUid(walletId);
            filter.setFilter(params);
            com.example.transaction.dto.Pageable pageable = new com.example.transaction.dto.Pageable();
            pageable.setPage(5);
            pageable.setSize(10);
            filter.setPageable(pageable);

            return filter;
        }

        public static com.example.transaction.dto.TransactionStatusResponse transactionStatusResponse(UUID transactionId) {
            com.example.transaction.dto.TransactionStatusResponse response = new com.example.transaction.dto.TransactionStatusResponse();
            response.setStatus("active");
            response.setUid(transactionId);
            response.setType("FAILED");
            response.setFailureReason("External service error");
            return response;
        }
    }


}
