package com.stefanini.ceptracker.presentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stefanini.ceptracker.domain.dto.CepResponse;
import com.stefanini.ceptracker.domain.service.AuditService;
import com.stefanini.ceptracker.domain.service.CepService;
import com.stefanini.ceptracker.presentation.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/cep")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CepController {

    private final CepService cepService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{cep}")
    public ResponseEntity<?> getCep(
            @PathVariable @Pattern(regexp = "\\d{8}", message = "CEP deve conter exatamente 8 dígitos") String cep,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String sourceIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            log.info("Iniciando consulta do CEP: {}", cep);

            CepResponse response = cepService.findCep(cep);
            long executionTime = System.currentTimeMillis() - startTime;

            if (response != null && (response.getErro() == null || !response.getErro())) {
                // Log da consulta bem-sucedida
                auditService.logCepRequest(
                        cep,
                        serializeToJson(response),
                        true,
                        null,
                        executionTime,
                        sourceIp,
                        userAgent);

                log.info("CEP {} consultado com sucesso em {}ms", cep, executionTime);
                return ResponseEntity.ok(response);
            } else {
                // CEP não encontrado
                auditService.logCepRequest(
                        cep,
                        null,
                        false,
                        "CEP não encontrado",
                        executionTime,
                        sourceIp,
                        userAgent);

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log do erro
            auditService.logCepRequest(
                    cep,
                    null,
                    false,
                    e.getMessage(),
                    executionTime,
                    sourceIp,
                    userAgent);

            log.error("Erro ao consultar CEP {}: {}", cep, e.getMessage());

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .message("Erro interno do servidor")
                    .details(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar resposta para JSON: {}", e.getMessage());
            return object.toString();
        }
    }
}