# Kronos Redis — pacote de execução para CODEX

## Objetivo

Implementar Redis nos endpoints específicos do Kronos, com Redis rodando localmente dentro da VPS Hostinger em produção, sem alterar contratos HTTP do front-end e sem substituir PostgreSQL como fonte da verdade.

## Repositórios e branches alvo

| Repositório | Branch | Papel |
|---|---|---|
| `LsaBarbosa/Kronos-Tech-Solutions-KTS` | `prod-redis` | Implementação principal Redis no back-end |
| `LsaBarbosa/Kronos-Tech-Solution-User-Plataform` | `PROD_HOSTINGER_v2` | Validação de contrato/front, sem Redis no browser |
| `LsaBarbosa/kronos-business` | `main` | Fonte documental de regras, fluxos e arquitetura |

## Leitura obrigatória antes de codar

### Back-end

1. `build.gradle`
2. `src/main/resources/application.yml`
3. `src/main/resources/application-prod.yml`
4. `src/main/resources/application-test.yml`, se existir
5. `docker-compose.yml`, `Dockerfile`, `.env.example`, `deploy/hostinger-nginx.conf`, se existirem
6. `src/main/java/com/kts/kronos/application/security/AuthenticationRateLimitService.java`
7. `src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java`
8. `src/main/java/com/kts/kronos/application/service/AuthService.java`
9. `src/main/java/com/kts/kronos/adapter/out/security/JwtAuthenticationFilter.java`
10. `src/main/java/com/kts/kronos/adapter/out/security/AuthCookieService.java`
11. `src/main/java/com/kts/kronos/adapter/out/security/JwtUtils.java`
12. `src/main/java/com/kts/kronos/application/port/out/provider/TokenBlacklistProvider.java`
13. `src/main/java/com/kts/kronos/application/port/out/provider/PasswordResetTokenProvider.java`
14. Implementações JPA atuais de blacklist e reset token
15. `UserService`, `EmployeeService`, `CompanyService`, `DashboardService`, `TimeRecordService`, `PublicPrivacyService`, `GeolocationService`, `AcceptTermsService`
16. `KronosMetrics`, `KronosTracing`, `PlatformHealthService`

### Front-end

1. `package.json`
2. Configuração Axios/API client
3. Configuração TanStack Query
4. Chamadas para `/auth/*`, `/records/*`, `/dashboard/summary`, `/users/own-profile`, `/employee/own-profile`
5. `docs/openapi/flag-redis.openapi.json`, se existir

### Documentação

1. Arquitetura de pastas e arquitetura do projeto
2. Fluxos de aplicação
3. Regras de negócio
4. Entradas e saídas por fluxo
5. Entidades
6. Documento mais recente de estado atual da branch `PROD_HOSTINGER_V2`, se existir no `kronos-business/main`

## Arquivos deste pacote

| Arquivo | Função |
|---|---|
| `00-contexto-observado.md` | Contexto técnico já observado e decisões obrigatórias |
| `rules/redis-architecture-rules.md` | Regras arquiteturais para Redis no Kronos |
| `rules/security-lgpd-observability-rules.md` | Regras de segurança, LGPD e observabilidade |
| `skills/redis-spring-boot-skill.md` | Skill de implementação Redis/Spring Boot |
| `skills/hostinger-redis-prod-skill.md` | Skill de deploy Redis local na VPS Hostinger |
| `agents/*.md` | Agentes principais para execução/revisão |
| `subagents/*.md` | Subagentes especializados por área |
| `plan/redis-action-plan.md` | Plano de ação cronológico com tarefas e critérios de aceite |
| `prompts/CODEX_REDIS_IMPLEMENTATION_PROMPT.md` | Prompt principal para colar no CODEX |
| `checklists/review-checklist.md` | Checklist final de revisão técnica |

## Decisão central

Redis deve ser usado como infraestrutura auxiliar para:

- rate limit distribuído;
- tokens temporários de recuperação de senha;
- blacklist de JWT com TTL;
- cache-aside de consultas caras e seguras;
- locks/idempotência de curta duração;
- cache de integrações externas, como geolocalização.

Redis não deve armazenar como fonte primária:

- registros de ponto;
- NSR;
- AFD/AEJ;
- documentos;
- auditoria legal;
- consentimentos legais;
- dados LGPD duráveis;
- imagens biométricas.
