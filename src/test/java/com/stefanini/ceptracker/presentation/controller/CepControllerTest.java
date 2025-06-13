package com.stefanini.ceptracker.presentation.controller;

import com.stefanini.ceptracker.domain.dto.CepResponse;
import com.stefanini.ceptracker.domain.service.AuditService;
import com.stefanini.ceptracker.domain.service.CepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CepController.class)
class CepControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CepService cepService;

        @MockBean
        private AuditService auditService;

        @Test
        void shouldReturnCepWhenValidCepProvided() throws Exception {
                // Given
                String cep = "01310100";
                CepResponse expectedResponse = CepResponse.builder()
                                .cep(cep)
                                .street("Avenida Paulista")
                                .neighborhood("Bela Vista")
                                .city("São Paulo")
                                .state("SP")
                                .build();

                when(cepService.findCep(cep)).thenReturn(expectedResponse);
                doNothing().when(auditService).logCepRequest(
                                anyString(), anyString(), anyBoolean(), any(), anyLong(), anyString(), anyString());

                // When & Then
                mockMvc.perform(get("/api/v1/cep/{cep}", cep)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.cep").value(cep))
                                .andExpect(jsonPath("$.logradouro").value("Avenida Paulista"))
                                .andExpect(jsonPath("$.localidade").value("São Paulo"));
        }

        @Test
        void shouldReturnBadRequestForInvalidCep() throws Exception {
                // Given
                String invalidCep = "123";

                // When & Then
                mockMvc.perform(get("/api/v1/cep/{cep}", invalidCep)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnNotFoundWhenCepNotExists() throws Exception {
                // Given
                String cep = "00000000";
                CepResponse responseWithError = CepResponse.builder()
                                .erro(true)
                                .build();

                when(cepService.findCep(cep)).thenReturn(responseWithError);
                doNothing().when(auditService).logCepRequest(
                                anyString(), any(), anyBoolean(), anyString(), anyLong(), anyString(), anyString());

                // When & Then
                mockMvc.perform(get("/api/v1/cep/{cep}", cep)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }
}