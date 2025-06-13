package com.stefanini.ceptracker.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stefanini.ceptracker.domain.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> void save(String key, T value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
            log.debug("Valor salvo no cache com chave: {}", key);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar objeto para cache: {}", e.getMessage());
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(key);
            if (jsonValue != null) {
                return objectMapper.readValue(jsonValue, type);
            }
        } catch (JsonProcessingException e) {
            log.error("Erro ao deserializar objeto do cache: {}", e.getMessage());
        }
        return null;
    }
}