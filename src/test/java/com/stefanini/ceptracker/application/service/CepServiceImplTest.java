package com.stefanini.ceptracker.application.service;

import com.stefanini.ceptracker.domain.dto.CepResponse;
import com.stefanini.ceptracker.domain.service.CacheService;
import com.stefanini.ceptracker.domain.service.CepService;
import com.stefanini.ceptracker.infrastructure.client.CepApiClient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CepServiceImplTest {

        @MockBean
        private CepApiClient cepApiClient;

        @MockBean
        private CacheService cacheService;

        @Autowired
        private CepService cepService;

        @Test
        void shouldReturnCachedCepWhenAvailable() {
                // Given
                String cep = "01310100";
                CepResponse cachedResponse = CepResponse.builder()
                                .cep(cep)
                                .street("Cached Street")
                                .build();

                when(cacheService.get("cep:" + cep, CepResponse.class))
                                .thenReturn(cachedResponse);

                // When
                CepResponse result = cepService.findCep(cep);

                // Then
                assertThat(result).isEqualTo(cachedResponse);
                verify(cepApiClient, never()).findCep(cep);
                verify(cacheService, never()).save(any(), any(), any());
        }

        @Test
        void shouldFetchFromApiAndCacheWhenNotInCache() {
                // Given
                String cep = "01310100";
                CepResponse apiResponse = CepResponse.builder()
                                .cep(cep)
                                .street("API Street")
                                .build();

                when(cacheService.get("cep:" + cep, CepResponse.class))
                                .thenReturn(null);
                when(cepApiClient.findCep(cep))
                                .thenReturn(apiResponse);

                // When
                CepResponse result = cepService.findCep(cep);

                // Then
                assertThat(result).isEqualTo(apiResponse);
                verify(cepApiClient).findCep(cep);
                verify(cacheService).save(eq("cep:" + cep), eq(apiResponse), eq(Duration.ofSeconds(3600)));
        }

        @Test
        void shouldNotCacheWhenResponseHasError() {
                // Given
                String cep = "00000000";
                CepResponse errorResponse = CepResponse.builder()
                                .erro(true)
                                .build();

                when(cacheService.get("cep:" + cep, CepResponse.class))
                                .thenReturn(null);
                when(cepApiClient.findCep(cep))
                                .thenReturn(errorResponse);

                // When
                CepResponse result = cepService.findCep(cep);

                // Then
                assertThat(result).isEqualTo(errorResponse);
                verify(cepApiClient).findCep(cep);
                verify(cacheService, never()).save(any(), any(), any());
        }
}