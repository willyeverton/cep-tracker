package com.stefanini.ceptracker.integration;

import com.stefanini.ceptracker.domain.dto.CepResponse;
import com.stefanini.ceptracker.infrastructure.client.CepApiClient;
import com.stefanini.ceptracker.infrastructure.repository.CepAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CepTrackerIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @MockBean
    private CepApiClient cepApiClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CepAuditLogRepository auditLogRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void shouldProcessCompleteWorkflowEndToEnd() {
        // Given
        String cep = "01310100";
        CepResponse apiResponse = CepResponse.builder()
                .cep(cep)
                .street("Rua das Flores")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .build();

        when(cepApiClient.findCep(cep)).thenReturn(apiResponse);

        // When - Primeira chamada (cache miss)
        ResponseEntity<CepResponse> response1 = restTemplate.getForEntity(
                "/api/v1/cep/{cep}", CepResponse.class, cep);

        // Then
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody()).isNotNull();
        assertThat(response1.getBody().getCep()).isEqualTo(cep);
        assertThat(response1.getBody().getStreet()).isEqualTo("Rua das Flores");

        // Verificar auditoria
        assertThat(auditLogRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll().get(0).getSuccess()).isTrue();

        // Verificar cache
        String cacheKey = "cep:" + cep;
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // When - Segunda chamada (cache hit)
        ResponseEntity<CepResponse> response2 = restTemplate.getForEntity(
                "/api/v1/cep/{cep}", CepResponse.class, cep);

        // Then - Mesma resposta, nova auditoria
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getBody().getCep()).isEqualTo(cep);
        assertThat(auditLogRepository.count()).isEqualTo(2); // Nova auditoria
    }

    @Test
    void shouldHandleInvalidCepWorkflow() {
        // Given
        String invalidCep = "00000000";
        CepResponse errorResponse = CepResponse.builder()
                .erro(true)
                .build();

        when(cepApiClient.findCep(invalidCep)).thenReturn(errorResponse);

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/cep/{cep}", String.class, invalidCep);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Verificar auditoria de falha
        assertThat(auditLogRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll().get(0).getSuccess()).isFalse();
    }

    @Test
    void shouldHandleInvalidCepFormat() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/cep/{cep}", String.class, "123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldProvideAuditingCapabilities() {
        // Given - Fazer algumas consultas
        setupValidCepMock("01310100");
        setupInvalidCepMock("00000000");

        restTemplate.getForEntity("/api/v1/cep/01310100", CepResponse.class);
        restTemplate.getForEntity("/api/v1/cep/00000000", String.class);

        // When - Consultar estatísticas
        ResponseEntity<Map> statsResponse = restTemplate.getForEntity(
                "/api/v1/audit/stats", Map.class);

        // Then
        assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statsResponse.getBody()).isNotNull();
        assertThat(statsResponse.getBody().get("totalRequests")).isEqualTo(2);
        assertThat(statsResponse.getBody().get("successfulRequests")).isEqualTo(1);
        assertThat(statsResponse.getBody().get("failedRequests")).isEqualTo(1);
    }

    @Test
    void shouldCacheOnlyValidResponses() {
        // Given
        String validCep = "01310100";
        String invalidCep = "00000000";

        setupValidCepMock(validCep);
        setupInvalidCepMock(invalidCep);

        // When
        restTemplate.getForEntity("/api/v1/cep/{cep}", CepResponse.class, validCep);
        restTemplate.getForEntity("/api/v1/cep/{cep}", String.class, invalidCep);

        // Then
        assertThat(redisTemplate.hasKey("cep:" + validCep)).isTrue(); // Válido é cacheado
        assertThat(redisTemplate.hasKey("cep:" + invalidCep)).isFalse(); // Inválido não é cacheado
    }

    @Test
    void shouldProvidePaginatedAuditLogs() {
        // Given - Fazer algumas consultas
        setupValidCepMock("01310100");
        restTemplate.getForEntity("/api/v1/cep/01310100", CepResponse.class);
        restTemplate.getForEntity("/api/v1/cep/01310100", CepResponse.class);

        // When - Consultar logs paginados
        ResponseEntity<Map> logsResponse = restTemplate.getForEntity(
                "/api/v1/audit/logs?size=1&page=0", Map.class);

        // Then
        assertThat(logsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logsResponse.getBody()).isNotNull();
        assertThat(logsResponse.getBody().get("totalElements")).isEqualTo(2);
        assertThat(logsResponse.getBody().get("size")).isEqualTo(1);
    }

    @Test
    void shouldProvideLogsByCep() {
        // Given
        String cep = "01310100";
        setupValidCepMock(cep);
        restTemplate.getForEntity("/api/v1/cep/{cep}", CepResponse.class, cep);

        // When
        ResponseEntity<Map> logsResponse = restTemplate.getForEntity(
                "/api/v1/audit/logs/cep/{cep}", Map.class, cep);

        // Then
        assertThat(logsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logsResponse.getBody()).isNotNull();
        assertThat(logsResponse.getBody().get("totalElements")).isEqualTo(1);
    }

    // Métodos auxiliares
    private void setupValidCepMock(String cep) {
        CepResponse validResponse = CepResponse.builder()
                .cep(cep)
                .street("Rua Teste")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .build();
        when(cepApiClient.findCep(cep)).thenReturn(validResponse);
    }

    private void setupInvalidCepMock(String cep) {
        CepResponse errorResponse = CepResponse.builder()
                .erro(true)
                .build();
        when(cepApiClient.findCep(cep)).thenReturn(errorResponse);
    }
}