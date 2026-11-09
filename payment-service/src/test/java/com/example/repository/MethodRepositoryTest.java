package com.example.repository;

import com.example.config.TestConfig;
import com.example.entity.PaymentMethod;
import com.example.entity.PaymentType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestConfig.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "logging.level.sql=debug")
class MethodRepositoryTest {

    @Autowired
    private MethodRepository methodRepository;
    @Autowired
    JdbcTemplate jdbc;

    private static final String TRUNCATE_PAYMENTS_QUERY = "TRUNCATE payment.payments";
    private static final String TRUNCATE_PAYMENTS_PROVIDER = "TRUNCATE payment.payment_providers CASCADE";

    @AfterEach
    void clear() {
        jdbc.execute(TRUNCATE_PAYMENTS_QUERY);
        jdbc.execute(TRUNCATE_PAYMENTS_PROVIDER);
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void findByCurrencyAndCountryCodes_whenCurrencyAndCountryIsPresent_shouldReturnPaymentMethod() {
        String currency = "RUB", country = "USA";

        List<PaymentMethod> result = methodRepository.findByCurrencyAndCountryAndType(currency, country, PaymentType.PAYOUT);

        assertThat(result)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("providerMethodType", "provider method type 1")
                .hasFieldOrPropertyWithValue("paymentMethodDefinition.currencyCode", "RUB")
                .hasFieldOrPropertyWithValue("paymentMethodDefinition.countryAlpha3Code", "RUS")
                .extracting("paymentMethodRequiredFields", InstanceOfAssertFactories.LIST).hasSize(2)
                .extracting("paymentType", "dataType")
                .contains(Tuple.tuple("some payment type 1", "data type 1"), Tuple.tuple("some payment type 2", "data type 2"));
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void findById() {
        Optional<PaymentMethod> result = methodRepository.findById(1);

        assertThat(result)
                .isPresent()
                .get()
                .hasFieldOrPropertyWithValue("id", 1)
                .hasFieldOrPropertyWithValue("providerMethodType", "provider method type 1");
    }
}