package com.example.personservice.IT;

import com.example.person.dto.ErrorResponse;
import com.example.personservice.config.DatabaseConfig;
import com.example.personservice.config.SecurityTestConfig;
import com.example.personservice.entity.Status;
import com.example.personservice.util.JdbcUtils;
import com.example.personservice.util.KeycloakUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Import({DatabaseConfig.class, SecurityTestConfig.class})
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PrivateIndividualControllerIT {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    JdbcUtils jdbc;
    @Autowired
    SecurityTestConfig securityConfig;

    @AfterEach
    void truncate() {
        jdbc.truncateCascadeTable("person.individuals");
    }

    @Sql("/query/individual/lastnamov.sql")
    @Test
    void shouldDeleteById() {
        // given
        var id = jdbc.getIndividualIdByPassport("1331 4429");

        var adminToken = KeycloakUtils.adminToken(securityConfig.getKeycloakServerUrl()).getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        // when
        ResponseEntity<Void> exchange = restTemplate.exchange("/private/api/v1/individuals/{id}", HttpMethod.DELETE,
                new HttpEntity<>(null, headers), Void.class, id);
        var exists = jdbc.existsIndividualById(id);
        // then
        assertEquals(HttpStatus.NO_CONTENT, exchange.getStatusCode());
        assertFalse(exists);
    }


    @Sql("/query/individual/lastnamov.sql")
    @Test
    void shouldActivateIndividualById() {
        // given
        var id = jdbc.getIndividualIdByPassport("1331 4429");
        var adminToken = KeycloakUtils.adminToken(securityConfig.getKeycloakServerUrl()).getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        // when
        ResponseEntity<Void> exchange = restTemplate.exchange("/private/api/v1/individuals/{id}",
                HttpMethod.POST, new HttpEntity<>(null, headers), Void.class, id);
        var params = jdbc.getIndividualParams(id);
        // then
        assertEquals(HttpStatus.OK, exchange.getStatusCode());
        assertEquals(Status.ACTIVE.getStatusCode(), params.get("status"));
    }

    @Test
    void activateIndividualById_WhenIndividualIsAbsent_shouldReturn404() {
        // given
        var individualId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        var adminToken = KeycloakUtils.adminToken(securityConfig.getKeycloakServerUrl()).getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        // when
        ResponseEntity<ErrorResponse> exchange = restTemplate.exchange("/private/api/v1/individuals/{id}",
                HttpMethod.POST, new HttpEntity<>(null, headers), ErrorResponse.class, individualId);

        // then
        assertEquals(HttpStatus.NOT_FOUND, exchange.getStatusCode());
        assertThat(exchange.getBody())
                .hasFieldOrPropertyWithValue("status", 404)
                .hasFieldOrPropertyWithValue("error", "Individual with id [%s] not found".formatted(individualId));
    }
}

