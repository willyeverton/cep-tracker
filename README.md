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
- **TestContainers** (Redis)
- **H2** (banco de testes)

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
| `PROFILE` | `padrão` | Perfil ativo |
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host Redis |
| `CEP_API_URL` | `https://viacep.com.br` | URL API CEP |

### Profiles

- **`padrão`**: Configuração base (application.yml)
- **`docker`**: Containers Docker (application-docker.yml)
- **`aws`**: Produção AWS (application-aws.yml)
- **`test`**: Testes automatizados (application-test.yml)

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
│   └── config/                 # Configurações
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
- Tratamento de erros robusto
- Timeout configurável (5000ms)

### Cache Redis  
- Cache-aside pattern
- TTL configurável (padrão: 1 hora)
- Serialização JSON automática
- Namespace `cep:` para organização

### Auditoria
- Log de todas as consultas
- Captura IP e User-Agent automaticamente
- Dados de performance (tempo execução)
- Persistência PostgreSQL
- Endpoints para consulta de logs

### Monitoramento
- Spring Actuator habilitado
- Métricas customizadas com Micrometer
- Health checks automáticos
- Logs estruturados com SLF4J

### Testes
- Testes unitários com Mockito
- Testes de integração com TestContainers (Redis)
- Mock de dependências com @MockBean
- Cobertura de código com JaCoCo
- Banco H2 para testes

### Tratamento de Erros
- Global Exception Handler
- Validação Bean Validation
- Status HTTP apropriados
- Mensagens de erro estruturadas

## Ambientes

### Desenvolvimento Local
```bash
# Usar perfil padrão (application.yml)
./mvnw spring-boot:run

# A aplicação usa:
# - PostgreSQL local (localhost:5432)
# - Redis local (localhost:6379)  
# - API ViaCEP real (https://viacep.com.br)
```

### Desenvolvimento com Docker
```bash
# Usar perfil docker
docker-compose up -d

# A aplicação usa:
# - PostgreSQL container (postgres:5432)
# - Redis container (redis:6379)
# - WireMock container (wiremock:8080)
```

### Produção AWS
```bash
# Usar perfil aws
export PROFILE=aws
java -jar target/cep-tracker-*.jar

# A aplicação usa:
# - RDS PostgreSQL
# - ElastiCache Redis
# - API ViaCEP real
```

### Testes Automatizados
```bash
# Usar perfil test
./mvnw test

# Os testes usam:
# - H2 em memória
# - TestContainers Redis
# - @MockBean para API externa
```

## Configurações Importantes

### Cache
```yaml
app:
  cep-service:
    cache:
      ttl: 3600  # TTL em segundos (1 hora)
```

### API Externa
```yaml
app:
  cep-service:
    external-api:
      base-url: https://viacep.com.br
      timeout: 5000  # Timeout em millisegundos
```

### Banco de Dados
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Criação automática das tabelas
    show-sql: false     # Logs SQL (dev: true, prod: false)
```

### Monitoramento
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

## Troubleshooting

### Problemas Comuns

#### Aplicação não inicia
```bash
# Verificar se Java 11+ está instalado
java -version

# Verificar se PostgreSQL está rodando
docker-compose ps postgres

# Verificar logs
docker-compose logs -f app
```

#### Cache não funciona
```bash
# Verificar se Redis está rodando
docker-compose ps redis

# Testar conexão Redis
redis-cli ping

# Verificar configuração
curl http://localhost:8080/actuator/health
```

#### Testes falham
```bash
# Limpar e recompilar
./mvnw clean compile

# Executar testes específicos
./mvnw test -Dtest=CepServiceImplTest

# Verificar se Docker está rodando (TestContainers)
docker ps
```

## Performance

### Métricas de Cache
- **Hit Rate**: ~80% após aquecimento
- **TTL**: 1 hora (configurável)
- **Namespace**: `cep:*`

### Métricas de API
- **Timeout**: 5000ms
- **Retry**: Não configurado
- **Circuit Breaker**: Não implementado

### Métricas de Banco
- **Connection Pool**: HikariCP
- **Pool Size**: 10 (docker), 20 (aws)
- **DDL**: update (dev), validate (prod)

## Segurança

### Configurações
- **Headers HTTP**: Captura User-Agent
- **IP Tracking**: X-Forwarded-For, X-Real-IP
- **Validação**: Bean Validation (@Pattern)
- **Sanitização**: Input validation apenas

### Limitações
- **Rate Limiting**: Não implementado
- **Autenticação**: Não implementada  
- **Autorização**: Não implementada
- **HTTPS**: Configurável via proxy/load balancer

## Licença

Este projeto foi desenvolvido como parte de um teste técnico da Stefanini.

---

## Links Úteis

- **Documentação Técnica**: [DOCUMENTATION.md](DOCUMENTATION.md)
- **Spring Boot**: https://spring.io/projects/spring-boot
- **ViaCEP API**: https://viacep.com.br/
- **Docker Compose**: https://docs.docker.com/compose/
- **PostgreSQL**: https://www.postgresql.org/
- **Redis**: https://redis.io/

## Contato

Para dúvidas sobre a implementação, consulte a documentação técnica ou os comentários no código.

---

## 🎯 **Scripts Executáveis:**

```bash
# Tornar scripts executáveis
chmod +x scripts/build.sh
chmod +x scripts/deploy-aws.sh
```