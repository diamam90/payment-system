package com.example.transactionservice.service.impl;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.entity.WalletType;
import com.example.transactionservice.exception.ObjectNotFoundException;
import com.example.transactionservice.repository.WalletRepository;
import com.example.transactionservice.repository.WalletTypeRepository;
import com.example.transactionservice.service.WalletService;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTypeRepository walletTypeRepository;
    private final Clock clock;

    @Value("${activeWalletYears:2}")
    private Integer activeWalletYears;

    @Counted("create_wallet")
    @Override
    public Wallet create(CreateWalletRequest request) {
        WalletType walletType = walletTypeRepository.findById(request.getWalletTypeUid())
                .orElseThrow(() -> new ObjectNotFoundException("WalletType", "walletTypeId", request.getWalletTypeUid().toString()));

        Instant archivedAt = ZonedDateTime.now(clock).plusYears(activeWalletYears).toInstant();

        Wallet wallet = Wallet.builder()
                .name(request.getName())
                .userId(request.getUserUid())
                .type(walletType)
                .archivedAt(archivedAt)
                .status("active")
                .balance(BigDecimal.valueOf(0.00))
                .build();

        var result = walletRepository.save(wallet);
        log.debug("Wallet for user with UID {} successfully created", result.getUserId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getByIdAndUserId(UUID walletId, UUID userId) {
        return walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ObjectNotFoundException("Wallet", "id", walletId, "userId", userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ObjectNotFoundException("Wallet", walletId));

    }

    @Override
    @Transactional(readOnly = true)
    public List<Wallet> findByUserId(UUID id) {
        return walletRepository.findByUserId(id);
    }
}
