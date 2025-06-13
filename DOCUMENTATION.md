# CEP Tracker - Arquitetura da Aplicação

## Visão Geral

O CEP Tracker implementa **Clean Architecture** para consulta de CEPs com cache Redis, auditoria completa e deploy em containers Docker.

## 🎯 Diagrama de Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                               PRESENTATION LAYER                                    │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │    CepController        │  │   AuditController       │  │ GlobalExceptionHandler│ │
│  │                         │  │                         │  │                     │ │
│  │ GET /api/v1/cep/{cep}  │  │ GET /api/v1/audit/logs  │  │  - Validation errors│ │
│  │ - Validação @Pattern   │  │ GET /api/v1/audit/stats │  │  - Entity not found │ │
│  │ - Captura IP/UserAgent │  │ - Paginação (Pageable) │  │  - CepApiException  │ │
│  │ - Logs de requisição   │  │                         │  │  - Generic errors   │ │
│  └─────────────────────────┘  └─────────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────┬───────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                               APPLICATION LAYER                                     │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │    CepServiceImpl       │  │   AuditServiceImpl      │  │  CacheServiceImpl   │ │
│  │                         │  │                         │  │                     │ │
│  │ - Cache-aside pattern  │  │ - logCepRequest()       │  │ - save() with TTL   │ │
│  │ - Métricas customizadas│  │ - findById()            │  │ - get() com Class   │ │
│  │ - @Timed annotation    │  │ - Enriquece com IP/UA   │  │ - delete()          │ │
│  │ - Orquestração fluxo   │  │ - Timestamp automático  │  │ - exists()          │ │
│  └─────────────────────────┘  └─────────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────┬───────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                DOMAIN LAYER                                         │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │      Entities           │  │        DTOs             │  │     Interfaces      │ │
│  │                         │  │                         │  │                     │ │
│  │ CepAuditLog (@Entity)  │  │ CepResponse             │  │ CepService          │ │
│  │ - id (IDENTITY)        │  │ - cep, street, city     │  │ AuditService        │ │
│  │ - cep (8 chars)        │  │ - state, neighborhood   │  │ CacheService        │ │
│  │ - requestTimestamp     │  │ - @JsonProperty mapping │  │ CepApiClient        │ │
│  │ - responseData (TEXT)  │  │ - erro (Boolean)        │  │                     │ │
│  │ - success, errorMsg    │  │                         │  │                     │ │
│  │ - executionTimeMs      │  │ ErrorResponse           │  │                     │ │
│  │ - sourceIp, userAgent  │  │ - message, details      │  │                     │ │
│  └─────────────────────────┘  └─────────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────┬───────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                             INFRASTRUCTURE LAYER                                    │
│                                                                                     │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │   Database Access       │  │    External APIs        │  │      Caching        │ │
│  │                         │  │                         │  │                     │ │
│  │ CepAuditLogRepository  │  │  CepApiClientImpl       │  │ RedisTemplate<S,S>  │ │
│  │ - JpaRepository        │  │  - WebClient.Builder    │  │ - StringSerializer  │ │
│  │ - findByCepOrderBy...  │  │  - @Value(baseUrl)      │  │ - JSON serialization│ │
│  │ - findByTimestampBetw. │  │  - @Value(timeout)      │  │ - ObjectMapper      │ │
│  │ - countSuccessful()    │  │  - Exception handling   │  │                     │ │
│  │ - countFailed()        │  │  - .timeout(Duration)   │  │                     │ │
│  └─────────────────────────┘  └─────────────────────────┘  └─────────────────────┘ │
│                                                                                     │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │     Configuration       │  │      Monitoring         │  │      Exception      │ │
│  │                         │  │                         │  │                     │ │
│  │ WebClientConfig        │  │ MetricsConfig           │  │ CepApiException     │ │
│  │ - WebClient.Builder    │  │ - TimedAspect Bean      │  │ - RuntimeException  │ │
│  │                        │  │ - MeterRegistry         │  │                     │ │
│  │ RedisConfig            │  │                         │  │                     │ │
│  │ - RedisTemplate        │  │ Custom Counters:        │  │                     │ │
│  │ - ConnectionFactory    │  │ - cepRequestCounter     │  │                     │ │
│  │ - Serializers          │  │ - cacheHitCounter       │  │                     │ │
│  └─────────────────────────┘  └─────────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────┬───────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL SYSTEMS                                       │
│                                                                                     │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │      PostgreSQL         │  │      Redis Cache        │  │    External CEP API │ │
│  │                         │  │                         │  │                     │ │
│  │ Tabela: cep_audit_logs │  │ - Key pattern: cep:*    │  │ - ViaCEP (prod)     │ │
│  │ Criação automática JPA │  │ - TTL: configurável     │  │ - WireMock (dev)    │ │
│  │ - DDL auto: update     │  │ - JSON serialization    │  │ - Timeout: 5000ms   │ │
│  │                        │  │ - String Redis Template │  │ - GET /ws/{cep}/json│ │
│  └─────────────────────────┘  └─────────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Fluxo de Dados Implementado

### **Consulta de CEP - Fluxo Completo**

```
┌─────────┐    ┌──────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐
│ Cliente │    │ CepController│    │ CepService  │    │ CacheService│    │ CepApiClient │
└────┬────┘    └──────┬───────┘    └──────┬──────┘    └──────┬──────┘    └──────┬───────┘
    │                │                   │                  │                  │
    │ GET /cep/01310100                  │                  │                  │
    ├───────────────►│                   │                  │                  │
    │                │ @Pattern validation                  │                  │
    │                │ getClientIpAddress()                 │                  │
    │                │                   │                  │                  │
    │                │ findCep("01310100")                  │                  │
    │                ├──────────────────►│                  │                  │
    │                │                   │ get("cep:01310100", CepResponse.class) │
    │                │                   ├─────────────────►│                  │
    │                │                   │ null (cache miss)│                  │
    │                │                   │◄─────────────────┤                  │
    │                │                   │ cacheMissCounter.increment()        │
    │                │                   │                  │                  │
    │                │                   │ findCep("01310100")                 │
    │                │                   ├──────────────────────────────────────►│
    │                │                   │                  │ WebClient.get()  │
    │                │                   │                  │ .timeout(5000ms) │
    │                │                   │                  │ CepResponse      │
    │                │                   │◄──────────────────────────────────────┤
    │                │                   │ save("cep:01310100", response, TTL)    │
    │                │                   ├─────────────────►│                  │
    │                │                   │                  │                  │
    │                │ CepResponse       │                  │                  │
    │                │◄──────────────────┤                  │                  │
    │                │ serializeToJson(response)            │                  │
    │                │                   │                  │                  │
    │ 200 OK + JSON  │                   │                  │                  │
    │◄───────────────┤                   │                  │                  │
```

### **Auditoria - Fluxo Paralelo**

```
┌─────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ CepController│    │ AuditService    │    │ Database        │
└──────┬──────┘    └─────────┬───────┘    └─────────┬───────┘
      │                     │                      │
      │ logCepRequest(cep, responseData, success,  │
      │   errorMessage, executionTime,             │
      │   sourceIp, userAgent)                     │
      ├────────────────────►│                      │
      │                     │ CepAuditLog.builder()│
      │                     │ .cep(cep)            │
      │                     │ .requestTimestamp(now)│
      │                     │ .responseData(json)   │
      │                     │ .success(success)     │
      │                     │ .executionTimeMs(time)│
      │                     │ .sourceIp(ip)        │
      │                     │ .userAgent(ua)       │
      │                     │ .build()             │
      │                     │                      │
      │                     │ save(auditLog)       │
      │                     ├─────────────────────►│
      │                     │                      │
      │ (continua execução) │                      │
      │◄────────────────────┤                      │
```

## 🏛️ Componentes Implementados

### **1. Presentation Layer**

#### **CepController**
```java
@RestController
@RequestMapping("/api/v1/cep")
@RequiredArgsConstructor
@Validated
public class CepController {
    
   @GetMapping("/{cep}")
   public ResponseEntity<?> getCep(
           @PathVariable @Pattern(regexp = "\\d{8}") String cep,
           HttpServletRequest request) {
        
       // Funcionalidades implementadas:
       // - Validação Bean Validation
       // - Captura IP (X-Forwarded-For, X-Real-IP, RemoteAddr)
       // - Captura User-Agent
       // - Medição tempo execução
       // - Chamada para CepService
       // - Auditoria via AuditService
       // - Serialização JSON da resposta
       // - Tratamento de erros
    }
    
    // Métodos utilitários implementados:
    // - getClientIpAddress() - captura IP real
    // - serializeToJson() - conversão para JSON
}
```

#### **AuditController**
```java
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
    
    // GET /logs - lista todos os logs com paginação
    // GET /logs/cep/{cep} - logs por CEP específico
    // GET /stats - estatísticas de uso
}
```

#### **GlobalExceptionHandler**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // Tratamentos implementados:
    // - ConstraintViolationException → 400 Bad Request
    // - EntityNotFoundException → 404 Not Found
    // - CepApiException → 503 Service Unavailable
    // - Exception genérica → 500 Internal Server Error
}
```

### **2. Application Layer**

#### **CepServiceImpl**
```java
@Service
public class CepServiceImpl implements CepService {
    
    // Funcionalidades implementadas:
    // - Cache-aside pattern
    // - Métricas customizadas (Counter para requests, hits, misses)
    // - @Timed para métricas de tempo
    // - Verificação cache primeiro
    // - Fallback para API externa
    // - Cache apenas respostas válidas (erro != true)
    // - TTL configurável via @Value
}
```

#### **AuditServiceImpl**
```java
@Service
public class AuditServiceImpl implements AuditService {
    
    // Funcionalidades implementadas:
    // - logCepRequest() - salva auditoria completa
    // - findById() - busca por ID
    // - Enriquecimento com timestamp automático
    // - Logs estruturados com SLF4J
}
```

#### **CacheServiceImpl**
```java
@Service
public class CacheServiceImpl implements CacheService {
    
    // Funcionalidades implementadas:
    // - save() com TTL Duration
    // - get() com deserialização por Class
    // - delete() para limpeza
    // - exists() para verificação
    // - Serialização/deserialização JSON via ObjectMapper
    // - Tratamento de erros de serialização
}
```

### **3. Domain Layer**

#### **Entidades**
```java
@Entity
@Table(name = "cep_audit_logs")
public class CepAuditLog {
    
    // Campos implementados:
    // - id (IDENTITY)
    // - cep (8 chars, not null)
    // - requestTimestamp (not null)
    // - responseData (TEXT)
    // - success (Boolean, not null)
    // - errorMessage (String)
    // - executionTimeMs (Long)
    // - sourceIp (String)
    // - userAgent (String)
}
```

#### **DTOs**
```java
public class CepResponse implements Serializable {
    
    // Mapeamento JSON implementado:
    // - cep
    // - logradouro → street
    // - complemento → complement
    // - bairro → neighborhood
    // - localidade → city
    // - uf → state
    // - ibge → ibgeCode
    // - gia → giaCode
    // - ddd → areaCode
    // - siafi → siafiCode
    // - erro (Boolean)
}

public class ErrorResponse {
    // - message, details, timestamp
}
```

### **4. Infrastructure Layer**

#### **CepApiClientImpl**
```java
@Component
public class CepApiClientImpl implements CepApiClient {
    
    // Implementações:
    // - WebClient com baseUrl configurável
    // - Timeout configurável via @Value
    // - GET /ws/{cep}/json/
    // - Tratamento WebClientException → CepApiException
    // - Logs de erro estruturados
}
```

#### **CepAuditLogRepository**
```java
@Repository
public interface CepAuditLogRepository extends JpaRepository<CepAuditLog, Long> {
    
    // Queries implementadas:
    // - findByCepOrderByRequestTimestampDesc()
    // - findByRequestTimestampBetween()
    // - countSuccessfulRequests()
    // - countFailedRequests()
}
```

#### **Configurações**
```java
@Configuration
public class WebClientConfig {
    // WebClient.Builder bean
}

@Configuration
public class RedisConfig {
    // RedisTemplate<String, String> com StringRedisSerializer
}

@Configuration
public class MetricsConfig {
    // TimedAspect bean para @Timed
}
```

## Banco de Dados

### **Schema Implementado**
```sql
-- Tabela: cep_audit_logs (criada automaticamente pelo JPA)
-- DDL AUTO: update (spring.jpa.hibernate.ddl-auto=update)

-- Estrutura da tabela:
CREATE TABLE cep_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    cep VARCHAR(8) NOT NULL,
    request_timestamp TIMESTAMP NOT NULL,
    response_data TEXT,
    success BOOLEAN NOT NULL DEFAULT false,
    error_message TEXT,
    execution_time_ms BIGINT,
    source_ip VARCHAR(45),
    user_agent TEXT
);

-- Índices são criados automaticamente pelo JPA conforme necessário
-- Para otimizações futuras, podem ser adicionados índices manuais
```

## Cache Strategy

### **Implementação Cache-Aside**
```java
public CepResponse findCep(String cep) {
    cepRequestCounter.increment();
    
    String cacheKey = "cep:" + cep;
    
    // 1. Verificar cache
    CepResponse cached = cacheService.get(cacheKey, CepResponse.class);
    if (cached != null) {
        cacheHitCounter.increment();
        return cached; // Cache HIT
    }
    
    cacheMissCounter.increment();
    
    // 2. Buscar na API
    CepResponse response = cepApiClient.findCep(cep);
    
    // 3. Cachear se válido
    if (response != null && (response.getErro() == null || !response.getErro())) {
        cacheService.save(cacheKey, response, Duration.ofSeconds(cacheTtlSeconds));
    }
    
    return response;
}
```

### **Configurações de Cache**
- **Namespace**: `cep:` prefix
- **TTL**: Configurável via `app.cep-service.cache.ttl` (padrão: 3600s)
- **Serialização**: JSON via Jackson ObjectMapper
- **Eviction**: Não configurada (padrão Redis)

## 📈 Monitoramento Implementado

### **Métricas Customizadas**
```java
// Implementadas no CepServiceImpl:
- cepRequestCounter: Total de requests
- cacheHitCounter: Cache hits
- cacheMissCounter: Cache misses

// Via @Timed annotation:
- cep.lookup.time: Tempo de consulta CEP
```

### **Health Checks**
```yaml
# application.yml - implementado
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### **Endpoints de Monitoramento**
- `GET /actuator/health` - Status aplicação
- `GET /actuator/metrics` - Lista todas métricas
- `GET /actuator/metrics/{nome}` - Métrica específica

## Testes Implementados

### **Estrutura de Testes**
```
src/test/java/
├── integration/
│   └── CepTrackerIntegrationTest.java    # TestContainers (Redis) + @MockBean
├── application/service/
│   └── CepServiceImplTest.java           # Testes unitários
└── presentation/controller/
    └── CepControllerTest.java            # Testes WebMvc
```

### **Tecnologias de Teste**
- **JUnit 5**: Framework base
- **Mockito**: Mocks e stubs
- **TestContainers**: Redis real para integração
- **@MockBean**: Mock da API externa
- **@WebMvcTest**: Testes controller
- **@SpringBootTest**: Testes integração
- **H2**: Banco de dados para testes

## Containerização

### **Docker Compose Implementado**
```yaml
services:
  postgres:    # PostgreSQL 13-alpine
  redis:       # Redis 7-alpine  
  wiremock:    # WireMock para desenvolvimento
  app:         # Aplicação Spring Boot
```

### **Dockerfile**
```dockerfile
FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/cep-tracker-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Configurações por Ambiente

### **Profile: padrão (application.yml)**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ceptracker
  redis:
    host: localhost
app:
  cep-service:
    external-api:
      base-url: https://viacep.com.br  # API real
```

### **Profile: docker (application-docker.yml)**
```yaml
# Sobrescrições para ambiente Docker
logging:
  level:
    com.stefanini.ceptracker: DEBUG
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/ceptracker
  redis:
    host: redis
app:
  cep-service:
    external-api:
      base-url: http://wiremock:8080  # WireMock
```

### **Profile: aws (application-aws.yml)**
```yaml
# Configurações específicas de produção
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
app:
  cep-service:
    external-api:
      base-url: https://viacep.com.br  # API real
```

### **Profile: test (application-test.yml)**
```yaml
# Configurações para testes automatizados
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Conclusão

A aplicação CEP Tracker implementa uma arquitetura limpa e bem estruturada com:

### **Funcionalidades Implementadas**
- **Clean Architecture** com separação clara de camadas
- **Cache Redis** com pattern cache-aside
- **Auditoria completa** de consultas
- **Métricas customizadas** para monitoramento
- **Tratamento de erros** robusto
- **Testes automatizados** com boa cobertura
- **Containerização** completa
- **Multi-environment** (padrão/docker/aws/test)

### **Tecnologias Core**
- Java 11 + Spring Boot 2.7
- PostgreSQL + Redis
- Docker + Docker Compose
- JUnit 5 + TestContainers + Mockito
- Micrometer + Spring Actuator

A implementação foca em **simplicidade, robustez e manutenibilidade**, seguindo boas práticas de desenvolvimento.