package com.example.repository;

import com.example.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            SELECT p FROM Payment p WHERE p.id = :id
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdPessimisticWrite(Long id);
}
