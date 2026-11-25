package com.example.repository;

import com.example.config.TestConfig;
import com.example.entity.Payment;
import com.example.entity.PaymentMethod;
import com.example.entity.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "logging.level.sql=debug")
public class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private MethodRepository methodRepository;
    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void clear() {
        jdbc.execute("DELETE FROM payment.payments");
        jdbc.execute("DELETE FROM payment.payment_method_definitions WHERE payment_method_id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_method_required_fields WHERE payment_method_id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_methods WHERE id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_providers WHERE id NOT IN (2)");
    }

    @Sql("/sql/same-external-transaction-id.sql")
    @Test
    void savePayment_whenExternalTransactionId_shouldThrowSqlException() {
        PaymentMethod paymentMethod = methodRepository.findActiveById(3).get();

        Payment payment = new Payment();
        payment.setInternalTransactionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(Status.SUCCESS);
        payment.setAmount(BigDecimal.valueOf(300.33));
        payment.setCurrency("USD");
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
