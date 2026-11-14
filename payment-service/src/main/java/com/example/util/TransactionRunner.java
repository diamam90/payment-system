package com.example.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionRunner {

    @Transactional(propagation = Propagation.REQUIRED)
    public void requiredWithoutResult(Runnable runnable) {
        runnable.run();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T requiredWithResult(Supplier<T> supplier) {
        return supplier.get();
    }
}
