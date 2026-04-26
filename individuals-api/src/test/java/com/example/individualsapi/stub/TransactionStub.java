package com.example.individualsapi.stub;

import com.example.individuals.dto.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public class TransactionStub {

    public static class IndividualService {

        public static DepositInitRequest depositInitRequest(UUID userId, UUID walletId) {
            DepositInitRequest request = new DepositInitRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType("super type");
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            return request;
        }

        public static TransactionInitResponse depositInitResponse(UUID walletId) {
            TransactionInitResponse response = new com.example.individuals.dto.TransactionInitResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN);
            response.setFee(BigDecimal.valueOf(1.1d));

            return response;
        }

        public static TransactionConfirmRequest depositConfirmRequest(UUID userId, UUID walletId) {
            DepositConfirmRequest request = new DepositConfirmRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType("super type");
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            request.setComment("confirm comment");
            return request;
        }

        public static TransactionConfirmResponse depositConfirmResponse(UUID userId, UUID walletId) {
            com.example.individuals.dto.TransactionConfirmResponse response = new com.example.individuals.dto.TransactionConfirmResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN);
            response.setFee(BigDecimal.valueOf(1.1d));
            response.setStatus("PENDING");
            response.setComment("confirm comment");
            response.setType("super type");
            response.setUserUid(userId);
            response.setWalletUid(walletId);

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
            request.setType("super type");
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            return request;
        }

        public static com.example.transaction.dto.TransactionInitResponse depositInitResponse(UUID walletId) {
            com.example.transaction.dto.TransactionInitResponse response = new com.example.transaction.dto.TransactionInitResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN);
            response.setFee(BigDecimal.valueOf(1.1d));

            return response;
        }

        public static com.example.transaction.dto.TransactionConfirmRequest depositConfirmRequest(UUID userId, UUID walletId) {
            com.example.transaction.dto.DepositConfirmRequest request = new com.example.transaction.dto.DepositConfirmRequest();
            request.setAmount(BigDecimal.TEN);
            request.setType("super type");
            request.setUserUid(userId);
            request.setWalletUid(walletId);
            request.setComment("confirm comment");
            return request;
        }

        public static com.example.transaction.dto.TransactionConfirmResponse depositConfirmResponse(UUID userId, UUID walletId) {
            com.example.transaction.dto.TransactionConfirmResponse response = new com.example.transaction.dto.TransactionConfirmResponse();
            response.setWalletUid(walletId);
            response.setAmount(BigDecimal.TEN);
            response.setFee(BigDecimal.valueOf(1.1d));
            response.setStatus("PENDING");
            response.setComment("confirm comment");
            response.setType("super type");
            response.setUserUid(userId);
            response.setWalletUid(walletId);

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
