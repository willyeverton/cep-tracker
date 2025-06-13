package com.stefanini.ceptracker.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.stefanini.ceptracker.infrastructure.repository.CepAuditLogRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@ActiveProfiles("test") // usa H2 + Redis embedded
@AutoConfigureWebMvc
class CepTrackerIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CepAuditLogRepository auditLogRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private MockMvc mockMvc;

    @BeforeAll
    static void setUpWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Mock API externa - para teste de FLUXO, não integração real
        wireMockServer.stubFor(get(urlPathMatching("/ws/([0-9]{8})/json/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"cep\": \"01310100\",\n" +
                                "  \"logradouro\": \"Rua das Flores\",\n" +
                                "  \"bairro\": \"Centro\",\n" +
                                "  \"localidade\": \"São Paulo\",\n" +
                                "  \"uf\": \"SP\"\n" +
                                "}")));

        wireMockServer.stubFor(get(urlPathEqualTo("/ws/00000000/json/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"erro\": true}")));
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        auditLogRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();

    }

    @Test
    void shouldProcessCompleteWorkflowEndToEnd() throws Exception {
        // TESTE DO FLUXO COMPLETO:
        // Controller -> Service -> Cache -> API -> Audit

        String cep = "01310100";

        // When - Primeira chamada
        mockMvc.perform(get("/api/v1/cep/{cep}", cep))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value(cep))
                .andExpect(jsonPath("$.logradouro").value("Rua das Flores"));

        // Then - Verificar auditoria
        assertThat(auditLogRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll().get(0).getSuccess()).isTrue();

        // And - Verificar cache
        String cacheKey = "cep:" + cep;
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // When - Segunda chamada (deve usar cache)
        mockMvc.perform(get("/api/v1/cep/{cep}", cep))
                .andExpect(status().isOk());

        // Then - Nova auditoria, mas mesmo resultado
        assertThat(auditLogRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldHandleInvalidCepWorkflow() throws Exception {
        mockMvc.perform(get("/api/v1/cep/00000000"))
                .andExpect(status().isNotFound());

        // Deve auditar falha
        assertThat(auditLogRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll().get(0).getSuccess()).isFalse();
    }

    @Test
    void shouldProvideAuditingCapabilities() throws Exception {
        // Given - Algumas consultas
        mockMvc.perform(get("/api/v1/cep/01310100"));
        mockMvc.perform(get("/api/v1/cep/00000000"));

        // When - Consultar auditoria
        mockMvc.perform(get("/api/v1/audit/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(2))
                .andExpect(jsonPath("$.successfulRequests").value(1))
                .andExpect(jsonPath("$.failedRequests").value(1));
    }
}
