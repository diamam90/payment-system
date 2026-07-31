package com.example.currencyrateservice.repository;

import com.example.currencyrateservice.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    @Query("""
            SELECT code
            FROM Currency
            WHERE active = true
            """)
    List<String> findAllCodeByActiveIsTrue();
}
