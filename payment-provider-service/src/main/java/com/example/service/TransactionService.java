package com.example.service;

import com.example.entity.Transaction;
import com.example.fake.dto.TransactionRequest;
import org.springframework.security.core.Authentication;

import java.time.ZonedDateTime;
import java.util.List;

public interface TransactionService {

    Transaction create(TransactionRequest request, Authentication auth);

    Transaction getById(Long id, Authentication auth);

    List<Transaction> findBy(ZonedDateTime start, ZonedDateTime end, Authentication auth);
}