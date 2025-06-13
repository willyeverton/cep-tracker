package com.stefanini.ceptracker.domain.service;

import com.stefanini.ceptracker.domain.dto.CepResponse;

// Interface Segregation Principle & Dependency Inversion Principle
public interface CepService {
    CepResponse findCep(String cep);
}