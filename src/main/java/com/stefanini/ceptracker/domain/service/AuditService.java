package com.stefanini.ceptracker.domain.service;

import com.stefanini.ceptracker.domain.entity.CepAuditLog;

// Single Responsibility Principle
public interface AuditService {
    void logCepRequest(String cep, String responseData, boolean success,
            String errorMessage, long executionTime,
            String sourceIp, String userAgent);

    CepAuditLog findById(Long id);
}