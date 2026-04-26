package com.example.transactionservice.repository;

import com.example.transactionservice.config.DatabaseTestConfig;
import com.example.transactionservice.entity.WalletType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.transactionservice.stub.WalletTypeStub.rub_wallet;
import static com.example.transactionservice.stub.WalletTypeStub.usd_wallet;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(DatabaseTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class WalletTypeRepositoryTest {

    @Autowired
    WalletTypeRepository repository;
    @Autowired
    JdbcOperations jdbc;
    @MockitoBean
    JwtDecoder decoder;

    private static final String RUB_CURRENCY_CODE = "RUB";
    private static final String USD_CURRENCY_CODE = "USD";

    private static final String FIND_UID_BY_CURRENCY_CODE_QUERY = "SELECT uid FROM transaction.wallet_types WHERE currency_code = ?";
    private static final String FIND_BY_UID_QUERY = "SELECT * FROM transaction.wallet_types WHERE uid = ?";

    @Test
    void findByCurrencyCode_ShouldReturnRubWalletType() {
        // given
        WalletType excepted = rub_wallet();
        // when
        List<WalletType> actual = repository.findByCurrencyCode(RUB_CURRENCY_CODE);
        // then
        assertThat(actual)
                .hasSize(1)
                .element(0)
                .usingRecursiveComparison().ignoringFields("id", "createdAt", "wallets")
                .isEqualTo(excepted);
    }

    @Test
    void findByCurrencyCode_ShouldReturnUSDWalletType() {
        // given
        WalletType type = usd_wallet();
        // when
        List<WalletType> actual = repository.findByCurrencyCode(USD_CURRENCY_CODE);
        // then
        assertThat(actual)
                .hasSize(1)
                .element(0)
                .usingRecursiveComparison().ignoringFields("id", "createdAt", "wallets")
                .isEqualTo(type);
    }

    @Test
    void findByUid_ShouldReturnWalletType() {
        // given
        UUID uid = jdbc.queryForObject(FIND_UID_BY_CURRENCY_CODE_QUERY, UUID.class, RUB_CURRENCY_CODE);
        UUID uid2 = jdbc.queryForObject(FIND_UID_BY_CURRENCY_CODE_QUERY, UUID.class, RUB_CURRENCY_CODE);
        UUID uid3 = jdbc.queryForObject(FIND_UID_BY_CURRENCY_CODE_QUERY, UUID.class, RUB_CURRENCY_CODE);
        UUID uid4 = jdbc.queryForObject(FIND_UID_BY_CURRENCY_CODE_QUERY, UUID.class, RUB_CURRENCY_CODE);
        UUID uid5 = jdbc.queryForObject(FIND_UID_BY_CURRENCY_CODE_QUERY, UUID.class, RUB_CURRENCY_CODE);
        assertThat(uid).isNotNull();
        WalletType type = rub_wallet();
        type.setId(uid);
        // when
        Optional<WalletType> actual = repository.findById(uid);
        // then
        assertThat(actual)
                .isPresent()
                .get()
                .usingRecursiveComparison().ignoringFields("createdAt", "wallets")
                .isEqualTo(type);
    }

    @Test
    void findByCurrencyCode_ShouldNotReturnAnyWalletType() {
        // given
        String currencyCode = "NOT EXISTING CURRENCY CODE";
        // when
        List<WalletType> actual = repository.findByCurrencyCode(currencyCode);
        // then
        assertThat(actual).hasSize(0);
    }

    @Test
    void findByUid_shouldNotReturnWalletType() {
        // given
        UUID uid = UUID.fromString("66666666-6666-6666-6666-666666666666");
        // when
        Optional<WalletType> actual = repository.findById(uid);
        // then
        assertThat(actual).isEmpty();
    }

    @Test
    void shouldAddNewWalletType() {
        // given
        WalletType type = new WalletType();
        type.setUserType("new user type");
        type.setName("new wallet");
        type.setStatus("active");
        type.setCurrencyCode("NEW");
        type.setArchivedAt(Instant.parse("2025-05-05T12:00:00+00:00"));
        type.setCreator("new admin");
        type.setCreatedAt(Instant.parse("2020-05-05T12:00:00+00:00"));
        // when
        WalletType savedWalletType = repository.saveAndFlush(type);
        // then
        Map<String, Object> params = jdbc.queryForMap(FIND_BY_UID_QUERY, savedWalletType.getId());

        assertThat(params)
                .containsEntry("name", type.getName())
                .containsEntry("status", type.getStatus())
                .containsEntry("user_type", type.getUserType())
                .containsEntry("creator", type.getCreator())
                .containsEntry("currency_code", type.getCurrencyCode())
                .containsEntry("archived_at", Timestamp.valueOf(LocalDateTime.ofInstant(Instant.parse("2025-05-05T12:00:00Z"), ZoneOffset.UTC)))
                .containsEntry("created_at", Timestamp.valueOf(LocalDateTime.ofInstant(Instant.parse("2020-05-05T12:00:00Z"), ZoneOffset.UTC)));
    }
}