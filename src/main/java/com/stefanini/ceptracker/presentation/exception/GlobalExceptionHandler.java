package com.stefanini.ceptracker.presentation.exception;

import com.stefanini.ceptracker.infrastructure.exception.CepApiException;
import com.stefanini.ceptracker.presentation.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ConstraintViolationException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Dados inválidos")
                .details(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Recurso não encontrado")
                .details(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(CepApiException.class)
    public ResponseEntity<ErrorResponse> handleCepApiException(CepApiException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Erro na consulta do CEP")
                .details(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Erro interno não tratado: ", e);

        ErrorResponse error = ErrorResponse.builder()
                .message("Erro interno do servidor")
                .details("Ocorreu um erro inesperado")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.internalServerError().body(error);
    }
}