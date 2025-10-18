package com.example.personservice.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class JdbcUtils {

    private final JdbcTemplate jdbc;

    private static final String INDIVIDUAL_BY_INDIVIDUAL_ID = "SELECT * FROM person.individuals WHERE individual_id = ?";
    private static final String INDIVIDUAL_BY_PASSPORT = "SELECT * FROM person.individuals WHERE passport_number = ?";
    private static final String USER_BY_USER_ID = "SELECT * FROM person.users WHERE user_id = ?";
    private static final String ADDRESS_BY_ADDRESS_ID = "SELECT * FROM person.addresses WHERE address_id = ?";
    private static final String INDIVIDUAL_ID_BY_PASSPORT = "SELECT individual_id FROM person.individuals WHERE passport_number = ?";
    private static final String INDIVIDUAL_ID_BY_EMAIL = "SELECT i.individual_id FROM person.individuals i, person.users u WHERE u.email = ?";

    private static final String IDS_BY_INDIVIDUAL_ID = """
            SELECT i.individual_id, a.address_id, u.user_id
            FROM person.individuals i, person.users u, person.addresses a
            WHERE i.user_id = u.user_id AND u.address_id = a.address_id AND i.individual_id = ?
            """;

    public UUID getIndividualIdByPassport(String passportNumber) {
        return jdbc.queryForObject(INDIVIDUAL_ID_BY_PASSPORT, UUID.class, passportNumber);
    }

    public UUID getIndividualIdByEmail(String email) {
        return jdbc.queryForObject(INDIVIDUAL_ID_BY_EMAIL, UUID.class, email);
    }

    public Boolean existsIndividualById(UUID individualId) {
        return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM person.individuals WHERE individual_id = ?)", Boolean.class, individualId);
    }

    public Map<String, Object> getIndividualParamsByPassport(String passportNumber) {
        return jdbc.queryForMap(INDIVIDUAL_BY_PASSPORT, passportNumber);
    }

    public Map<String, Object> getIndividualParams(UUID individualId) {
        return getRowParamsById(INDIVIDUAL_BY_INDIVIDUAL_ID, individualId);
    }

    public Map<String, Object> getAddressParams(UUID addressId) {
        return getRowParamsById(ADDRESS_BY_ADDRESS_ID, addressId);
    }

    public Map<String, Object> getUserParams(UUID userId) {
        return getRowParamsById(USER_BY_USER_ID, userId);
    }

    public Map<String, Object> getIndividualUserAddressIdsByIndividualId(UUID individualId) {
        return jdbc.queryForMap(IDS_BY_INDIVIDUAL_ID, individualId);
    }

    public void truncateCascadeTable(String table) {
        jdbc.execute("Truncate table %s cascade".formatted(table));
    }

    private Map<String, Object> getRowParamsById(String entityQuery, Object id) {
        return jdbc.queryForMap(entityQuery, id);
    }


    public JdbcUtils(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
