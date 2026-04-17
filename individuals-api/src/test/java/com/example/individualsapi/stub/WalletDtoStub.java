package com.example.individualsapi.stub;

import com.example.individuals.dto.CreateWalletRequest;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.dto.WalletTypeResponse;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public class WalletDtoStub {


    public static CreateWalletRequest createWalletRequest(UUID userId, UUID walletTypeId) {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setName("test wallet");
        request.setWalletTypeUid(walletTypeId);
        request.setUserUid(userId);
        return request;
    }

    public static com.example.individuals.dto.WalletResponse expectedWallet(UUID userId, UUID walletId, UUID walletTypeId, ZonedDateTime archivedAt) {
        com.example.individuals.dto.WalletResponse expected = new com.example.individuals.dto.WalletResponse();
        expected.setUserUid(userId);
        expected.setUid(walletId);
        expected.setBalance(BigDecimal.ZERO);
        expected.setName("wallet name");
        expected.setStatus("active");
        expected.setArchivedAt(archivedAt);

        com.example.individuals.dto.WalletTypeResponse expectedWalletType = new com.example.individuals.dto.WalletTypeResponse();
        expectedWalletType.setUid(walletTypeId);
        expectedWalletType.setName("test wallet type name");
        expectedWalletType.setStatus("ACTIVE");
        expected.setType(expectedWalletType);

        return expected;
    }

    public static WalletResponse walletResponse(UUID userId, UUID walletId, UUID walletTypeId, ZonedDateTime archivedAt) {
        WalletResponse response = new WalletResponse();
        response.setUid(walletId);
        response.setBalance(BigDecimal.ZERO);
        response.setStatus("active");
        response.setUserUid(userId);
        response.setName("wallet name");
        response.setArchivedAt(archivedAt);
        WalletTypeResponse walletTypeResponse = new WalletTypeResponse();
        walletTypeResponse.setUid(walletTypeId);
        walletTypeResponse.setStatus("ACTIVE");
        walletTypeResponse.setName("test wallet type name");
        response.setType(walletTypeResponse);

        return response;
    }

}
