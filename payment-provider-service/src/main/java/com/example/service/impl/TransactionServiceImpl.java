package com.example.service.impl;

import com.example.entity.Merchant;
import com.example.entity.Transaction;
import com.example.exception.ObjectNotFoundException;
import com.example.fake.dto.TransactionRequest;
import com.example.mapper.TransactionMapper;
import com.example.repository.TransactionRepository;
import com.example.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Transaction create(TransactionRequest request, Merchant merchant) {
        Transaction transaction = transactionMapper.create(request);
        transaction.setMerchant(merchant);
        log.debug("Транзакция на пополнение успешно создана, id: {}", transaction.getId());
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getById(Long id, Integer merchantId) {
        return transactionRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new ObjectNotFoundException("Transaction", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findBy(ZonedDateTime start, ZonedDateTime end, Integer merchantId) {
        return transactionRepository
                .findByMerchantIdAndCreatedAtBetween(merchantId, start.toLocalDateTime(), end.toLocalDateTime());
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }
}
