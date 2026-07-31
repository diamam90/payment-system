package com.example.transactionservice.service.impl;

import com.example.transaction.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.entity.TransactionType;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.exception.BadRequestException;
import com.example.transactionservice.exception.ObjectNotFoundException;
import com.example.transactionservice.mapper.TransactionMapper;
import com.example.transactionservice.model.TransactionFilter;
import com.example.transactionservice.model.kafka.TransactionCompletedEvent;
import com.example.transactionservice.model.kafka.TransactionCreatedEvent;
import com.example.transactionservice.repository.TransactionRepository;
import com.example.transactionservice.repository.TransactionSpecification;
import com.example.transactionservice.service.TransactionService;
import com.example.transactionservice.service.WalletService;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.math.RoundingMode.HALF_EVEN;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafka;

    private final TransactionMapper transactionMapper;
    private final Clock clock;

    @Value("${transaction-service.kafka.topic.out}")
    private String topic;

    @Counted("deposit_init")
    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse depositInit(DepositInitRequest request) {
        UUID walletUid = request.getWalletUid();
        BigDecimal amount = request.getAmount().setScale(2, HALF_EVEN);
        Wallet wallet = walletService.getByIdAndUserId(walletUid, request.getUserUid());
        checkWalletStatus(wallet, TransactionType.DEPOSIT);
        BigDecimal fee = calculateFee(amount, TransactionType.DEPOSIT);

        var response = new TransactionInitResponse();
        response.setWalletUid(walletUid);
        response.setAmount(amount);
        response.setFee(fee);

        return response;
    }

    @Counted("transfer_init")
    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse transferInit(TransferInitRequest request) {
        UUID walletId = request.getWalletUid();
        Wallet wallet = walletService.getByIdAndUserId(walletId, request.getUserUid());
        checkWalletStatus(wallet, TransactionType.TRANSFER);

        UUID targetWalletId = request.getTargetWalletUid();
        Wallet targetWallet = walletService.getByIdAndUserId(targetWalletId, request.getTargetUserUid());
        checkWalletStatus(targetWallet, TransactionType.TRANSFER);

        BigDecimal amount = request.getAmount().setScale(2, HALF_EVEN);
        BigDecimal fee = calculateTransferFee(request.getRate(), amount, TransactionType.TRANSFER);
        BigDecimal accrual = amount.subtract(fee);
        validateTransfer(wallet, amount);

        var response = new TransactionInitResponse();
        response.setWalletUid(walletId);
        response.setTargetWalletUid(targetWalletId);
        response.setAmount(accrual);
        response.setFee(fee);
        return response;
    }

    @Counted("withdrawal_init")
    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse withdrawalInit(WithdrawalInitRequest request) {
        UUID walletId = request.getWalletUid();
        Wallet wallet = walletService.getByIdAndUserId(walletId, request.getUserUid());
        checkWalletStatus(wallet, TransactionType.WITHDRAWAL);

        BigDecimal amount = request.getAmount().setScale(2, HALF_EVEN);
        BigDecimal fee = calculateFee(amount, TransactionType.WITHDRAWAL);
        BigDecimal total = amount.add(fee);

        validateTransfer(wallet, total);

        var response = new TransactionInitResponse();
        response.setWalletUid(walletId);
        response.setFee(fee);
        response.setAmount(total);

        return response;
    }

    @Counted("deposit_confirm")
    @Override
    public TransactionConfirmResponse depositConfirm(DepositConfirmRequest request) {
        Wallet wallet = walletService.getByIdAndUserId(request.getWalletUid(), request.getUserUid());
        checkWalletStatus(wallet, TransactionType.DEPOSIT);

        BigDecimal amount = request.getAmount().setScale(2, HALF_EVEN);
        BigDecimal fee = calculateFee(amount, TransactionType.DEPOSIT);
        BigDecimal accrual = amount.subtract(fee);

        Transaction transaction = deposit(wallet, accrual, fee, request.getComment());
        transactionRepository.save(transaction);

        TransactionCreatedEvent transactionRequest = createMessage(transaction, TransactionType.DEPOSIT);

        kafka.send(topic, transaction.getId().toString(), transactionRequest);
        log.info("Transaction with Id {} has been successfully saved", transaction.getId());
        log.debug("Transaction {} for userId {} ", transaction, request.getUserUid());
        log.debug("{} sent to kafka", transactionRequest);

        return transactionMapper.toResponse(transaction);
    }

    @Counted("transfer_confirm")
    @Override
    public TransactionConfirmResponse transferConfirm(TransferConfirmRequest request) {
        Wallet wallet = walletService.getByIdAndUserId(request.getWalletUid(), request.getUserUid());
        checkWalletStatus(wallet, TransactionType.TRANSFER);

        UUID targetWalletId = request.getTargetWalletUid();
        Wallet targetWallet = walletService.getByIdAndUserId(targetWalletId, request.getTargetUserUid());
        checkWalletStatus(targetWallet, TransactionType.TRANSFER);

        BigDecimal amount = request.getAmount();
        BigDecimal fee = calculateTransferFee(request.getRate(), amount, TransactionType.TRANSFER);
        BigDecimal accrual = amount.subtract(fee);
        validateTransfer(wallet, amount);

        wallet.decreaseBalance(amount);
        targetWallet.increaseBalance(accrual);

        Transaction transaction = transfer(wallet, targetWallet, accrual, fee, request.getComment());

        transactionRepository.save(transaction);
        log.info("Transaction with Id {} has been successfully saved", transaction.getId());
        log.debug("Transaction {} for userId {} ", transaction, request.getUserUid());

        return transactionMapper.toResponse(transaction);
    }

    @Counted("withdrawal_confirm")
    @Override
    public TransactionConfirmResponse withdrawalConfirm(WithdrawalConfirmRequest request) {
        Wallet wallet = walletService.getByIdAndUserId(request.getWalletUid(), request.getUserUid());
        checkWalletStatus(wallet, TransactionType.WITHDRAWAL);

        BigDecimal amount = request.getAmount().setScale(2, HALF_EVEN);
        BigDecimal fee = calculateFee(amount, TransactionType.WITHDRAWAL);
        BigDecimal total = amount.add(fee);
        validateTransfer(wallet, total);

        wallet.decreaseBalance(total);
        Transaction transaction = withdrawal(wallet, amount, fee, request.getComment());
        transactionRepository.save(transaction);

        TransactionCreatedEvent transactionRequest = createMessage(transaction, TransactionType.WITHDRAWAL);
        kafka.send(topic, transaction.getId().toString(), transactionRequest);

        log.info("Transaction with Id {} has been successfully saved", transaction.getId());
        log.debug("Transaction {} for userId {} ", transaction, request.getUserUid());
        log.debug("{} sent to kafka", transactionRequest);

        return transactionMapper.toResponse(transaction);
    }

    @Counted("complete")
    @Override
    public void complete(TransactionCompletedEvent event) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(event.transactionId());
        if (optionalTransaction.isEmpty()) {
            log.info("Transaction with Id {} not found", event.transactionId());
            return;
        }

        Transaction transaction = optionalTransaction.get();
        if (transaction.isFinished()) {
            log.info("Transaction with Id {} has finished status", transaction.getId());
            return;
        }

        Wallet wallet = transaction.getWallet();
        transaction.setStatus(TransactionStatus.COMPLETED);
        log.info("Transaction with id {} completed", transaction.getId());
        if (TransactionType.DEPOSIT.equals(transaction.getType())) {
            BigDecimal accrual = transaction.getAmount().setScale(2, HALF_EVEN);
            wallet.increaseBalance(accrual);
            log.debug("Wallet [id={}] balance successfully increased, amount: {}, transaction: {}", wallet.getId(), accrual, transaction);
        }
    }

    @Counted("fail")
    @Override
    public void fail(TransactionCompletedEvent event) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(event.transactionId());
        if (optionalTransaction.isEmpty()) {
            log.info("Transaction with Id {} not found", event.transactionId());
            return;
        }

        Transaction transaction = optionalTransaction.get();
        if (transaction.isFinished()) {
            log.info("Transaction with Id {} has finished status", transaction.getId());
            return;
        }
        Wallet wallet = transaction.getWallet();
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(event.failureReason());
        log.info("Transaction with id {} failed", transaction.getId());
        if (TransactionType.WITHDRAWAL.equals(transaction.getType())) {
            BigDecimal amount = transaction.getAmount().setScale(2, HALF_EVEN);
            BigDecimal fee = transaction.getFee();
            wallet.increaseBalance(amount.add(fee));
            log.debug("Wallet [id={}] balance successfully increased, amount: {}, transaction: {}", wallet.getId(), amount, transaction);
        }
    }

    @Override
    public Page<Transaction> findBy(TransactionFilter filter, Pageable pageable) {
        Specification<Transaction> specification = new TransactionSpecification(filter);
        return transactionRepository.findAll(specification, pageable);
    }

    @Override
    public Transaction findById(UUID transactionId) {
        return transactionRepository.findById(transactionId).orElseThrow(() -> new ObjectNotFoundException("Transaction", transactionId));
    }

    private void checkWalletStatus(Wallet wallet, TransactionType type) {
        if (wallet.isInactive()) {
            throw new BadRequestException("Wallet with Uid  [%s] is not available for %s operation".formatted(wallet.getId(), type));
        }
    }

    private void validateTransfer(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("There is not enough money in the wallet with id [%s].".formatted(wallet.getId()));
        }
    }

    private BigDecimal calculateFee(BigDecimal amount, TransactionType type) {
        return amount
                .multiply(getTransactionTypePart().apply(type))
                .setScale(2, HALF_EVEN);
    }

    private BigDecimal calculateTransferFee(BigDecimal rate, BigDecimal amount, TransactionType type) {
        return amount.multiply(rate)
                .multiply(getTransactionTypePart().apply(type))
                .setScale(2, HALF_EVEN);
    }

    private Function<TransactionType, BigDecimal> getTransactionTypePart() {
        return (type) -> switch (type) {
            case TransactionType.DEPOSIT -> BigDecimal.valueOf(0.01);
            case TransactionType.TRANSFER -> BigDecimal.valueOf(0.001);
            case TransactionType.WITHDRAWAL -> BigDecimal.valueOf(0.02);
        };
    }

    private Transaction withdrawal(Wallet wallet, BigDecimal amount, BigDecimal fee, String comment) {
        Transaction transaction = new Transaction();
        transaction.setUserId(wallet.getUserId());
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setComment(comment);

        return transaction;
    }

    private Transaction transfer(Wallet wallet, Wallet targetWallet, BigDecimal accrual, BigDecimal fee, String comment) {
        Transaction transaction = new Transaction();
        transaction.setUserId(wallet.getUserId());
        transaction.setWallet(wallet);
        transaction.setTargetWallet(targetWallet);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmount(accrual);
        transaction.setFee(fee);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setComment(comment);

        return transaction;
    }

    private Transaction deposit(Wallet wallet, BigDecimal amount, BigDecimal fee, String comment) {
        Transaction transaction = new Transaction();
        transaction.setUserId(wallet.getUserId());
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setComment(comment);

        return transaction;
    }

    private TransactionCreatedEvent createMessage(Transaction transaction, TransactionType type) {
        return TransactionCreatedEvent
                .builder()
                .transactionId(transaction.getId())
                .userId(transaction.getWallet().getUserId())
                .walletId(transaction.getWallet().getId())
                .currency(transaction.getWallet().getType().getCurrencyCode())
                .amount(transaction.getAmount())
                .timestamp(ZonedDateTime.now(clock))
                .type(type.name())
                .build();
    }
}
