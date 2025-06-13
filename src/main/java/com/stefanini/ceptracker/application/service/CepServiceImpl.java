package com.stefanini.ceptracker.application.service;

import com.stefanini.ceptracker.domain.dto.CepResponse;
import com.stefanini.ceptracker.domain.service.CacheService;
import com.stefanini.ceptracker.domain.service.CepService;
import com.stefanini.ceptracker.infrastructure.client.CepApiClient;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class CepServiceImpl implements CepService {

    private final CepApiClient cepApiClient;
    private final CacheService cacheService;
    private final Counter cepRequestCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public CepServiceImpl(CepApiClient cepApiClient,
            CacheService cacheService,
            MeterRegistry meterRegistry) {
        this.cepApiClient = cepApiClient;
        this.cacheService = cacheService;
        this.cepRequestCounter = Counter.builder("cep.requests.total")
                .register(meterRegistry);
        this.cacheHitCounter = Counter.builder("cep.cache.hits")
                .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cep.cache.misses")
                .register(meterRegistry);
    }

    @Value("${app.cep-service.cache.ttl:3600}")
    private int cacheTtlSeconds;

    @Override
    @Timed(value = "cep.lookup.time", description = "Time taken to lookup CEP")
    public CepResponse findCep(String cep) {
        cepRequestCounter.increment();

        String cacheKey = "cep:" + cep;

        // Tentar buscar no cache primeiro
        CepResponse cachedResponse = cacheService.get(cacheKey, CepResponse.class);
        if (cachedResponse != null) {
            log.debug("CEP {} encontrado no cache", cep);
            cacheHitCounter.increment();
            return cachedResponse;
        }

        cacheMissCounter.increment();

        // Buscar na API externa
        log.debug("Buscando CEP {} na API externa", cep);
        CepResponse response = cepApiClient.findCep(cep);

        // Cachear o resultado se for válido
        if (response != null && (response.getErro() == null || !response.getErro())) {
            cacheService.save(cacheKey, response, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("CEP {} cacheado com sucesso", cep);
        }

        return response;
    }
}