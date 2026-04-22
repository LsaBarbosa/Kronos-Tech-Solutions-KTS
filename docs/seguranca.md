## Objetivo
Documentar os principais pontos de melhoria de cyber security / anti-hacking identificados na branch `main`, com foco em exposição de segredos, autenticação, autorização, superfície operacional e endurecimento de configuração.

## Escopo inspecionado
- `build.gradle`
- `src/main/resources/application.yml`
- `src/main/java/com/kts/kronos/config/SecurityConfig.java`
- `src/main/java/com/kts/kronos/adapter/out/security/JwtUtils.java`
- `src/main/java/com/kts/kronos/adapter/out/security/JwtAuthenticationFilter.java`
- `src/main/java/com/kts/kronos/adapter/out/security/TermsValidationFilter.java`
- `src/main/java/com/kts/kronos/adapter/out/security/CustomUserDetailsService.java`
- `src/main/java/com/kts/kronos/adapter/in/web/exceptions/DelegatedAuthenticationEntryPoint.java`
- `src/main/java/com/kts/kronos/KronosApplication.java`
- `.gitignore`

---

## Checklist executivo

### [CRITICAL] 1. Material criptográfico e senha de certificado definidos em configuração versionada
**Evidência**
No `application.yml` existe bloco de certificado com `path` e `password` definidos no arquivo.

**Risco**
- Incentiva versionamento acidental de segredo real.
- Facilita vazamento por commit, fork, backup ou log de configuração.
- Amplia impacto em caso de acesso indevido ao repositório.

**Ação corretiva**
- Remover senha e caminho fixo do arquivo versionado.
- Ler esses valores de Secret Manager / Vault / AWS Secrets Manager.
- Adicionar validação para falhar na inicialização se o segredo não estiver configurado.
- Revisar histórico do Git para confirmar que nenhum segredo real já foi commitado.

**Prioridade**: Imediata.

---

### [HIGH] 2. `.gitignore` não protege artefatos e arquivos sensíveis comuns
**Evidência**
`.gitignore` atual não cobre `.env`, `.pfx`, `.jks`, `.pem`, `.key`, `.crt`, `secrets.*` e variações.

**Risco**
- Comitar acidentalmente credenciais, certificados e chaves privadas.
- Vazamento de segredos em pull requests e histórico do repositório.

**Ação corretiva**
Adicionar entradas como:
```gitignore
.env
.env.*
*.pfx
*.p12
*.jks
*.pem
*.key
*.crt
secrets.*
```
Também habilitar secret scanning e varredura no pipeline.

**Prioridade**: Alta.

---

### [HIGH] 3. Logging excessivo de segurança e SMTP
**Evidência**
`application.yml` define:
- `org.springframework.security: DEBUG`
- `spring.mail.properties.mail.smtp.debug: true`

**Risco**
- Exposição de detalhes de autenticação, fluxo de filtros e falhas internas.
- Geração de logs úteis para enumeração e troubleshooting ofensivo.
- Possível vazamento operacional de SMTP.

**Ação corretiva**
- Em produção, trocar para `INFO` ou `WARN`.
- Separar logging de segurança com mascaramento de dados.
- Manter `DEBUG` apenas localmente e com profile específico.

**Prioridade**: Alta.

---

### [HIGH] 4. Decisão de acesso condicionada à claim `terms_accepted` do JWT
**Evidência**
`JwtUtils` emite a claim `terms_accepted` no token e `TermsValidationFilter` usa essa claim para permitir ou bloquear acesso.

**Risco**
- Estado de autorização pode ficar desatualizado até expiração do token.
- Mudanças de aceite no banco não invalidam automaticamente tokens já emitidos.
- A decisão fica presa a dado transportado no token, não necessariamente ao estado mais atual do usuário.

**Ação corretiva**
- Validar aceite em fonte server-side confiável.
- Adotar `tokenVersion`, `jti` revogável, ou introspecção/revalidação por evento sensível.
- Reemitir / invalidar tokens quando o estado de aceite mudar.

**Prioridade**: Alta.

---

### [HIGH] 5. JWT sem atributos adicionais de endurecimento e sem revogação
**Evidência**
`JwtUtils` gera token com subject, claims, issuedAt e expiration, mas não há evidência de `issuer`, `audience`, `jti`, `not-before`, blacklist ou versionamento de token.

**Risco**
- Replay facilitado.
- Revogação difícil em caso de comprometimento.
- Maior risco de aceitação indevida entre ambientes se houver configuração inadequada.

**Ação corretiva**
- Incluir `iss`, `aud`, `jti`, e opcionalmente `nbf`.
- Implementar estratégia de revogação ou versionamento por usuário.
- Segregar segredo JWT por ambiente.

**Prioridade**: Alta.

---

### [MEDIUM] 6. Resposta 401 pode propagar detalhes internos da autenticação
**Evidência**
`DelegatedAuthenticationEntryPoint` usa `authException.getMessage()` quando o resolver não trata a exceção. `CustomUserDetailsService` lança `DisabledException` com mensagem específica sobre conta desativada.

**Risco**
- Enumeração de estado de conta.
- Retorno de mensagens excessivamente descritivas para cliente não autenticado.

**Ação corretiva**
- Padronizar resposta externa para mensagem genérica.
- Registrar detalhe apenas em log interno/auditoria.
- Separar semântica de autenticação falha e conta inativa no backend, sem expor diretamente ao cliente.

**Prioridade**: Média.

---

### [MEDIUM] 7. CORS amplo em headers e `allowCredentials(true)`
**Evidência**
`SecurityConfig` define:
- origens explícitas
- métodos amplos
- `allowedHeaders = *`
- `allowCredentials(true)`
- `csrf.disable()` global

**Risco**
- Superfície maior do que o necessário para integrações browser-based.
- Se no futuro houver uso de cookie/sessão, a combinação pode se tornar arriscada.
- Abertura excessiva de headers aceitos.

**Ação corretiva**
- Restringir headers ao mínimo necessário.
- Validar se `allowCredentials(true)` é realmente indispensável.
- Documentar que o fluxo é exclusivamente bearer token.
- Reavaliar CSRF caso qualquer fluxo web com cookie seja introduzido.

**Prioridade**: Média.

---

### [MEDIUM] 8. Endpoints de autenticação públicos sem evidência clara de proteção anti-brute-force para senha
**Evidência**
`SecurityConfig` libera:
- `/auth/login`
- `/auth/login-face`
- `/auth/recover-password`
- `/auth/reset-password`

Há limites configurados para fluxos biométricos no `application.yml`, mas não foi encontrada evidência equivalente para login por senha e recuperação de senha no material inspecionado.

**Risco**
- Credential stuffing.
- Brute-force.
- Abuso de recuperação de senha.

**Ação corretiva**
- Rate limiting por IP/usuário.
- Backoff progressivo.
- Bloqueio temporário e trilha de auditoria.
- CAPTCHA adaptativo em fluxos de recuperação.

**Prioridade**: Média.

---

### [MEDIUM] 9. `InMemoryHttpExchangeRepository` amplia risco futuro se a exposição do Actuator crescer
**Evidência**
`KronosApplication` registra `InMemoryHttpExchangeRepository`. No momento, `management.endpoints.web.exposure.include: health`, o que reduz o risco atual.

**Risco**
- Caso alguém exponha `httpexchanges` futuramente, metadados de tráfego podem ficar acessíveis.
- Risco operacional por mudança futura de configuração.

**Ação corretiva**
- Remover o bean se não houver necessidade real.
- Ou manter proteção explícita impedindo exposição de endpoints de exchanges.
- Revisar profiles para garantir que isso não seja ampliado acidentalmente.

**Prioridade**: Média.

---

### [LOW] 10. Ausência visível de hardening explícito de security headers
**Evidência**
Na `SecurityConfig` inspecionada não há configuração explícita para CSP, `Referrer-Policy`, `Permissions-Policy` e política de frame mais restritiva.

**Risco**
- Proteção incompleta no canal browser.
- Dependência implícita de defaults do framework.

**Ação corretiva**
- Configurar headers explicitamente.
- Adicionar Content Security Policy adequada ao frontend.
- Definir `Referrer-Policy`, `Permissions-Policy` e política de frame/embedding conforme necessidade.

**Prioridade**: Baixa.

---

## Pontos positivos observados
- Uso de variáveis de ambiente para banco, JWT e AWS.
- Sessão stateless via Spring Security.
- Swagger e OpenAPI desabilitados por padrão.
- Actuator restrito a `health`.
- `management.endpoint.health.show-details: never`.

Esses pontos reduzem exposição, mas não eliminam os riscos listados acima.

---

## Ordem sugerida de correção
1. Remover qualquer segredo/configuração sensível versionada.
2. Endurecer `.gitignore` e pipeline com secret scanning.
3. Desligar `DEBUG` de segurança e SMTP em produção.
4. Implementar proteção anti-brute-force para login/reset.
5. Revisar modelo de confiança do JWT (`terms_accepted`, revogação, issuer/audience/jti).
6. Padronizar respostas 401/403 para não expor detalhe interno.
7. Restringir CORS/headers e revisar necessidade de `allowCredentials(true)`.
8. Revisar necessidade do `InMemoryHttpExchangeRepository`.
9. Adicionar security headers explícitos.

---

## Critérios de aceite sugeridos
- Nenhum segredo sensível em arquivo versionado.
- `DEBUG` de segurança e SMTP desligados em produção.
- Login e recuperação de senha protegidos por rate limiting.
- JWT com estratégia de revogação/versionamento.
- Autorização sensível não dependente apenas de claim transportada.
- CORS minimizado ao necessário.
- Security headers definidos explicitamente.
- Pipeline com varredura de segredos e SAST.
