package com.stefanini.ceptracker.domain.service;

import java.time.Duration;

// Interface Segregation Principle
public interface CacheService {
    <T> void save(String key, T value, Duration ttl);

    <T> T get(String key, Class<T> type);
}