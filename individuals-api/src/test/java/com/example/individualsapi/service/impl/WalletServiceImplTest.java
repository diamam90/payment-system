package com.example.individualsapi.service.impl;

import com.example.individuals.dto.CreateWalletRequest;
import com.example.individualsapi.mapper.WalletMapperImpl;
import com.example.individualsapi.mapper.WalletTypeMapperImpl;
import com.example.transaction.api.WalletApiClient;
import com.example.transaction.dto.WalletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.example.individualsapi.stub.WalletDtoStub.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        WalletServiceImpl.class,
        WalletMapperImpl.class,
        WalletTypeMapperImpl.class,
        ObjectMapper.class
})
class WalletServiceImplTest {

    @Autowired
    WalletServiceImpl walletService;
    @MockitoBean
    WalletApiClient client;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID WALLET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000066");
    private static final UUID WALLET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ZonedDateTime ARCHIVED_AT = ZonedDateTime.parse("2025-05-05T12:00:00+00:00");

    @Test
    void shouldCreate() {
        // given
        CreateWalletRequest request = createWalletRequest(USER_ID, WALLET_TYPE_ID);
        com.example.individuals.dto.WalletResponse expected = expectedWallet(USER_ID, WALLET_ID, WALLET_TYPE_ID, ARCHIVED_AT);
        // when
        com.example.transaction.dto.CreateWalletRequest transactionServiceRequest = new com.example.transaction.dto.CreateWalletRequest();
        transactionServiceRequest.setName("test wallet");
        transactionServiceRequest.setWalletTypeUid(WALLET_TYPE_ID);
        transactionServiceRequest.setUserUid(USER_ID);

        WalletResponse transactionServiceResponse = walletResponse(USER_ID, WALLET_ID, WALLET_TYPE_ID, ARCHIVED_AT);

        when(client.createWallet(transactionServiceRequest))
                .thenReturn(ResponseEntity.ok(transactionServiceResponse));
        // then
        StepVerifier.create(walletService.create(request))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void shouldGetById() {
        // given
        com.example.individuals.dto.WalletResponse expected = expectedWallet(USER_ID, WALLET_ID, WALLET_TYPE_ID, ARCHIVED_AT);
        // when
        when(client.getWalletById(WALLET_ID))
                .thenReturn(ResponseEntity.ok(walletResponse(USER_ID, WALLET_ID, WALLET_TYPE_ID, ARCHIVED_AT)));

        StepVerifier.create(walletService.getById(WALLET_ID)).expectNext(expected).verifyComplete();
    }

    @Test
    void getByUserId() {
        // given
        com.example.individuals.dto.WalletResponse expected = expectedWallet(USER_ID, WALLET_ID, WALLET_TYPE_ID, ARCHIVED_AT);
        // when
        when(client.getWalletByUserId(USER_ID))
                .thenReturn(ResponseEntity.ok(List.of(walletResponse(USER_ID, WALLET_ID, WALLET_TYPE_ID, ARCHIVED_AT))));
        StepVerifier.create(walletService.findByUserId(USER_ID)).expectNext(expected).verifyComplete();
    }
}