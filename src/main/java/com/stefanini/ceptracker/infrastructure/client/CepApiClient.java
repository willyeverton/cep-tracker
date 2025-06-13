package com.stefanini.ceptracker.infrastructure.client;

import com.stefanini.ceptracker.domain.dto.CepResponse;

public interface CepApiClient {
    CepResponse findCep(String cep);
}