package com.example.personservice.repository;

import com.example.personservice.config.AppConfig;
import com.example.personservice.config.AppContainers;
import com.example.personservice.entity.Individual;
import com.example.personservice.stub.entity.IndividualStub;
import com.example.personservice.util.JdbcUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ImportTestcontainers(AppContainers.class)
@Import({AppConfig.class, JdbcUtils.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class IndividualRepositoryTest {

    @Autowired
    IndividualRepository individualRepository;

    @Autowired
    JdbcUtils jdbc;

    @Test
    void save() {
        var individual = IndividualStub.individualWithoutCountry();
        Individual save = individualRepository.save(individual);

        var individualParams = jdbc.getIndividualParams(save.getId());
        var userParams = jdbc.getUserParams(save.getUser().getId());
        var addressParams = jdbc.getAddressParams(save.getUser().getAddress().getId());

        assertThat(individualParams).isNotEmpty()
                .containsEntry("individual_id", save.getId())
                .containsEntry("passport_number", save.getPassportNumber())
                .containsEntry("phone_number", save.getPhoneNumber())
                .containsEntry("user_id", save.getUser().getId())
                // TODO zone?
                .containsEntry("verified_at", Timestamp.valueOf("2025-06-06 10:00:00.000"))
                .containsEntry("archived_at", Timestamp.valueOf("2025-05-05 10:00:00.000"));

        var user = save.getUser();
        assertThat(userParams).isNotEmpty()
                .containsEntry("user_id", user.getId())
                .containsEntry("secret_key", user.getSecretKey())
                .containsEntry("email", user.getEmail())
                .containsEntry("first_name", user.getFirstName())
                .containsEntry("last_name", user.getLastName())
                .containsEntry("address_id", user.getAddress().getId())
                .hasEntrySatisfying("created", Objects::nonNull)
                .hasEntrySatisfying("updated", Objects::nonNull);

        var address = user.getAddress();
        assertThat(addressParams).isNotEmpty()
                .containsEntry("address_id", address.getId())
                .containsEntry("zip_code", address.getZipCode())
                .containsEntry("archived", Timestamp.valueOf("2015-05-05 10:00:00.000"))
                .containsEntry("city", address.getCity())
                .containsEntry("state", address.getState())
                .hasEntrySatisfying("created", Objects::nonNull)
                .hasEntrySatisfying("updated", Objects::nonNull)
                .hasEntrySatisfying("country_id", Objects::isNull);
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void findByEmail() {
        var individual = IndividualStub.firstNamov();

        var result = individualRepository.findByEmail("email@email.email").get();

        assertThat(result.getUser())
                .hasNoNullFieldsOrPropertiesExcept("lastName", "filled")
                .usingRecursiveComparison()
                .comparingOnlyFields("firstName", "email", "secretKey")
                .isEqualTo(individual.getUser());

        assertThat(result.getUser().getAddress())
                .hasNoNullFieldsOrPropertiesExcept("state")
                .usingRecursiveComparison()
                .ignoringFields("state", "id", "created", "updated", "users", "country").isEqualTo(individual.getUser().getAddress());

        assertThat(result.getUser().getAddress().getCountry())
                .hasNoNullFieldsOrPropertiesExcept("status")
                .usingRecursiveComparison()
                .ignoringFields("id", "created", "updated").isEqualTo(individual.getUser().getAddress().getCountry());

        assertThat(result)
                .hasNoNullFieldsOrPropertiesExcept("status")
                .usingRecursiveComparison()
                .ignoringFields("id", "user")
                .isEqualTo(individual);
    }

    @Test
    void findByNotExistingEmailShouldReturnEmpty() {
        Optional<Individual> result = individualRepository.findByEmail("not_exist@email");
        assertTrue(result.isEmpty());
    }

    @Sql("/query/individual/firstnamov.sql")
    @Test
    void hardDelete() {
        var id = jdbc.getIndividualIdByPassport("1331 4429");

        // before
        var idMap = jdbc.getIndividualUserAddressIdsByIndividualId(id);
        assertThat(idMap)
                .hasEntrySatisfying("individual_id", Objects::nonNull)
                .hasEntrySatisfying("user_id", Objects::nonNull)
                .hasEntrySatisfying("address_id", Objects::nonNull);

        individualRepository.deleteById(id);

        // after
        idMap = jdbc.getIndividualUserAddressIdsByIndividualId(id);
        assertThat(idMap)
                .hasEntrySatisfying("individual_id", Objects::isNull)
                .hasEntrySatisfying("user_id", Objects::isNull)
                .hasEntrySatisfying("address_id", Objects::isNull);
    }
}