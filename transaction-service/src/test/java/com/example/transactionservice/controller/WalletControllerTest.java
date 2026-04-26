package com.example.transactionservice.controller;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.dto.WalletTypeResponse;
import com.example.transactionservice.config.SecurityConfig;
import com.example.transactionservice.config.SecurityTestConfig;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.exception.ObjectNotFoundException;
import com.example.transactionservice.mapper.WalletMapper;
import com.example.transactionservice.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityTestConfig.class})
class WalletControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    WalletService walletService;
    @MockitoBean
    WalletMapper walletMapper;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID WALLET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID NOT_EXISTING_WALLET_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID WALLET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    @WithMockUser(authorities = "transaction_service_wr")
    void shouldCreateWallet() throws Exception {
        // given
        Wallet wallet = new Wallet();
        WalletResponse response = walletResponse();
        CreateWalletRequest request = createWalletRequest();
        // when
        when(walletService.create(request)).thenReturn(wallet);
        when(walletMapper.toResponse(wallet)).thenReturn(response);
        // then
        mvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWalletRequestStr()))
                .andExpectAll(
                        status().isCreated(),
                        content().json(walletResponseStr(), JsonCompareMode.STRICT)
                );
    }

    @ParameterizedTest
    @WithMockUser(authorities = "transaction_service_wr")
    @MethodSource("createWalletInvalidRequest")
    void createWallet_withInvalidRequest_shouldReturn400(String content) throws Exception {
        mvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "transaction_service_wr")
    void shouldGetWalletById() throws Exception {
        //given
        Wallet wallet = new Wallet();
        WalletResponse response = walletResponse();
        // when
        when(walletService.getById(WALLET_ID)).thenReturn(wallet);
        when(walletMapper.toResponse(wallet)).thenReturn(response);
        // then
        mvc.perform(get("/api/v1/wallets/{walletUid}", WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWalletRequestStr()))
                .andExpectAll(
                        status().isOk(),
                        content().json(walletResponseStr(), JsonCompareMode.STRICT)
                );
    }

    @Test
    @WithMockUser(authorities = "transaction_service_wr")
    void getWalletById_WhenWalletNotFound_ShouldReturn404() throws Exception {
        // when
        when(walletService.getById(NOT_EXISTING_WALLET_ID))
                .thenThrow(new ObjectNotFoundException("Wallet", "Id", NOT_EXISTING_WALLET_ID));
        // then
        mvc.perform(get("/api/v1/wallets/{walletUid}", NOT_EXISTING_WALLET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWalletRequestStr()))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(walletNotFoundResponse(), JsonCompareMode.STRICT)
                );
    }

    @Test
    @WithMockUser(authorities = "transaction_service_wr")
    void shouldGetWalletByUserId() throws Exception {
        // given
        Wallet wallet = new Wallet();
        WalletResponse response = walletResponse();
        // when
        when(walletService.findByUserId(USER_ID)).thenReturn(List.of(wallet));
        when(walletMapper.toResponse(wallet)).thenReturn(response);
        // then
        mvc.perform(get("/api/v1/wallets/user/{userUid}", USER_ID))
                .andExpectAll(
                        status().isOk(),
                        content().json(findByUserIdResponse(), JsonCompareMode.STRICT)
                );
    }


    private String walletResponseStr() {
        return //language=JSON
                """
                        {
                          "uid": "00000000-0000-0000-0000-000000000002",
                          "name": "my wallet",
                          "type": {
                            "uid": "00000000-0000-0000-0000-000000000003",
                            "name": "wallet type name",
                            "currencyCode": "RUB",
                            "status": "active",
                            "userType": "individual"
                          },
                          "userUid": "00000000-0000-0000-0000-000000000001",
                          "status": "active",
                          "balance": 0.00,
                          "archivedAt": "2030-05-05T12:12:12Z",
                          "createdAt": "2025-05-05T12:12:12Z",
                          "updatedAt": null
                        }
                        """;
    }

    private String findByUserIdResponse() {
        return //language=JSON
                """
                        [
                          {
                            "uid": "00000000-0000-0000-0000-000000000002",
                            "name": "my wallet",
                            "type": {
                              "uid": "00000000-0000-0000-0000-000000000003",
                              "name": "wallet type name",
                              "currencyCode": "RUB",
                              "status": "active",
                              "userType": "individual"
                            },
                            "userUid": "00000000-0000-0000-0000-000000000001",
                            "status": "active",
                            "balance": 0.00,
                            "archivedAt": "2030-05-05T12:12:12Z",
                            "createdAt": "2025-05-05T12:12:12Z",
                            "updatedAt": null
                          }
                        ]""";
    }

    private String createWalletRequestStr() {
        return //language=json
                """
                        {
                          "name": "my wallet",
                          "walletTypeUid": "%s",
                          "userUid": "%s"
                        }
                        """.formatted(WALLET_TYPE_ID, USER_ID);
    }

    private static List<String> createWalletInvalidRequest() {
        return List.of(createWalletWithoutUserUid(), createWalletWithoutWalletType(), createWalletWithoutName());
    }

    private static String createWalletWithoutUserUid() {
        return //language=json
                """
                        {
                          "name": "my wallet",
                          "walletTypeUid": "%s"
                        }
                        """.formatted(WALLET_TYPE_ID);
    }

    private static String createWalletWithoutName() {
        return //language=json
                """
                        {
                          "walletTypeUid": "%s",
                          "userUid": "%s"
                        }
                        """.formatted(WALLET_TYPE_ID, USER_ID);
    }

    private static String createWalletWithoutWalletType() {
        return //language=json
                """
                        {
                          "name": "my wallet",
                          "userUid": "%s"
                        }
                        """.formatted(USER_ID);
    }

    private WalletResponse walletResponse() {
        var response = new WalletResponse();
        response.setUid(WALLET_ID);
        response.setName("my wallet");
        response.setBalance(BigDecimal.valueOf(0.00));
        response.setCreatedAt(ZonedDateTime.parse("2025-05-05T12:12:12+00:00"));
        response.setArchivedAt(ZonedDateTime.parse("2030-05-05T12:12:12+00:00"));
        response.setStatus("active");
        response.setUserUid(USER_ID);
        response.setType(walletTypeResponse());

        return response;
    }

    private WalletTypeResponse walletTypeResponse() {
        var type = new WalletTypeResponse();
        type.setUid(WALLET_TYPE_ID);
        type.setName("wallet type name");
        type.setCurrencyCode("RUB");
        type.setStatus("active");
        type.setUserType("individual");

        return type;
    }

    private CreateWalletRequest createWalletRequest() {
        var request = new CreateWalletRequest();
        request.setUserUid(USER_ID);
        request.setName("my wallet");
        request.setWalletTypeUid(WALLET_TYPE_ID);

        return request;
    }

    private String walletNotFoundResponse() {
        return //JSON
                """
                        {
                            "status": 404,
                            "error": "Wallet with Id [%s] not found"
                        }
                        """.formatted(NOT_EXISTING_WALLET_ID);
    }
}