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

    @AfterEach
    void clear() {
        jdbc.execute("DELETE FROM payment.payment_method_definitions WHERE payment_method_id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_method_required_fields WHERE payment_method_id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_methods WHERE id NOT IN (10,11)");
        jdbc.execute("DELETE FROM payment.payment_providers WHERE id NOT IN (2)");
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void findByCurrencyAndCountryAndType_whenCountryNotEqualsButIsAllCountriesIsTrue_shouldReturnPaymentMethod() {
        String currency = "JPY", country = "USA";

        List<PaymentMethod> result = methodRepository.findByCurrencyAndCountryAndType(currency, country, PaymentType.PAYOUT);

        assertThat(result)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("providerMethodType", "provider method type 1")
                .hasFieldOrPropertyWithValue("paymentMethodDefinition.currencyCode", currency)
                .hasFieldOrPropertyWithValue("paymentMethodDefinition.countryAlpha3Code", "RUS")
                .extracting("paymentMethodRequiredFields", InstanceOfAssertFactories.LIST).hasSize(2)
                .extracting("paymentType", "dataType")
                .contains(Tuple.tuple("some payment type 1", "data type 1"), Tuple.tuple("some payment type 2", "data type 2"));
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void findByCurrencyAndCountry_whenCountryIsNull_shouldReturnPaymentMethodWithAllCountryIsTrue() {
        String currency = "JPY", country = null;

        List<PaymentMethod> result = methodRepository.findByCurrencyAndCountry(currency, country);
        assertThat(result)
                .hasSize(1)
                .extracting("id")
                .containsOnly(1);
    }

    @Sql("/sql/find-by-currency-and-country-codes.sql")
    @Test
    void findByCurrencyAndCountry_whetIsAllCountriesITrue_shouldReturnPaymentMethod() {
        String currency = "JPY", country = "AAA";

        List<PaymentMethod> result = methodRepository.findByCurrencyAndCountry(currency, country);
        assertThat(result)
                .hasSize(2)
                .extracting("id")
                .containsOnly(1, 6);
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