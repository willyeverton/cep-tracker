# CEP Tracker

> Sistema de consulta e rastreamento de CEPs com auditoria completa e cache Redis.

## Sobre o Projeto

O **CEP Tracker** é uma aplicação Spring Boot que implementa Clean Architecture para consulta de CEPs brasileiros com:

- **Consulta de CEPs** via API externa
- **Cache Redis** para performance
- **Auditoria completa** de todas as consultas
- **Métricas básicas** com Micrometer
- **Containerização** com Docker
- **Testes automatizados**

## Tecnologias

### Core
- **Java 11**
- **Spring Boot 2.7.14**
- **Spring Data JPA**
- **Spring WebFlux** (WebClient)
- **Spring Data Redis**
- **Spring Actuator**

### Banco de Dados
- **PostgreSQL 13**
- **Redis 7**

### Testes
- **JUnit 5**
- **Mockito**
- **TestContainers**
- **WireMock**

### DevOps
- **Docker** e **Docker Compose**
- **Maven 3.x**

## Pré-requisitos

- **Java 11+**
- **Maven 3.6+**
- **Docker** e **Docker Compose**

## Instalação e Execução

### 1. Clone o Repositório
```bash
git clone https://github.com/willyeverton/cep-tracker.git
cd cep-tracker
```

### 2. Build da aplicação (gera o .jar)
```bash
./mvnw clean package -DskipTests
```

### 3. Execução com Docker
```bash
# Inicia todos os serviços
docker-compose up -d

# Verifica status
docker-compose ps

# Logs da aplicação
docker-compose logs -f app
```

A aplicação estará disponível em: `http://localhost:8080`

## API Endpoints

### Consultar CEP
```http
GET /api/v1/cep/{cep}
```

**Exemplo:**
```bash
curl http://localhost:8080/api/v1/cep/01310100
```

**Resposta:**
```json
{
  "cep": "01310100",
  "logradouro": "Rua das Flores",
  "complemento": "Apto 101",
  "bairro": "Centro",
  "localidade": "São Paulo",
  "uf": "SP",
  "ibge": "3550308",
  "gia": "1004",
  "ddd": "11",
  "siafi": "7107"
}
```

### Logs de Auditoria
```http
GET /api/v1/audit/logs
GET /api/v1/audit/logs/cep/{cep}
GET /api/v1/audit/stats
```

### Monitoramento
```http
GET /actuator/health
GET /actuator/metrics
```

## Configuração

### Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|---------|-----------|
| `PROFILE` | `dev` | Perfil ativo |
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host Redis |
| `CEP_API_URL` | `http://localhost:8089` | URL API CEP |

### Profiles

- **`dev`**: Ambiente local
- **`docker`**: Containers Docker  
- **`aws`**: Produção AWS

## Testes

```bash
# Todos os testes
./mvnw test

# Com cobertura
./mvnw test jacoco:report

# Ver cobertura
open target/site/jacoco/index.html
```

## Arquitetura

### Estrutura do Projeto
```
src/main/java/com/stefanini/ceptracker/
├── application/service/          # Camada de Aplicação
│   ├── CepServiceImpl           # Orquestração CEP
│   ├── AuditServiceImpl         # Auditoria
│   └── CacheServiceImpl         # Cache Redis
├── domain/                      # Camada de Domínio
│   ├── entity/CepAuditLog      # Entidade de Auditoria
│   ├── dto/CepResponse         # DTO de Resposta
│   └── service/                # Interfaces de Serviço
├── infrastructure/             # Camada de Infraestrutura
│   ├── client/CepApiClientImpl # Cliente API Externa
│   ├── repository/             # Repositórios JPA
│   ├── config/                 # Configurações
│   └── exception/              # Exceções
└── presentation/               # Camada de Apresentação
    ├── controller/             # Controllers REST
    ├── dto/ErrorResponse       # DTOs de Erro
    └── exception/              # Exception Handlers
```

### Fluxo Principal
1. **Controller** recebe requisição e valida CEP
2. **CepService** verifica cache Redis primeiro
3. Se não encontrado, chama **CepApiClient** 
4. **AuditService** registra a consulta no banco
5. Resposta é cacheada (se válida) e retornada

## Docker

### Serviços Docker Compose
- **app**: Aplicação Spring Boot
- **postgres**: Banco PostgreSQL
- **redis**: Cache Redis  
- **wiremock**: Mock da API CEP (desenvolvimento)


## Deploy AWS

### Infraestrutura
- **ECS Fargate**: Containers da aplicação
- **RDS PostgreSQL**: Banco de dados
- **ElastiCache Redis**: Cache
- **Application Load Balancer**: Load balancer

### Scripts
```bash
# Build e push ECR
./scripts/build.sh

# Deploy AWS
./scripts/deploy-aws.sh
```

## Monitoramento

### Métricas Disponíveis
- **cep.requests.total**: Total de requisições
- **cep.cache.hits**: Cache hits
- **cep.cache.misses**: Cache misses  
- **cep.lookup.time**: Tempo de consulta

### Health Checks
```bash
curl http://localhost:8080/actuator/health
```

## Funcionalidades Implementadas

### Consulta de CEP
- Validação de formato (8 dígitos)
- Integração com API externa via WebClient
- Tratamento de erros

### Cache Redis  
- Cache-aside pattern
- TTL configurável (padrão: 1 hora)
- Serialização JSON

### Auditoria
- Log de todas as consultas
- Captura IP e User-Agent
- Dados de performance (tempo execução)
- Persistência PostgreSQL

### Monitoramento
- Spring Actuator
- Métricas customizadas
- Health checks
- Logs estruturados

### Testes
- Testes unitários (Mockito)
- Testes de integração (TestContainers)
- Mock de API externa (WireMock)
- Cobertura de código (JaCoCo)

## Licença

Este projeto foi desenvolvido como parte de um teste técnico.