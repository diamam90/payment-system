package com.example.personservice.controller;

import com.example.personservice.config.SecurityConfig;
import com.example.personservice.entity.Individual;
import com.example.personservice.exception.ObjectNotFoundException;
import com.example.personservice.mapper.IndividualMapper;
import com.example.personservice.service.IndividualService;
import com.example.personservice.stub.entity.IndividualStub;
import com.example.personservice.stub.request.IndividualRequestStub;
import com.example.personservice.stub.response.IndividualResponseStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.example.personservice.util.JsonUtils.getDataFromFile;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebMvcTest(value = IndividualController.class)
@Import(SecurityConfig.class)
class IndividualControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    IndividualService individualService;
    @MockitoBean
    IndividualMapper mapper;

    private static final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final String email = "email@email.com";


    @WithMockUser
    @Test
    void findBy() throws Exception {
        var individual = IndividualResponseStub.individualResponse();
        when(mapper.toDto(null)).thenReturn(individual);
        mvc.perform(get("/api/v1/individuals?email={email}", email))
                .andExpectAll(
                        status().isOk(),
                        content().json(getDataFromFile("/responses/find-by-email-success.json"))
                );
        verify(individualService).findByEmail(email);
    }

    @WithMockUser
    @Test
    void findByEmail_whenIndividualNotExists_shouldReturn404() throws Exception {
        when(individualService.findByEmail(email))
                .thenThrow(new ObjectNotFoundException(Individual.class, "email", email));

        mvc.perform(get("/api/v1/individuals?email={email}", email))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(getDataFromFile("/responses/email-not-found-response.json"))
                );
        verify(individualService).findByEmail(email);
    }

    @Test
    void findByEmail_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/api/v1/individuals?email={email}", email))
                .andExpect(status().isUnauthorized());
        verify(individualService, never()).findByEmail(any());
    }

    @WithMockUser
    @Test
    void findById() throws Exception {
        var individual = IndividualResponseStub.individualResponse2();
        when(mapper.toDto(null)).thenReturn(individual);

        mvc.perform(get("/api/v1/individuals/{id}", id))
                .andExpectAll(
                        status().isOk(),
                        content().json(getDataFromFile("/responses/find-by-id-success.json"))
                );
        verify(individualService).findById(id);
    }

    @WithMockUser
    @Test
    void findById_whenIndividualNotExists_shouldReturn404() throws Exception {
        when(individualService.findById(id)).thenThrow(new ObjectNotFoundException(Individual.class, "id", id));

        mvc.perform(get("/api/v1/individuals/{id}", id))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(getDataFromFile("/responses/id-not-found-response.json"))
                );
    }

    @Test
    void findById_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/api/v1/individuals/{id}", id))
                .andExpect(status().isUnauthorized());
        verify(individualService, never()).findById(any());
    }

    @WithMockUser
    @Test
    void update() throws Exception {
        var request = IndividualRequestStub.update();
        var individual = IndividualStub.individual_1();

        when(individualService.update(id, request)).thenReturn(individual);
        when(mapper.toDto(individual)).thenReturn(IndividualResponseStub.individualResponse());

        mvc.perform(post("/api/v1/individuals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getDataFromFile("/request/update-individual.json")))
                .andExpectAll(status().isOk(),
                        content().json(getDataFromFile("/responses/update-response.json")));
    }

    @WithMockUser
    @Test
    void update_whenIndividualNotExists_shouldReturn404() throws Exception {
        var request = IndividualRequestStub.update();

        when(individualService.update(id, request)).thenThrow(new ObjectNotFoundException(Individual.class, "id", id));

        mvc.perform(post("/api/v1/individuals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getDataFromFile("/request/update-individual.json")))
                .andExpectAll(status().isNotFound(),
                        content().json(getDataFromFile("/responses/id-not-found-response.json")));
    }

    @Test
    void update_withoutAuth_shouldReturn401() throws Exception {

        mvc.perform(post("/api/v1/individuals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getDataFromFile("/request/update-individual.json")))
                .andExpect(status().isUnauthorized());
        verify(individualService, never()).update(any(), any());
    }

    @Test
    void create() throws Exception {
        var request = IndividualRequestStub.create();
        var individual = IndividualStub.individual_1();

        when(individualService.create(request)).thenReturn(individual);
        when(mapper.toDto(individual)).thenReturn(IndividualResponseStub.individualResponse());

        mvc.perform(post("/api/v1/individuals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getDataFromFile("/request/create-individual.json")))
                .andExpectAll(status().isCreated(),
                        content().json(getDataFromFile("/responses/create-response.json")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "create-verifiedAt-null.json",
            "create-archivedAt-null.json",
            "create-address-archived-null.json"})
    void createWithRequiredFieldReturn400(String invalidRequestName) throws Exception {
        mvc.perform(post("/api/v1/individuals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getDataFromFile("/request/" + invalidRequestName)))
                .andExpectAll(status().isBadRequest(),
                        jsonPath("status").value(400),
                        jsonPath("error").isNotEmpty());
    }

    @WithMockUser
    @Test
    void deleteById() throws Exception {
        mvc.perform(delete("/api/v1/individuals/{id}", id))
                .andExpect(status().isNoContent());
        verify(individualService).softDelete(id);
    }

    @Test
    void deleteById_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(delete("/api/v1/individuals/{id}", id))
                .andExpect(status().isUnauthorized());
        verify(individualService, never()).softDelete(id);
    }
}