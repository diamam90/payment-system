package com.example.transactionservice.repository;

import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.entity.Wallet;
import com.example.transactionservice.entity.WalletType;
import com.example.transactionservice.stub.WalletTypeStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(DatabaseTestConfig.class)
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTypeRepository walletTypeRepository;
    @Autowired
    private JdbcOperations jdbc;
    @MockitoBean
    JwtDecoder decoder;

    private static final String FIND_WALLET_BY_UID = "SELECT * FROM transaction.wallets WHERE uid = ?";
    private static final String FIND_UUID_WALLET_TYPE = "SELECT uid FROM transaction.wallet_types WHERE currency_code = 'RUB'";

    private static final String INSERT_WALLET_QUERY =
            // language=SQL
            """
                    INSERT INTO transaction.wallets (name, user_uid, wallet_type_uid, status, balance, archived_at, uid)
                    VALUES (?, ? ::uuid, ? ::uuid, ?, ?, ?, ? ::uuid)
                    """;

    @Test
    void shouldSaveWallet() {
        WalletType walletType = walletTypeRepository.findByCurrencyCode("RUB").getFirst();

        Wallet wallet = new Wallet();
        wallet.setName("KAMIKAZE");
        wallet.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        wallet.setStatus("active");
        wallet.setBalance(BigDecimal.ONE);
        wallet.setType(walletType);
        wallet.setCreatedAt(Instant.now());
        Wallet savedWallet = walletRepository.save(wallet);

        Map<String, Object> params = jdbc.queryForMap(FIND_WALLET_BY_UID, savedWallet.getId());
        assertThat(params)
                .containsEntry("name", wallet.getName());
    }

    @Test
    void shouldFindByUid() {
        // given
        UUID walletTypeUid = jdbc.queryForObject(FIND_UUID_WALLET_TYPE, UUID.class);
        assertNotNull(walletTypeUid);
        UUID userUid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var walletUid = UUID.randomUUID();
        Wallet wallet = createWallet(walletTypeUid, userUid);
        wallet.setId(walletUid);

        PreparedStatementCreator psc = preparedStatementCreator(wallet);
        jdbc.update(psc);

        // when
        Optional<Wallet> actual = walletRepository.findById(walletUid);
        // then
        assertThat(actual).isPresent()
                .get()
                .usingRecursiveComparison().ignoringFields("id", "transactions", "targetTransactions", "createdAt", "type.createdAt", "type.wallets")
                .isEqualTo(wallet);
    }

    @Sql(scripts = "/sql/find-by-user-uid-case.sql")
    @Test
    void shouldFindByUserId() {
        List<String> walletNames = List.of("my wallet 1", "my wallet 2", "my wallet 3", "my wallet 4", "my wallet 5");
        UUID userUid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<Wallet> actual = walletRepository.findByUserId(userUid);
        assertThat(actual).hasSize(5)
                .extracting("name").isEqualTo(walletNames);
    }

    private Wallet createWallet(UUID walletTypeUid, UUID userUid) {
        Wallet wallet = new Wallet();
        wallet.setName("my wallet");
        WalletType walletType = WalletTypeStub.rub_wallet();
        walletType.setId(walletTypeUid);
        wallet.setType(walletType);
        wallet.setUserId(userUid);
        wallet.setBalance(new BigDecimal("23.94"));
        wallet.setArchivedAt(Instant.parse("2025-05-05T12:00:00+03:00"));
        wallet.setStatus("active");

        return wallet;
    }

    private PreparedStatementCreator preparedStatementCreator(Wallet wallet) {
        return con -> {
            PreparedStatement preparedStatement = con.prepareStatement(INSERT_WALLET_QUERY);
            preparedStatement.setString(1, wallet.getName());
            preparedStatement.setObject(2, wallet.getUserId());
            preparedStatement.setObject(3, wallet.getType().getId());
            preparedStatement.setString(4, wallet.getStatus());
            preparedStatement.setBigDecimal(5, wallet.getBalance());
            preparedStatement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.ofInstant(wallet.getArchivedAt(), ZoneOffset.UTC)));
            preparedStatement.setObject(7, wallet.getId());
            return preparedStatement;
        };
    }
}