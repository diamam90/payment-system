package com.example.transactionservice.repository;

import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.entity.TransactionStatus;
import com.example.transactionservice.entity.TransactionType;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.model.TransactionFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(DatabaseTestConfig.class)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private JdbcOperations jdbc;
    @MockitoBean
    JwtDecoder decoder;

    private static final String FIND_TRANSACTION_BY_ID = "SELECT * FROM transaction.transactions WHERE uid = ?";

    @Sql("/sql/save-transaction.sql")
    @Test
    void shouldSave() {
        Wallet wallet = walletRepository.findById(UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1")).orElseThrow();
        Wallet targetWAllet = walletRepository.findById(UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a2")).orElseThrow();
        var userId = wallet.getUserId();
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setUserId(userId);
        transaction.setAmount(new BigDecimal(100));
        transaction.setComment("На еду");
        transaction.setFee(new BigDecimal("2.28"));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setTargetWallet(targetWAllet);
        transaction.setCreatedAt(Instant.now());
        transactionRepository.saveAndFlush(transaction);

        Map<String, Object> params = jdbc.queryForMap(FIND_TRANSACTION_BY_ID, transaction.getId());
        assertThat(params)
                .containsEntry("uid", transaction.getId())
                .containsEntry("user_uid", transaction.getUserId())
                .containsEntry("wallet_uid", transaction.getWallet().getId())
                .containsEntry("amount", transaction.getAmount().setScale(2, RoundingMode.HALF_UP))
                .containsEntry("status", "COMPLETED");
    }

    @Sql("/sql/find-by-id-transaction.sql")
    @Test
    void shouldFindById() {
        //when
        Optional<Transaction> actual = transactionRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000007"));
        // then
        assertThat(actual).isPresent()
                .get()
                .hasFieldOrPropertyWithValue("id", UUID.fromString("00000000-0000-0000-0000-000000000007"))
                .hasFieldOrPropertyWithValue("userId", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .hasFieldOrPropertyWithValue("walletId", UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1"))
                .hasFieldOrPropertyWithValue("amount", new BigDecimal("2.00"))
                .hasFieldOrPropertyWithValue("type", TransactionType.DEPOSIT)
                .hasFieldOrPropertyWithValue("status", TransactionStatus.COMPLETED)
                .hasFieldOrPropertyWithValue("targetWalletId", UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e0033"))
                .hasFieldOrPropertyWithValue("createdAt", Instant.parse("2030-01-01T00:00:00+00:00"));
    }

    @Sql("/sql/find-transaction-by-status.sql")
    @Test
    void shouldFindByStatusFilter() {
        // given
        TransactionFilter filter = TransactionFilter.builder()
                .status(TransactionStatus.FAILED)
                .build();

        TransactionSpecification spec = new TransactionSpecification(filter);
        // when
        List<Transaction> actual = transactionRepository.findAll(spec);
        // then
        assertThat(actual).hasSize(2)
                .element(0)
                .hasFieldOrPropertyWithValue("id", UUID.fromString("00000000-0000-0000-0000-000000000008"));
    }

    @Sql("/sql/find-transaction-by-status-and-user-id.sql")
    @Test
    void shouldFindByStatusAndUserId() {
        // given
        TransactionFilter filter = TransactionFilter.builder()
                .status(TransactionStatus.FAILED)
                .userUid(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .build();

        TransactionSpecification spec = new TransactionSpecification(filter);
        // when
        List<Transaction> actual = transactionRepository.findAll(spec);
        // then
        assertThat(actual).hasSize(1)
                .element(0)
                .hasFieldOrPropertyWithValue("id", UUID.fromString("00000000-0000-0000-0000-000000000009"));
    }

    @Sql("/sql/find-transaction-by-user-id-and-date-between.sql")
    @Test
    void shouldFindByUserIdAndDateBetween() {
        // given
        TransactionFilter filter = TransactionFilter.builder()
                .userUid(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .dateFrom(ZonedDateTime.parse("2030-01-01T00:00:00+00:00"))
                .dateTo(ZonedDateTime.parse("2030-02-02T00:00:00+00:00"))
                .build();

        TransactionSpecification spec = new TransactionSpecification(filter);
        // when
        List<Transaction> actual = transactionRepository.findAll(spec);
        // then
        assertThat(actual).hasSize(2)
                .extracting("id").contains(
                        UUID.fromString("00000000-0000-0000-0000-000000000009"),
                        UUID.fromString("00000000-0000-0000-0000-000000000010")
                );
    }

    @Sql("/sql/find-transaction-all-filters.sql")
    @Test
    void shouldFindByAllFilters_shouldReturn3Transactions() {
        // given
        TransactionFilter filter = TransactionFilter.builder()
                .userUid(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .walletUid(UUID.fromString("fa65903f-2441-4ada-81fa-d8cd6a2e00a1"))
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.DEPOSIT)
                .dateFrom(ZonedDateTime.parse("2026-01-02T09:00:00+00:00"))
                .dateTo(ZonedDateTime.parse("2026-01-04T12:00:00+00:00"))
                .build();

        TransactionSpecification spec = new TransactionSpecification(filter);
        PageRequest firstPageSize3 = PageRequest.of(0, 3);
        PageRequest secondPageSize3 = PageRequest.of(1, 3);
        PageRequest firstPageSize2 = PageRequest.of(0, 2);
        PageRequest secondPageSize2 = PageRequest.of(1, 2);
        // when
        Page<Transaction> resultFirstPageSize3 = transactionRepository.findAll(spec, firstPageSize3);
        Page<Transaction> resultSecondPageSize3 = transactionRepository.findAll(spec, secondPageSize3);
        Page<Transaction> resultFirstPageSize2 = transactionRepository.findAll(spec, firstPageSize2);
        Page<Transaction> resultSecondPageSize2 = transactionRepository.findAll(spec, secondPageSize2);

        assertThat(resultFirstPageSize3).hasSize(3)
                .extracting("id").contains(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        UUID.fromString("00000000-0000-0000-0000-000000000004"),
                        UUID.fromString("00000000-0000-0000-0000-000000000007")
                );
        assertFalse(resultFirstPageSize3.hasNext());
        assertThat(resultSecondPageSize3).isEmpty();

        assertThat(resultFirstPageSize2).hasSize(2);
        assertTrue(resultFirstPageSize2.hasNext());
        assertThat(resultSecondPageSize2).hasSize(1);
    }
}