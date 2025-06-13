package com.stefanini.ceptracker.infrastructure.client;

import com.stefanini.ceptracker.domain.dto.CepResponse;
import com.stefanini.ceptracker.infrastructure.exception.CepApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class CepApiClientImpl implements CepApiClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.cep-service.external-api.base-url}")
    private String baseUrl;

    @Value("${app.cep-service.external-api.timeout:5000}")
    private int timeoutMs;

    @Override
    public CepResponse findCep(String cep) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .build();

            return webClient
                    .get()
                    .uri("/ws/{cep}/json/", cep)
                    .retrieve()
                    .bodyToMono(CepResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

        } catch (WebClientException e) {
            log.error("Erro ao consultar CEP {} na API externa: {}", cep, e.getMessage());
            throw new CepApiException("Erro ao consultar CEP na API externa", e);
        }
    }
}