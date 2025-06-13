package com.stefanini.ceptracker.presentation.controller;

import com.stefanini.ceptracker.domain.entity.CepAuditLog;
import com.stefanini.ceptracker.infrastructure.repository.CepAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final CepAuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    public ResponseEntity<Page<CepAuditLog>> getAllLogs(Pageable pageable) {
        Page<CepAuditLog> logs = auditLogRepository.findAll(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/cep/{cep}")
    public ResponseEntity<Page<CepAuditLog>> getLogsByCep(
            @PathVariable String cep,
            Pageable pageable) {
        Page<CepAuditLog> logs = auditLogRepository.findByCepOrderByRequestTimestampDesc(cep, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", auditLogRepository.count());
        stats.put("successfulRequests", auditLogRepository.countSuccessfulRequests());
        stats.put("failedRequests", auditLogRepository.countFailedRequests());

        return ResponseEntity.ok(stats);
    }
}