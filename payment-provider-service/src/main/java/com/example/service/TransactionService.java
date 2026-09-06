package com.example.service;

import com.example.entity.Merchant;
import com.example.entity.Transaction;
import com.example.fake.dto.TransactionRequest;
import org.springframework.security.core.Authentication;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionService {

    Transaction create(TransactionRequest request, Merchant merchant);

    Transaction getById(Long id, Integer merchantId);

    Optional<Transaction> findById(Long id);

    List<Transaction> findBy(ZonedDateTime start, ZonedDateTime end, Integer merchantId);
}