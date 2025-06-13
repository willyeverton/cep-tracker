package com.stefanini.ceptracker.application.service;

import com.stefanini.ceptracker.domain.entity.CepAuditLog;
import com.stefanini.ceptracker.domain.service.AuditService;
import com.stefanini.ceptracker.infrastructure.repository.CepAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final CepAuditLogRepository auditLogRepository;

    @Override
    public void logCepRequest(String cep, String responseData, boolean success,
            String errorMessage, long executionTime,
            String sourceIp, String userAgent) {

        CepAuditLog auditLog = CepAuditLog.builder()
                .cep(cep)
                .requestTimestamp(LocalDateTime.now())
                .responseData(responseData)
                .success(success)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTime)
                .sourceIp(sourceIp)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log criado para CEP: {}, Success: {}", cep, success);
    }

    @Override
    public CepAuditLog findById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Audit log não encontrado com ID: " + id));
    }
}