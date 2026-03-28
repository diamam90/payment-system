package com.example.transactionservice.repository;

import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.entity.Transaction_;
import com.example.transactionservice.model.TransactionFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TransactionSpecification implements Specification<Transaction> {

    private final TransactionFilter filter;

    @Override
    public Predicate toPredicate(Root<Transaction> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        List<Predicate> predicateList = new ArrayList<>();

        if (filter.userUid() != null) {
            predicateList.add(cb.equal(root.get(Transaction_.USER_ID), filter.userUid()));
        }

        if (filter.walletUid() != null) {
            predicateList.add(cb.equal(root.get(Transaction_.WALLET_ID), filter.walletUid()));
        }

        if (filter.type() != null) {
            predicateList.add(cb.equal(root.get(Transaction_.TYPE), filter.type()));
        }

        if (filter.dateFrom() != null) {
            predicateList.add(cb.greaterThan(root.get(Transaction_.CREATED_AT), filter.dateFrom()));
        }

        if (filter.dateTo() != null) {
            predicateList.add(cb.lessThan(root.get(Transaction_.CREATED_AT), filter.dateTo()));
        }

        if (filter.status() != null) {
            predicateList.add(cb.equal(root.get(Transaction_.STATUS), filter.status()));
        }

        return cb.and(predicateList.toArray(new Predicate[0]));
    }
}
