# Kronos Back-end

API corporativa da plataforma Kronos, responsável por autenticação, regras de negócio, persistência, integrações externas, observabilidade e serviços críticos de jornada e conformidade.

## Visão Geral

Este repositório implementa o núcleo transacional da plataforma Kronos. A aplicação expõe contratos HTTP consumidos pelo front-end, executa regras de domínio sensíveis e centraliza integrações com banco de dados, Redis, serviços de e-mail, geolocalização, armazenamento documental e biometria.

O objetivo desta base é manter consistência operacional, segurança e rastreabilidade em fluxos que afetam jornada, documentos, perfis, autenticação e requisitos regulatórios.

## Responsabilidades do Repositório

- autenticação e autorização;
- emissão, validação e revogação de sessão/token;
- registro de ponto e fluxos associados;
- dashboard e consultas operacionais;
- gestão de usuários, colaboradores e empresas;
- documentos, assinatura e trilhas de auditoria;
- políticas de privacidade e fluxos LGPD;
- rate limiting, cache e infraestrutura Redis;
- observabilidade, métricas e saúde da plataforma.

## Stack Principal

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Build | Gradle |
| API | Spring Web MVC |
| Segurança | Spring Security + JWT + cookie HttpOnly + CSRF |
| Persistência | Spring Data JPA + PostgreSQL + Flyway |
| Cache e suporte distribuído | Spring Cache + Spring Data Redis |
| Observabilidade | Actuator + Micrometer + Prometheus + OpenTelemetry |
| Testes | JUnit 5 + Spring Test + Mockito + Testcontainers |

## Arquitetura da Aplicação

A estrutura principal segue separação por camadas e responsabilidades:

```text
src/main/java/com/kts/kronos/
  adapter/         entrada e saída da aplicação
  application/     serviços, casos de uso e portas
  config/          configuração técnica
  constants/       constantes e caminhos compartilhados
  domain/          regras e modelos de domínio
  infrastructure/  integrações e suporte técnico
  observability/   métricas, tracing e saúde
```

## Integrações Relevantes

- PostgreSQL como fonte primária de dados;
- Redis para cache, TTL, blacklist, rate limit e suporte distribuído;
- AWS S3 para armazenamento documental;
- AWS Rekognition para fluxos biométricos;
- SMTP para notificações e recuperação de acesso;
- HERE para geolocalização, quando habilitado.

## Execução Local

### Pré-requisitos

- Java 21;
- Docker e Docker Compose;
- variáveis de ambiente configuradas a partir de [`.env.example`](/home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS/.env.example).

### Infraestrutura local

O repositório já possui `docker-compose.yml` para PostgreSQL e Redis.

```bash
docker compose up -d
```

### Subida da aplicação

```bash
./gradlew bootRun
```

A aplicação usa `SERVER_PORT=8080` no exemplo de ambiente padrão.

## Qualidade e Validação

Comandos principais:

```bash
./gradlew test
./gradlew build
./gradlew jacocoTestReport
```

Comandos auxiliares:

```bash
./gradlew unitTest
./gradlew dataJpaTest
./gradlew jacocoTestCoverageVerification
```

## Segurança e Conformidade

Diretrizes operacionais desta base:

- não expor segredos reais em arquivos versionados;
- não registrar tokens, payloads sensíveis ou dados biométricos em texto puro;
- não usar Redis como fonte primária para dados regulatórios ou permanentes;
- preservar trilhas de auditoria e consistência dos contratos expostos;
- validar impactos de LGPD, autenticação e retenção em qualquer mudança sensível.

## Observabilidade

O projeto já inclui componentes para:

- health checks;
- métricas Prometheus;
- tracing com OpenTelemetry;
- artefatos locais de observabilidade em `infra/`, `prometheus/`, `loki/` e `tempo/`.

## Estrutura do Repositório

```text
src/          código-fonte e testes
deploy/       artefatos de entrega e apoio operacional
docs/         documentação complementar
infra/        configuração de observabilidade e suporte
storage/      área local de documentos quando aplicável
```

## Dependências de Ecossistema

Este repositório trabalha em conjunto com:

- `../Kronos-Tech-Solution-User-Plataform`: front-end web da plataforma;
- `../kronos-business`: documentação funcional, técnica e arquitetural.

Mudanças de contrato devem ser refletidas de forma coordenada entre esses repositórios.

## Fluxo de Colaboração

1. validar impacto de domínio e contrato;
2. implementar mantendo compatibilidade externa quando exigido;
3. executar testes adequados ao escopo;
4. revisar segurança, observabilidade e conformidade;
5. atualizar documentação correlata quando a mudança alterar comportamento público ou operacional.

## Licença e Uso

Uso interno do ecossistema Kronos. Qualquer distribuição externa deve seguir aprovação formal e política de governança da organização.
