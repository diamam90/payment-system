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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Transaction create(TransactionRequest request, Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        Transaction transaction = transactionMapper.create(request);
        transaction.setMerchant(merchant);
        log.debug("Транзакция на пополнение успешно создана, id: {}", transaction.getId());
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getById(Long id, Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        return transactionRepository.findByIdAndMerchantId(id, merchant.getId())
                .orElseThrow(() -> new ObjectNotFoundException("Transaction", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findBy(ZonedDateTime start, ZonedDateTime end, Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        return transactionRepository
                .findByMerchantIdAndCreatedAtBetween(merchant.getId(), start.toLocalDateTime(), end.toLocalDateTime());
    }
}
