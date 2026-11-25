package com.example.repository;

import com.example.entity.Merchant;
import com.example.entity.Status;
import com.example.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private MerchantRepository merchantRepository;

    @Sql("/sql/transaction-with-external-id.sql")
    @Test
    void saveTransaction_whenExternalIdAlreadyExists_shouldReturnException() {
        Merchant merchant = merchantRepository.findByMerchantId("merchant1").get();
        Transaction transaction = new Transaction();
        transaction.setExternalId("external id 228");
        transaction.setAmount(BigDecimal.TEN);
        transaction.setStatus(Status.PENDING);
        transaction.setMerchant(merchant);
        assertThatThrownBy(() -> transactionRepository.save(transaction))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
