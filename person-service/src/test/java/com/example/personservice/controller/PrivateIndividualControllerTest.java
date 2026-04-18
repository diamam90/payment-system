package com.example.personservice.controller;

import com.example.personservice.config.SecurityConfig;
import com.example.personservice.config.SecurityTestConfig;
import com.example.personservice.service.IndividualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateIndividualController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityTestConfig.class})
class PrivateIndividualControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    IndividualService individualService;

    private static final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000022");

    @WithMockUser(username = "user", authorities = {"person_service_wr"})
    @Test
    void shouldDeleteById() throws Exception {
        mvc.perform(delete("/private/api/v1/individuals/{id}", id))
                .andExpect(status().isNoContent());

        verify(individualService).hardDelete(id);
    }

    @WithMockUser(username = "user", authorities = {"person_service_wr"})
    @Test
    void shouldActivateUser() throws Exception {
        mvc.perform(post("/private/api/v1/individuals/{id}", id))
                .andExpect(status().isOk());

        verify(individualService).activateUser(id);
    }

    @Test
    void shouldDeleteById_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(delete("/private/api/v1/individuals/{id}/forced", id))
                .andExpect(status().isUnauthorized());
        verify(individualService, never()).hardDelete(any());
    }
}