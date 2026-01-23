package com.example.transactionservice.service.impl;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.entity.WalletType;
import com.example.transactionservice.exception.ObjectNotFoundException;
import com.example.transactionservice.repository.WalletRepository;
import com.example.transactionservice.repository.WalletTypeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceImplTest {

    WalletRepository repository = Mockito.mock(WalletRepository.class);
    WalletTypeRepository walletTypeRepository = Mockito.mock(WalletTypeRepository.class);

    Clock fixedClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    Clock mockClock = Mockito.mock(Clock.class);

    ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

    WalletServiceImpl walletService = new WalletServiceImpl(repository, walletTypeRepository, mockClock);

    static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID WALLET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void shouldCreate() throws NoSuchFieldException, IllegalAccessException {
        // given
        CreateWalletRequest request = createStub();

        Field field = walletService.getClass().getDeclaredField("activeWalletYears");
        field.setAccessible(true);
        field.set(walletService, 2);
        field.setAccessible(false);

        //when
        when(walletTypeRepository.findById(WALLET_TYPE_ID)).thenReturn(Optional.of(walletTypeStub()));
        when(mockClock.instant()).thenReturn(fixedClock.instant());
        when(mockClock.getZone()).thenReturn(fixedClock.getZone());

        walletService.create(request);
        verify(repository).save(walletCaptor.capture());

        // then
        assertThat(walletCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(walletStub());
    }

    @Test
    void create_WhenWalletTypeNotFound_shouldThrowObjectNotFoundException() {
        // given
        CreateWalletRequest request = createStub();

        // then
        assertThatThrownBy(() -> walletService.create(request))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("WalletType with walletTypeId [%s] not found".formatted(WALLET_TYPE_ID));
    }

    @Test
    void shouldFindById() {
        // given
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        // when
        Wallet wallet = new Wallet();
        when(repository.findById(id)).thenReturn(Optional.of(wallet));
        Wallet actual = walletService.getById(id);
        // then
        assertThat(actual).isEqualTo(wallet);
    }

    @Test
    void findById_whenWalletNotFound_shouldThrowObjectNotFoundException() {
        // given
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        // then
        assertThatThrownBy(() -> walletService.getById(id))
                .isExactlyInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Wallet with Id [%s] not found".formatted(id));
    }

    private WalletType walletTypeStub() {
        WalletType type = new WalletType();
        type.setId(WALLET_TYPE_ID);
        type.setUserType("individual");
        type.setName("test wallet type name");
        type.setCurrencyCode("RUB");
        type.setArchivedAt(Instant.parse("2030-01-01T00:00:00Z"));

        return type;
    }

    private Wallet walletStub() {
        WalletType walletType = walletTypeStub();
        Wallet wallet = new Wallet();
        wallet.setType(walletType);
        wallet.setWalletTypeId(walletType.getId());
        wallet.setBalance(BigDecimal.valueOf(0.00));
        wallet.setName("Test wallet");
        wallet.setStatus("active");
        wallet.setUserId(USER_ID);
        wallet.setArchivedAt(Instant.parse("2027-01-01T00:00:00Z"));

        return wallet;
    }

    private CreateWalletRequest createStub() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setName("Test wallet");
        request.setUserUid(USER_ID);
        request.setWalletTypeUid(WALLET_TYPE_ID);
        return request;
    }

    @Test
    void shouldFindByUserId(){
        // given
        Wallet expected = walletStub();
        //when
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(expected));
        List<Wallet> actual = walletService.findByUserId(USER_ID);
        //then
        assertThat(actual).hasSize(1)
                .element(0)
                .isEqualTo(expected);
    }
}