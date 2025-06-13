package com.stefanini.ceptracker.infrastructure.exception;

public class CepApiException extends RuntimeException {
    public CepApiException(String message) {
        super(message);
    }

    public CepApiException(String message, Throwable cause) {
        super(message, cause);
    }
}