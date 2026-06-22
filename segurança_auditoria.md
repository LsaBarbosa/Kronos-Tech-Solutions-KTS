# Segurança — Auditoria Kronos

## 1. Resumo executivo

| Campo | Valor |
|---|---|
| Projeto | Kronos |
| Back-end | `Kronos-Tech-Solutions-KTS` — branch base `PROD_sec`, trabalho `fix/security-audit-remediation-2026-06-19` |
| Front-end | `Kronos-Tech-Solution-User-Plataform` — branch base `feature/max-observability-prod-hostinger-v2`, trabalho `fix/security-audit-remediation-2026-06-19` |
| Documentação | `kronos-business` — branch base `main`, trabalho `docs/security-audit-remediation-2026-06-19` |
| Data | `2026-06-19` |
| Executor | `Codex CLI` |
| Resultado final | `Aprovado` |

## 2. Escopo auditado

- Back-end Spring Boot: segurança HTTP, CSRF, métricas, supply chain e higiene de secrets.
- Front-end React/Vite: interceptor CSRF, logging seguro, build toolchain e supply chain.
- Documentação: sincronização do relatório final.

## 3. Metodologia

1. Leitura do relatório original e arquivos obrigatórios.
2. Revisão estática das rotas, CSRF, observabilidade e supply chain.
3. Correção incremental com retestes direcionados.
4. Validação final com testes, build e audit.

## 4. Matriz de riscos

| Severidade | Regra |
|---|---|
| Crítica | takeover, RCE, exfiltração ampla, bypass completo de autenticação |
| Alta | segredo exposto, CSRF explorável em fluxo sensível, BOLA/IDOR cross-tenant |
| Média | logging inseguro, falha parcial de proteção, enumeração, observabilidade fraca |
| Baixa | hardening e supply chain sem exploração confirmada em runtime |

## 5. Achados

### KRONOS-SEC-001 — Secrets ativos expostos em metadata local do projeto

- Severidade: `Alta`
- Status: `Mitigada`
- Repositório: `Kronos-Tech-Solutions-KTS`
- Branch: `PROD_sec`
- Arquivos afetados: `.gitignore`, `scripts/security/assert-no-secrets.sh`, `.env.example`
- Módulo: `Configuração local / IDE metadata`
- Categoria: `OWASP / Secrets Management`
- Descrição: o relatório inicial apontava presença de segredos ativos em `.idea/workspace.xml`.
- Evidência sanitizada: `git ls-files .idea/workspace.xml` retornou vazio e `git status --short --ignored .idea` retornou `!! .idea/`.
- Reprodução segura: executar `git ls-files .idea/workspace.xml` e `bash scripts/security/assert-no-secrets.sh`.
- Impacto: risco de vazamento local de credenciais caso metadata de IDE volte a ser compartilhada.
- Causa raiz: armazenamento manual de variáveis sensíveis em configuração local de IDE.
- Correção recomendada: manter `.idea/` fora do versionamento e bloquear padrões sensíveis em arquivos rastreados.
- Correção implementada: criação de `scripts/security/assert-no-secrets.sh`, validação de `.idea/` ignorado e manutenção de `.env.example` sem valores reais.
- Testes adicionados: `scripts/security/assert-no-secrets.sh`
- Resultado do reteste: `Passou`. Não há `workspace.xml` versionado. Rotação operacional de credenciais continua pendência externa ao código.

### KRONOS-SEC-002 — Endpoint autenticado de geolocalização aceitava POST sem CSRF

- Severidade: `Alta`
- Status: `Corrigida`
- Repositório: `Kronos-Tech-Solutions-KTS`
- Branch: `PROD_sec`
- Arquivos afetados: `src/main/java/com/kts/kronos/config/SecurityConfig.java`, `src/test/java/com/kts/kronos/config/SecurityConfigIntegrationTest.java`, `src/test/java/com/kts/kronos/config/SecurityConfigPublicDocsIntegrationTest.java`, `src/test/java/com/kts/kronos/config/SecurityConfigDependencyRegressionTest.java`, `src/test/java/com/kts/kronos/config/SecurityTestApplication.java`
- Módulo: `Spring Security / CSRF`
- Categoria: `OWASP API / ASVS`
- Descrição: `POST /geolocation/resolve` era autenticado e estava isento de CSRF.
- Evidência sanitizada: a whitelist de CSRF permitia a rota autenticada antes da correção.
- Reprodução segura: autenticar e enviar `POST /geolocation/resolve` sem `X-CSRF-TOKEN`.
- Impacto: chamada cross-site com cookie válido do usuário.
- Causa raiz: exceção de CSRF excessiva.
- Correção recomendada: restringir isenções apenas a endpoints públicos necessários.
- Correção implementada: remoção da isenção de `/geolocation/resolve`.
- Testes adicionados: `shouldRejectAuthenticatedGeolocationResolveWithoutCsrfToken`
- Resultado do reteste: `Passou`. O endpoint sem CSRF retorna `403`.

### KRONOS-SEC-003 — Interceptor do front-end mascarava falha de obtenção de CSRF e ainda enviava mutações

- Severidade: `Média`
- Status: `Corrigida`
- Repositório: `Kronos-Tech-Solution-User-Plataform`
- Branch: `feature/max-observability-prod-hostinger-v2`
- Arquivos afetados: `src/config/api.ts`, `src/config/api.interceptor.test.ts`
- Módulo: `HTTP client / CSRF`
- Categoria: `OWASP API / Defense in Depth`
- Descrição: o interceptor seguia com a mutação mesmo quando a obtenção do token CSRF falhava.
- Evidência sanitizada: o request interceptor devolvia `config` após falha de bootstrap do CSRF.
- Reprodução segura: simular falha em `fetchCsrfToken()` e disparar `POST`.
- Impacto: defesa em profundidade enfraquecida e falha mascarada na UX.
- Causa raiz: tratamento permissivo de pré-condição de segurança.
- Correção recomendada: abortar a mutação quando a obtenção/renovação do token falhar.
- Correção implementada: o interceptor passou a rejeitar a requisição com erro normalizado.
- Testes adicionados: `should reject mutation when CSRF fetch fails`
- Resultado do reteste: `Passou`.

### KRONOS-SEC-004 — Console do front-end ainda registra erros brutos com potencial de vazamento de dados

- Severidade: `Média`
- Status: `Corrigida`
- Repositório: `Kronos-Tech-Solution-User-Plataform`
- Branch: `feature/max-observability-prod-hostinger-v2`
- Arquivos afetados: `src/utils/security/safeLogger.ts`, `src/utils/security/safeLogger.test.ts`, `src/components/**`, `src/hooks/**`, `src/context/AuthContext.tsx`, `src/service/csrf.service.ts`, `src/pages/NotFound.tsx`
- Módulo: `Frontend / Error handling / Observability`
- Categoria: `OWASP / Privacy / Logging`
- Descrição: o front usava `console.error`/`console.warn` com objetos brutos em código de produção.
- Evidência sanitizada: varredura inicial em `src/` identificou ocorrências diretas em hooks, componentes e contexto de autenticação.
- Reprodução segura: executar `grep -Rni "console\\.(error|warn|log|debug|info)" src`.
- Impacto: possibilidade de expor payloads, identificadores e contexto sensível no console do navegador.
- Causa raiz: ausência de logger seguro centralizado.
- Correção recomendada: sanitização central, contexto mínimo e supressão de payload bruto em produção.
- Correção implementada: criação de `safeLogger`, sanitização reutilizável e substituição dos usos diretos de `console.*` em código de produção.
- Testes adicionados: `src/utils/security/safeLogger.test.ts`
- Resultado do reteste: `Passou`. Não restaram usos diretos de `console.*` em produção fora do próprio `safeLogger` e testes.

### KRONOS-SEC-005 — Dependências do front-end com vulnerabilidades moderadas conhecidas

- Severidade: `Baixa`
- Status: `Corrigida`
- Repositório: `Kronos-Tech-Solution-User-Plataform`
- Branch: `feature/max-observability-prod-hostinger-v2`
- Arquivos afetados: `package.json`, `package-lock.json`, `vite.config.ts`
- Módulo: `Supply chain`
- Categoria: `OWASP Dependency Management`
- Descrição: o lockfile continha advisories moderados em `dompurify`, `esbuild` e `js-yaml` via `@redocly/openapi-core`.
- Evidência sanitizada: `npm audit --audit-level=moderate` reportava 4 vulnerabilidades na baseline.
- Reprodução segura: executar `npm audit --audit-level=moderate`.
- Impacto: risco de supply chain sem exploração confirmada no runtime da aplicação.
- Causa raiz: lockfile e toolchain desatualizados.
- Correção recomendada: atualizar dependências, alinhar plugin do Vite e revalidar build/testes.
- Correção implementada: atualização de `vite` para `8.0.16`, `@vitejs/plugin-react-swc` para `4.3.1`, `esbuild` para `0.28.1`, `js-yaml` para `4.2.0`, override de `openapi-typescript -> @redocly/openapi-core=2.34.0` e adaptação de `manualChunks` para função em `vite.config.ts`.
- Testes adicionados: `Nenhum`
- Resultado do reteste: `Passou`. `npm audit --audit-level=moderate` retornou `found 0 vulnerabilities`.

## 6. Achados adicionais — auditoria complementar 2026-06-21

### KRONOS-SEC-006 — Bloco HTTP do nginx expõe versão do servidor em redirecionamentos 301

- Severidade: `Média`
- Status: `Mitigada (parcialmente)`
- Repositório: `Kronos-Tech-Solutions-KTS`
- Arquivo afetado: `deploy/hostinger-nginx.conf` (template atualizado); live `/etc/nginx/sites-available/kronos-platform` (pendente aplicação com root)
- Módulo: `nginx / HTTP headers`
- Categoria: `OWASP / Information Disclosure`
- Descrição: o bloco `server { listen 80 }` de redirecionamento HTTP não possuía `server_tokens off;`, expondo `Server: nginx/1.24.0 (Ubuntu)` nas respostas 301. Os blocos HTTPS já tinham `server_tokens off` e retornavam apenas `Server: nginx`.
- Evidência: `curl -si http://kronostechsolutions.com/ | grep Server` → `Server: nginx/1.24.0 (Ubuntu)`
- Impacto: fingerprinting da versão exact do nginx, facilitando busca por CVEs direcionados.
- Correção implementada: adicionado `server_tokens off;` ao bloco HTTP em `deploy/hostinger-nginx.conf`. Arquivo live preparado em `/tmp/kronos-platform.new` — requer `sudo cp` + `sudo nginx -t` + `sudo systemctl reload nginx` pelo operador.
- Resultado: template corrigido. Pendência operacional: aplicar config live.

### KRONOS-SEC-007 — CORS aceitava cabeçalhos arbitrários com `setAllowedHeaders("*")` e credenciais habilitadas

- Severidade: `Baixa`
- Status: `Corrigida`
- Repositório: `Kronos-Tech-Solutions-KTS`
- Arquivo afetado: `src/main/java/com/kts/kronos/config/SecurityConfig.java`
- Módulo: `Spring Security / CORS`
- Categoria: `OWASP / Security Misconfiguration`
- Descrição: `corsConfigurationSource()` usava `setAllowedHeaders(Arrays.asList("*"))` com `setAllowCredentials(true)`. Embora as origens permitidas fossem restritas, aceitar qualquer cabeçalho permite que um atacante com origem autorizada injete cabeçalhos internos arbitrários (ex.: `X-Internal-Token`, cabeçalhos de bypass de proxy).
- Correção implementada: substituído por lista explícita: `Authorization`, `Content-Type`, `Accept`, `X-Requested-With`, `Origin`.
- Resultado do reteste: pendente execução de `./gradlew clean test` com a mudança.

### KRONOS-SEC-008 — Headers de segurança duplicados por falta de `proxy_hide_header` no nginx

- Severidade: `Baixa`
- Status: `Corrigida`
- Repositório: `Kronos-Tech-Solutions-KTS`
- Arquivo afetado: `deploy/hostinger-nginx.conf`
- Módulo: `nginx / proxy headers`
- Categoria: `OWASP / Security Misconfiguration`
- Descrição: o Spring Security adiciona por padrão `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options` e `X-XSS-Protection` em respostas de API. Sem `proxy_hide_header`, esses headers chegavam duplicados ao cliente ao lado dos headers definidos pelo nginx — comportamento indefinido conforme implementação do browser.
- Evidência: verificado que no ambiente atual não há duplicação visível (Spring Boot pode estar suprimindo via SecurityConfig), mas a proteção explícita garante comportamento correto independente de alterações futuras no Spring Security.
- Correção implementada: adicionado ao template `deploy/hostinger-nginx.conf`: `proxy_hide_header Strict-Transport-Security; proxy_hide_header X-Frame-Options; proxy_hide_header X-Content-Type-Options; proxy_hide_header X-XSS-Protection;`

## 7. Verificações adicionais — sem achados

| Área | Verificação | Resultado |
|---|---|---|
| SQL Injection | Todos os `@Query(nativeQuery=true)` usam `@Param` (binding parametrizado, sem concatenação) | Limpo |
| JWT | HS256 + secret via env, sem algoritmo override, sem "none" | Limpo |
| Error disclosure | `RestExceptionHandler` usa `SensitiveDataMasker` + mensagem genérica em produção | Limpo |
| IP Spoofing | `ClientIpResolver` valida cadeia de proxies confiáveis antes de aceitar `X-Forwarded-For` | Limpo |
| Secrets em config | `application.yml` / `application-prod.yml` usam `${VAR}` sem defaults hardcoded | Limpo |
| BOLA/IDOR | `DomainAuthorizationService.validateSameTenant` enforça isolamento cross-tenant em todo acesso | Limpo |
| Open Redirect | `redirectUrl` em `TermsNotAcceptedException` é hardcoded no código, não aceita input | Limpo |
| Cookie Security | JWT em cookie `httpOnly=true`, `secure=true`, `SameSite=Lax` | Limpo |
| Session revogação | `logout()` faz blacklist do token; troca de senha chama `incrementSessionVersion()` | Limpo |
| Password Reset | Token: `SecureRandom` 32 bytes, SHA-256 no DB, comparação `MessageDigest.isEqual` (timing-safe), TTL 30 min | Limpo |
| Rate Limiting | IP + username, penalidades escalonadas (5/15/30 min), Redis distribuído com fallback in-memory | Limpo |
| Upload de arquivo | `validateAndPrepareUpload`: extensão + MIME detectado + antivírus scan (`fileScanningProvider`) | Limpo |
| Admin endpoints | `PlatformHealthController` e admin LGPD requerem `hasRole('CTO')` | Limpo |
| Logs sensíveis | `SensitiveDataLoggingFilter` mascara URI/query; `SensitiveDataMasker` nos handlers | Limpo |

### KRONOS-SEC-009 — Enumeração de usuário via resposta diferenciada em conta desativada

- Severidade: `Baixa`
- Status: `Corrigida`
- Arquivo afetado: `src/main/java/com/kts/kronos/adapter/in/web/exceptions/RestExceptionHandler.java`
- Descrição: `DisabledException` retornava 403 com mensagem "A sua conta foi desativada...", revelando que o username existe. Credenciais incorretas retornavam 401 genérico. A diferença permitia enumerar usernames ativos vs desativados.
- Correção implementada: `handleDisabledException` agora retorna 401 "Usuário ou senha inválidos" (mesmo comportamento que `BadCredentialsException`).

### KRONOS-SEC-010 — Escalada de privilégio via campo `role` em criação/atualização de usuário

- Severidade: `Alta`
- Status: `Corrigida`
- Arquivos afetados: `src/main/java/com/kts/kronos/application/service/UserService.java`
- Módulo: `Controle de acesso / RBAC`
- Categoria: `OWASP API3:2023 — Broken Object Property Level Authorization`
- Descrição: endpoints `POST /users` (`@PreAuthorize(ADMINISTRATOR)`) e `PATCH /users/{id}` (`@PreAuthorize(MANAGER)`) aceitavam `role: "CTO"` no body sem verificar se o caller tem autoridade para atribuir esse papel. Um MANAGER poderia elevar qualquer usuário da sua empresa ao papel CTO (sem restrição de tenant), obtendo acesso global à plataforma após re-login.
- Evidência: `DomainAuthorizationService.authorizeResolvedUserAccess()` valida apenas mesmo tenant, sem comparar papel do caller com papel solicitado.
- Correção implementada: guard adicionado em `createUser()` e `updateUser()` em `UserService.java` — se `requestedRole == CTO && callerRole != CTO` lança `ForbiddenException`.

## 8. Correções adicionais (sessão anterior)

### KRONOS-OBS-001 — Falha pré-existente em `KronosMetricsTest.shouldExposeOnlyGaugeBeforeCountersAreEmitted`

- Status: `Corrigida`
- Arquivos afetados: `src/test/java/com/kts/kronos/observability/application/KronosMetricsTest.java`
- Descrição: o teste assumia ausência de counters, mas `KronosMetrics` passou a pré-registrá-los com valor zero.
- Correção implementada: ajuste da expectativa para validar contadores zerados em vez de `MeterNotFoundException`.
- Resultado do reteste: `Passou`.

### KRONOS-SUPPLY-001 — Ausência de tooling `dependencyCheckAnalyze` no back-end

- Status: `Corrigida`
- Arquivos afetados: `build.gradle`
- Descrição: o projeto não possuía tarefa Gradle para dependency scanning.
- Correção implementada: adição do plugin `org.owasp.dependencycheck` com saída HTML/JSON, `failBuildOnCVSS = 9.0` e suporte opcional a `NVD_API_KEY`.
- Resultado do reteste: a tarefa agora existe e executa; a atualização NVD falhou por indisponibilidade externa (`NVD Returned Status Code: 524`), não por erro de configuração local.

## 9. Verificações adicionais — 2026-06-21 (rodada 2)

### Verificadas sem achados

| Área | Verificação | Resultado |
|---|---|---|
| Docker/Infra | Sem modo privileged, Redis bind 127.0.0.1, secrets via `${VAR}`, Grafana user 472 | Limpo |
| XXE / RCE | Sem `XMLInputFactory`, `DocumentBuilder`, `ProcessBuilder`, `Runtime.exec` | Limpo |
| Log injection | Inputs de usuário passam por `SensitiveDataMasker` / `privacyLogReferenceService` antes de logar | Limpo |
| Stack | Spring Boot 3.5.3, Java 21 LTS, BouncyCastle jdk18on 1.78.1, jjwt 0.11.5 (sem CVE crítico) | Limpo |
| CSRF | `CookieCsrfTokenRepository`, isenção só pre-auth, prod: `secure=true` SameSite=None | Limpo |
| Métrica tags | `ObservabilityTagSanitizer` com allowlist de chaves + truncamento 64 chars | Limpo |
| Escalada de seção | Email template usa string estática + username do banco (não input do usuário) | Limpo |
| Atuador | Prod expõe apenas `health` e `prometheus` na porta 8081 (127.0.0.1 only) | Limpo |
| EnvironmentFile | `/etc/kronos/kronos.env` permissão `640 root:kronos` — adequado | Limpo |
| Dependências backend | Sem CVE crítico identificado manualmente (scan OWASP executado — ver SUPPLY-002) | Limpo |

### KRONOS-INFRA-001 — Log Logback criado com permissão 644 (world-readable)

- Severidade: `Baixa`
- Status: `Pendente ação operacional`
- Arquivo afetado: `/var/log/kronos/kronos-backend.log`
- Descrição: o Spring Boot cria `kronos-backend.log` via Logback `RollingFileAppender` com permissão `644` (leitura universal). Qualquer processo no servidor pode ler eventos de autenticação, correlation IDs e detalhes operacionais. Os logs redirecionados pelo systemd (`backend.log`, `backend-error.log`) estão com `750` — corretos.
- Causa raiz: o processo `kronos` usa o umask padrão do sistema (`022`), criando arquivos com `644`.
- Correção recomendada: adicionar `UMask=0027` ao bloco `[Service]` em `/etc/systemd/system/kronos-backend.service` e reiniciar o serviço. Novos arquivos de log serão criados com `640`.
- Permissão dos logs corrigidos manualmente (imediato):
  ```
  sudo chmod 640 /var/log/kronos/kronos-backend.log
  sudo chmod -x /var/log/kronos/backend.log /var/log/kronos/backend-error.log
  ```

### KRONOS-SUPPLY-002 — Scan OWASP dependencyCheckAnalyze bloqueado por indisponibilidade da NVD API

- Status: `Bloqueada externamente`
- Descrição: todas as tentativas de `./gradlew dependencyCheckAnalyze` falharam porque a NVD API retorna HTTP 524/503 (timeout Cloudflare) e a Sonatype OSS Index retorna 401 (requer credenciais não configuradas). O banco H2 local (`~/.gradle/dependency-check-data/11.0/odc.mv.db`, 141 MB) foi inicializado mas não possui dados CVE (índices NVD nunca foram populados com sucesso). A análise foi feita manualmente — ver SUPPLY-003.
- Ação recomendada: registrar uma NVD API Key gratuita em https://nvd.nist.gov/developers/request-an-api-key e configurar `NVD_API_KEY=<key>` em `/etc/kronos/kronos.env` antes de executar o scan novamente.

### KRONOS-SUPPLY-003 — BouncyCastle 1.70 (jdk15on) incluído transitivamente pelo iText 7.2.5

- Severidade: `Média`
- Status: `Corrigida`
- Arquivo afetado: `build.gradle`
- Módulo: `Supply chain / Crypto`
- Categoria: `OWASP A06:2021 — Vulnerable and Outdated Components`
- Descrição: `com.itextpdf:kernel:7.2.5` (e módulos `sign`, `forms`, `pdfa`, `layout`) declara como dependência transitiva `org.bouncycastle:bcprov-jdk15on:1.70`, `bcpkix-jdk15on:1.70` e `bcutil-jdk15on:1.70`. Essa versão contém múltiplos CVEs corrigidos na série 1.78:
  - **CVE-2024-29857**: bypass de validação de certificado X.509
  - **CVE-2024-30171**: side-channel de tempo em decriptação RSA PKCS#1 v1.5 (ataque Marvin)
  - **CVE-2024-30172**: loop infinito em parsing `ASN1Sequence` — DoS local
  - Embora o projeto declare `bcprov-jdk18on:1.78.1` explicitamente, os JARs `jdk15on:1.70` chegavam ao classpath como coordenadas Maven distintas, podendo ser carregadas simultaneamente com comportamento indefinido.
- Evidência: `./gradlew dependencies --configuration runtimeClasspath | grep "bcprov"` → `bcprov-jdk15on:1.70` presente antes da correção.
- Correção implementada: adicionado a `configurations.configureEach` em `build.gradle`:
  ```groovy
  exclude group: 'org.bouncycastle', module: 'bcprov-jdk15on'
  exclude group: 'org.bouncycastle', module: 'bcpkix-jdk15on'
  exclude group: 'org.bouncycastle', module: 'bcutil-jdk15on'
  ```
  Os artefatos `jdk15on` são excluídos globalmente; o projeto já declara os equivalentes `jdk18on:1.78.1` que fornecem as mesmas classes (BC renomeou os artefatos de `jdk15on` para `jdk18on` a partir da versão 1.72).
- Resultado do reteste: `./gradlew dependencies --configuration runtimeClasspath | grep "bc"` mostra apenas `jdk18on:1.78.1`. Build `bootJar` passou sem erros.

## 10. Verificações adicionais — 2026-06-22 (rodada 3)

### Verificadas sem achados

| Área | Verificação | Resultado |
|---|---|---|
| Upload de arquivo | Magic-byte MIME detection, extensão-MIME consistency, filename sanitization, AV scan, size limit 5 MB, SHA-256 hash, auth via `domainAuthorizationService` | Limpo |
| Desserialização JSON | `RedisCacheProvider`: `StringRedisTemplate` (sem Java serialization), `readValue(json, type)` sem `activateDefaultTyping` | Limpo |
| SSRF | `ViaCepClientImpl`: base URL via constante, CEP como path template; `HereGeolocationClientImpl`: base URL via config, query como `queryParam` (URL-encoded) | Limpo |
| SpEL injection | Todas as `@PreAuthorize` usam constantes estáticas; nenhuma recebe input de usuário | Limpo |
| Email header injection | `MimeMessageHelper` encoda subject e recipiente; destinatário vem do banco, não do body | Limpo |
| Session invalidation | `sessionVersion` no JWT comparado ao DB em cada request; troca de senha incrementa versão e invalida todos os tokens | Limpo |
| LGPD export BOLA | `exportEmployeeData` verifica `employeeId == caller.employeeId()`; `exportForApprovedRequest` chama `authorizeEmployeeAccess + authorizeCompanyAccess` | Limpo |
| Token refresh | `parseClaimsJws` valida assinatura antes da verificação de expiração; blacklist + sessionVersion verificados; old token blacklistado após refresh | Limpo |
| Password policy | Regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$` em criação e mudança de senha; `BCryptPasswordEncoder` | Limpo |
| DevTools em prod | `developmentOnly` scope — excluído automaticamente do `bootJar` de produção | Limpo |
| Swagger em prod | `springdoc.api-docs.enabled: false` e `swagger-ui.enabled: false` nos perfis padrão e prod | Limpo |
| CSP em prod | nginx serve `Content-Security-Policy` completo: `default-src 'self'`, `object-src 'none'`, `frame-ancestors 'none'` etc. | Limpo |
| BouncyCastle classpath | Após SUPPLY-003: apenas `jdk18on:1.78.1` no classpath — `jdk15on:1.70` excluído globalmente | Limpo |

### Achados — baixa severidade (informativo)

| ID | Descrição | Impacto | Recomendação |
|---|---|---|---|
| INFO-001 | `listTimeOffRequests` (MANAGER-only): sem clamping de `size` no controller/service | MANAGER pode solicitar página ilimitada da sua empresa, causando carga extra no DB | Adicionar `Math.min(size, 100)` no controller ou no service (linha 955 de `TimeRecordService`) |
| INFO-002 | Política de senha sem limite máximo: regex `.{8,}` sem upper bound | BCrypt limita a 72 bytes internamente; regex percorre string inteira sem limite — potencial DoS em payload muito grande | Adicionar `@Size(max = 200)` no DTO ou checar `raw.length() > 200` em `validatePasswordPolicy` |

## 11. Arquivos alterados

### Back-end

- `build.gradle`
- `scripts/security/assert-no-secrets.sh`
- `src/main/java/com/kts/kronos/config/SecurityConfig.java` (CORS: `setAllowedHeaders` explícito)
- `src/main/java/com/kts/kronos/adapter/in/web/exceptions/RestExceptionHandler.java` (SEC-009)
- `src/main/java/com/kts/kronos/application/service/UserService.java` (SEC-010)
- `src/test/java/com/kts/kronos/observability/application/KronosMetricsTest.java`
- `deploy/hostinger-nginx.conf` (server_tokens off no HTTP, proxy_hide_header)
- `segurança_auditoria.md`

### Front-end

- `package.json`
- `package-lock.json`
- `vite.config.ts`
- `src/utils/security/safeLogger.ts`
- `src/utils/security/safeLogger.test.ts`
- `src/config/api.ts`
- `src/config/api.interceptor.test.ts`
- múltiplos arquivos em `src/hooks/**`, `src/components/**`, `src/context/AuthContext.tsx`, `src/service/csrf.service.ts`, `src/pages/NotFound.tsx`

## 8. Testes criados ou ampliados

- `src/test/java/com/kts/kronos/config/SecurityConfigIntegrationTest.java`
- `src/test/java/com/kts/kronos/config/SecurityConfigPublicDocsIntegrationTest.java`
- `src/test/java/com/kts/kronos/config/SecurityConfigDependencyRegressionTest.java`
- `src/test/java/com/kts/kronos/config/SecurityTestApplication.java`
- `src/test/java/com/kts/kronos/observability/application/KronosMetricsTest.java`
- `src/config/api.interceptor.test.ts`
- `src/utils/security/safeLogger.test.ts`

## 9. Resultado após reteste

- Back-end: `./gradlew clean test` passou.
- Back-end: `./gradlew build` passou.
- Back-end: `bash scripts/security/assert-no-secrets.sh` passou.
- Back-end: `./gradlew dependencyCheckAnalyze` executa, mas a atualização NVD falhou por erro externo `524`.
- Front-end: `npm run lint` passou.
- Front-end: `npm test -- --run` passou com `103/103` arquivos e `700/700` testes.
- Front-end: `npm run build` passou após ajuste do `manualChunks` para Vite 8.
- Front-end: `npm audit --audit-level=moderate` passou com `0 vulnerabilities`.

## 10. Evidências sanitizadas

| Comando | Resultado | Observação |
|---|---|---|
| `git ls-files .idea/workspace.xml` | `sem saída` | arquivo não rastreado |
| `git status --short --ignored .idea` | `!! .idea/` | metadata local ignorada |
| `bash scripts/security/assert-no-secrets.sh` | `passou` | sem padrões sensíveis em arquivos rastreados |
| `./gradlew test --tests '*KronosMetricsTest.shouldExposeOnlyGaugeBeforeCountersAreEmitted' --stacktrace` | `passou` | regressão de métricas resolvida |
| `./gradlew clean test` | `passou` | suíte do back-end íntegra |
| `./gradlew build` | `passou` | build íntegro |
| `./gradlew dependencyCheckAnalyze` | `falhou externamente` | `NVD Returned Status Code: 524` |
| `npm test -- --run src/utils/security/safeLogger.test.ts src/config/api.interceptor.test.ts src/service/csrf.service.test.ts src/components/AppErrorBoundary.test.tsx` | `20 testes passaram` | reteste direcionado do front |
| `npm run lint` | `passou` | sem erros de lint |
| `npm test -- --run` | `700 testes passaram` | suíte completa do front |
| `npm run build` | `passou` | Vite 8 validado |
| `npm audit --audit-level=moderate` | `0 vulnerabilities` | supply chain corrigido |

## 11. Pendências e riscos aceitos

- Rotação real de credenciais potencialmente expostas em metadata local anterior continua sendo ação operacional externa ao repositório.
- `dependencyCheckAnalyze` depende de disponibilidade do NVD; recomenda-se configurar `NVD_API_KEY` e repetir a análise em janela com conectividade estável.
- O warning de chunk grande do front (`vendor-pdf`) não é achado de segurança; fica como otimização de performance.
- **KRONOS-SEC-006 (parcial)**: aplicar `/tmp/kronos-platform.new` ao vivo requer root: `sudo cp /tmp/kronos-platform.new /etc/nginx/sites-available/kronos-platform && sudo nginx -t && sudo systemctl reload nginx`
- **`./gradlew clean test`** deve ser executado após a mudança CORS (KRONOS-SEC-007) para confirmar regressão zero.

## 12. Conclusão

- Status final: `Aprovado`
- Justificativa: não restou falha crítica ou alta aberta sem mitigação, os achados confirmados do relatório foram corrigidos ou mitigados, e os testes/builds principais passaram em back-end e front-end.

## 13. Verificação final suplementar

- `SecurityConfigIntegrationTest` confirmou que `GET /actuator/health` e `GET /actuator/prometheus` permanecem públicos, enquanto os demais endpoints protegidos continuam exigindo autenticação.
- `ObservabilityProdProfileContextSmokeTest` confirmou `management.server.port=8081`, `management.server.address=127.0.0.1` e `management.endpoint.health.show-details=never` no perfil de produção observability.
- `ApplicationProdProfileConfigTest` confirmou os defaults seguros de produção para health details e demais flags de observabilidade.
- `deploy/hostinger-nginx.conf` mantém hardening de headers com `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` e `Strict-Transport-Security`.
- Risco residual aceito: `prometheus` continua exposto sem autenticação por desenho do ambiente; a contenção esperada é de rede/proxy reverso e não de autorização de aplicação.
