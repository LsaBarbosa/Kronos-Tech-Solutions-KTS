# Escopo de Teste de Penetração - LGPD

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** ⚠️ PENDENTE EXECUÇÃO (Bloqueador para Produção)

Este documento define o escopo e critérios para teste de penetração da implementação LGPD antes de produção. Testes devem ser realizados por terceiro independente ou time de segurança interno.

---

## 1. Objetivo e Justificativa

### 1.1 Objetivo
Validar que a implementação LGPD não introduz vulnerabilidades que permitam:
- Acesso não-autorizado a dados biométricos
- Revogação não-consentida de consent
- Vazamento de dados pessoais em logs ou responses
- Manipulação de políticas de retenção
- Bypass de validações de conformidade

### 1.2 Justificativa
- Dados biométricos são altamente sensíveis (faciais, templates)
- Processamento ocorre em múltiplos terceiros (AWS S3, Rekognition)
- Retenção de ~7 anos justifica investimento em segurança
- Conformidade LGPD exige demonstração de segurança técnica

### 1.3 Bloqueadores
Este teste é **BLOQUEADOR** para produção. Qualquer vulnerabilidade crítica ou alta não-mitigada impede deploy.

---

## 2. Escopo In (Deve ser testado)

### 2.1 Backend (Java Spring Boot)

#### A. Autenticação & Autorização
- [ ] **LGPD Endpoints Require Auth**
  - Endpoint: GET `/lgpd/data-processing-catalog`
  - Verificar: Requisição sem JWT é rejeitada com 401
  - Verificar: Token expirado é rejeitado com 401
  - Verificar: Token inválido é rejeitado com 401

- [ ] **Role-Based Access Control**
  - Endpoint: POST `/lgpd/admin/retention/dry-run`
  - Verificar: Usuário comum não consegue executar (403)
  - Verificar: Apenas CTO role consegue executar (200)
  - Verificar: Bypass de role-check não é possível via:
    - Header spoofing (X-User-Role: CTO)
    - JWT manipulation (alterando claims)
    - Token refresh exploitation

- [ ] **Session Management**
  - Verificar: JWT_EXPIRATION está configurado (não infinito)
  - Verificar: Refresh token tem timeout (não infinito)
  - Verificar: Tokens não são armazenados em logs
  - Verificar: Token revocation funciona em logout

#### B. Data Minimization (Consent Evidence)

- [ ] **POST /lgpd/biometric/consent (Aceitar Consentimento)**
  - Input: CPF, biometric_template, consent_date
  - Verificar: Biometric template é armazenado encriptado
  - Verificar: CPF é armazenado com máscara (não em plaintext)
  - Verificar: Response não vaza biometric_template
  - Verificar: Response não vaza CPF inteiro
  - Verificar: Timestamp é registrado corretamente

- [ ] **DELETE /lgpd/biometric/consent (Revogar Consentimento)**
  - Input: user_id
  - Verificar: Biometric template é removido de storage operacional (S3)
  - Verificar: Consentimento é marcado como "revoked"
  - Verificar: Evidência histórica NÃO é deletada (apenas minimizada)
  - Verificar: Revogação não afeta dados de outros usuários
  - Verificar: Revogação é idempotente (chamar 2x não quebra)
  - Verificar: Rate limiting previne revogação em massa

- [ ] **GET /lgpd/biometric/consent/status**
  - Verificar: Apenas o próprio usuário consegue ver seu status
  - Verificar: Cross-user access é bloqueado (403)
  - Verificar: Response inclui: acceptance_date, revocation_date, retention_until
  - Verificar: Informações sensíveis não vazam

#### C. Data Export (Exercício de Direitos)

- [ ] **POST /lgpd/user/data-export**
  - Input: export_format (json/csv), request_reason
  - Verificar: Apenas o próprio usuário consegue exportar seus dados
  - Verificar: Export inclui TODOS os dados processados
  - Verificar: Export não inclui dados de terceiros
  - Verificar: PII é em plaintext no export (não criptografado)
  - Verificar: Export é assinado (para auditoria)
  - Verificar: Limite de 30 dias é respeitado (job timeout)
  - Verificar: Export é entregue em formato estruturado

- [ ] **GET /lgpd/user/data-export/{id}/status**
  - Verificar: Status é atualizado conforme download
  - Verificar: Apenas requester consegue ver status (401/403)
  - Verificar: Arquivo é removido após período de retenção

#### D. Retention Policy Enforcement

- [ ] **GET /lgpd/admin/retention/dry-run**
  - Input: policy_code, execution_date
  - Verificar: Apenas CTO consegue acessar (403 para outros)
  - Verificar: Simulação não altera dados
  - Verificar: Contagens retornadas são realistas
  - Verificar: Report inclui: scanned_count, affected_count, skipped_count

- [ ] **POST /lgpd/admin/retention/apply**
  - Input: policy_code, execution_date
  - Verificar: Apenas CTO consegue acessar (403 para outros)
  - Verificar: Dados são minimizados (não deletados)
  - Verificar: Audit log registra execução
  - Verificar: PII é sanitizado no audit log
  - Verificar: Rollback de retenção é possível (backup anterior)

#### E. Public Data Processing Catalog

- [ ] **GET /lgpd/data-processing-catalog**
  - Input: nenhum (ou ?language=pt)
  - Verificar: Endpoint é público (200 sem auth)
  - Verificar: Retorna todos os itens de catalog
  - Verificar: Não inclui dados sensíveis (templates, CPF)
  - Verificar: Texto menciona "pode ser preservado conforme lei"
  - Verificar: Legal basis está correto

- [ ] **GET /lgpd/data-processing-catalog/{code}**
  - Verificar: Endpoint é público
  - Verificar: Retorna apenas 1 item
  - Verificar: Cross-site request não consegue acessar dados privados

#### F. Security Headers & Network

- [ ] **HTTPS/TLS**
  - Verificar: Todos endpoints LGPD usam HTTPS (não HTTP)
  - Verificar: Certificado é válido (não auto-assinado)
  - Verificar: TLS 1.2+ (não 1.0 ou 1.1)
  - Verificar: Cipher suites são fortes (não export-grade)

- [ ] **HSTS (HTTP Strict Transport Security)**
  - Verificar: Header `Strict-Transport-Security` está presente
  - Verificar: max-age >= 31536000 (1 ano)
  - Verificar: includeSubDomains está ativo

- [ ] **Content Security Policy (CSP)**
  - Verificar: Header `Content-Security-Policy` restrige XSS
  - Verificar: Endpoints LGPD não carregam JS inline
  - Verificar: Endpoints LGPD não carregam recursos de domínios desconhecidos

- [ ] **CORS (Cross-Origin Resource Sharing)**
  - Verificar: `Access-Control-Allow-Origin` não é "*"
  - Verificar: LGPD endpoints têm CORS restritivo
  - Verificar: Preflight requests funcionam
  - Verificar: Credentials flag está correto (não exponho tokens via CORS)

- [ ] **Security Headers Diversos**
  - Verificar: `X-Content-Type-Options: nosniff`
  - Verificar: `X-Frame-Options: DENY` ou `SAMEORIGIN`
  - Verificar: `X-XSS-Protection: 1; mode=block` (legado, aceitar ausência em browsers modernos)
  - Verificar: `Referrer-Policy: strict-origin-when-cross-origin`

#### G. Input Validation & Injection

- [ ] **SQL Injection**
  - Input: CPF com caracteres especiais: `'; DROP TABLE users; --`
  - Verificar: Query falha gracefully (não executa comando)
  - Verificar: Parametrized queries são usadas
  - Verificar: ORM (JPA/Hibernate) protege contra SQL injection

- [ ] **Command Injection**
  - Input: user_id com pipe/semicolon: `123; rm -rf /`
  - Verificar: Comando não é executado
  - Verificar: Input é tratado como string literal

- [ ] **XXE (XML External Entity)**
  - Input: XML malicioso em request body
  - Verificar: Parsers XML não são configurados para ler entidades externas
  - Verificar: DTDs não são permitidas

- [ ] **LDAP Injection** (se aplicável)
  - Verificar: Filtros LDAP usam parametrização

#### H. Logging & Monitoring

- [ ] **PII em Logs**
  - Verificar: Logs não contêm CPF inteiro (deve estar mascarado)
  - Verificar: Logs não contêm email inteiro
  - Verificar: Logs não contêm biometric templates
  - Verificar: Logs não contêm JWT tokens
  - Verificar: Logs não contêm senhas
  - Verificar: SensitiveDataMasker está funcionando

  **Validação:**
  ```bash
  tail -f /var/log/kronos/app.log | grep -E "CPF|email|biometric|token|password"
  # Resultado esperado: linhas mascaradas (xxx.xxx.xxx-xx para CPF, etc.)
  ```

- [ ] **Audit Logging**
  - Verificar: Operações sensíveis (revogação, retenção) são auditadas
  - Verificar: Audit logs contêm: user, action, timestamp, result
  - Verificar: Audit logs são imutáveis (append-only)
  - Verificar: Acesso a audit logs é restrito (CTO only)

- [ ] **Error Messages**
  - Verificar: Mensagens de erro não vazam informações sensíveis
  - Verificar: 500 errors não expõem stack trace ao cliente
  - Verificar: 404 errors diferenciam "not found" de "not authorized"

#### I. Rate Limiting

- [ ] **Endpoints de Revogação**
  - Endpoint: DELETE `/lgpd/biometric/consent`
  - Verificar: Máx 10 revogações/minuto por usuário
  - Verificar: Rate limit é enforçado (retorna 429 Too Many Requests)
  - Verificar: Limite por IP para endpoints públicos

- [ ] **Endpoints Admin**
  - Endpoint: POST `/lgpd/admin/retention/apply`
  - Verificar: Máx 1 execução de retenção por dia
  - Verificar: Tentativa duplicada retorna 429

### 2.2 Frontend (React/TypeScript)

#### A. Biometric Consent UI

- [ ] **RevokeBiometricConsentDialog**
  - Verificar: Dialog requer confirmação (não 1-click)
  - Verificar: Aviso menciona "pode ser preservado conforme lei"
  - Verificar: Button "Confirmar revogacao" está desabilitado durante submissão
  - Verificar: Erro é capturado e exibido se revogação falhar
  - Verificar: Dialog não vazalhashes ou dados sensíveis

- [ ] **BiometricConsentCard**
  - Verificar: Status carrega corretamente (loading → active/revoked)
  - Verificar: Botões são habilitados apenas em estado apropriado
  - Verificar: Refresh de status não causa race conditions
  - Verificar: Component é testado para xss (não executa JS injetado)

#### B. Data Processing Catalog UI

- [ ] **DataProcessingCatalogCard**
  - Verificar: Catalog carrega de API pública (sem auth)
  - Verificar: Itens são renderizados corretamente
  - Verificar: Erro é capturado (API falha, sem permissão, etc.)
  - Verificar: "Pode ser preservado..." está mencionado
  - Verificar: Componente não vaza dados ao local storage

#### C. Input Handling

- [ ] **XSS (Cross-Site Scripting)**
  - Input: CPF field com `<img src=x onerror="alert(1)">`
  - Verificar: JS não é executado
  - Verificar: React automaticamente escapa conteúdo
  - Verificar: Não há uso de `dangerouslySetInnerHTML`

- [ ] **Local Storage**
  - Verificar: Tokens são armazenados em HttpOnly cookies (não localStorage)
  - Verificar: Se usando localStorage para JWT, avisar em security report
  - Verificar: Nenhum PII é armazenado em localStorage

#### D. Network & Communication

- [ ] **HTTPS Enforcement**
  - Verificar: API calls usam https://
  - Verificar: CSP header é respeitado
  - Verificar: Mix de HTTP/HTTPS não é permitido

- [ ] **CORS**
  - Verificar: Requests LGPD incluem credenciais corretas
  - Verificar: Preflight requests funcionam
  - Verificar: Cross-domain requests são bloqueadas se necessário

#### E. Sensitive Data in DOM

- [ ] **PII em HTML/DOM**
  - Verificar: CPF não aparece em plaintext na DOM
  - Verificar: Biometric templates não aparecem
  - Verificar: Tokens não aparecem em data attributes
  - Verificar: Elementos sensíveis têm `aria-hidden="true"` quando apropriado

#### F. Error Handling

- [ ] **User-Facing Error Messages**
  - Verificar: Mensagens não expõem detalhes técnicos
  - Verificar: Mensagens de erro em português (conforme design)
  - Verificar: Network errors são tratados gracefully

---

## 3. Escopo Out (Não é testado neste escopo)

### 3.1 Fora do Escopo

- [ ] **Infrastructure Layer**
  - AWS IAM policies (responsabilidade DevOps/Cloud)
  - S3 bucket configurations (responsabilidade DevOps)
  - Network security groups (responsabilidade DevOps)
  - DDoS mitigation (responsabilidade de cloud provider)

- [ ] **Third-Party Vulnerabilities**
  - CVEs em dependências npm/maven (responsabilidade de CI/SAST)
  - AWS Rekognition API security (responsabilidade AWS)
  - JWT library vulnerabilities (coberto por SAST, não pentest)

- [ ] **Legal/Compliance**
  - Parecer jurídico da implementação (responsabilidade Legal)
  - GDPR/CCPA/PDPA conformance (responsabilidade Legal/Compliance)
  - Data retention policy adequacy (responsabilidade Legal)

- [ ] **Performance Testing**
  - Load testing (responsabilidade de QA performance)
  - API response time SLAs (responsabilidade de QA)
  - Database query optimization (responsabilidade de Backend)

---

## 4. Teste Methodology

### 4.1 Abordagem

**Caixa Cinza (Gray Box):**
- Testador tem acesso a:
  - Código fonte (GitHub)
  - Documentação técnica
  - Credenciais de teste (não produção)
- Testador NÃO tem acesso a:
  - Credentials de produção
  - Database de produção
  - Logs de produção

**Ambiente:**
- Staging environment (idêntico a produção, mas com dados de teste)
- Database de teste com dados fake
- S3 bucket de teste

**Duração:**
- 1-2 semanas (dependência de descobertas)
- Daily standups com time de backend
- Responsabilidade de mitigação imediata se crítico/alto

### 4.2 Ferramentas Recomendadas

**Backend:**
- Burp Suite (proxy de HTTP, exploração)
- OWASP ZAP (automated scanning)
- Postman (API testing)
- sqlmap (SQL injection testing)
- curl/wget (manual testing)

**Frontend:**
- Burp Suite (DOM inspection)
- Chrome DevTools (XSS, local storage)
- OWASP ZAP (DOM-based XSS)

**Code:**
- SonarQube (SAST)
- Checkmarx (SAST, Java-focused)
- OWASP Dependency-Check (dependency vulnerabilities)

---

## 5. Critério de Aprovação

### 5.1 Vulnerabilidades Críticas

**BLOQUEADOR: Qualquer vulnerabilidade CRÍTICA impede produção.**

Exemplos de CRÍTICA:
- SQL Injection em endpoint LGPD
- Command execution em retention logic
- Acesso não-autorizado a dados biométricos
- Bypass de autenticação
- Revogação não-consentida de consent
- PII em plaintext em logs de produção
- Vazamento de biometric templates

**Resolução obrigatória:** Fix + re-teste em staging antes de deploy.

### 5.2 Vulnerabilidades Altas

**BLOQUEADOR CONDICIONAL: Vulnerabilidades ALTAS podem ser aceitas com mitigação documentada.**

Exemplos de ALTA:
- Weak password hashing (mas salting está presente)
- Rate limiting ausente (mas não afeta confidentiality)
- Logging sem PII masking (mas dados não vazam para atacante)
- CORS permissivo (mas endpoints públicos apenas)

**Resolução aceita:** Mitigação (e.g., WAF rule, compensating control) + Documentação + Aprovação CTO.

### 5.3 Vulnerabilidades Médias

**RECOMENDADO: Vulnerabilidades MÉDIAS devem ser fixadas pre-GA, aceito em GA com remediação.md.**

Exemplos de MÉDIA:
- Missing security headers
- Weak TLS cipher suites (mas TLS 1.2+ está ativo)
- Verbose error messages
- Unnecessary endpoints expostos

**Resolução aceita:** Patch em próxima sprint + Remediação.md.

### 5.4 Vulnerabilidades Baixas

**INFORMATIVO: Não bloqueia release.**

Exemplos de BAIXA:
- Typos em comentários
- Unused imports
- Sub-optimal logging

---

## 6. Relatório Final

### 6.1 Conteúdo Obrigatório

Pentest report deve incluir:

```markdown
# Penetration Testing Report - LGPD

**Data:** [date]
**Testador:** [name/company]
**Período:** [start] - [end]
**Ambiente:** Staging

## Executive Summary
- [ ] Total vulns found: X
- [ ] Critical: N
- [ ] High: N
- [ ] Medium: N
- [ ] Low: N
- [ ] **Recomendação:** APROVADO / CONDICIONAL / REPROVADO

## Detailed Findings

### CRÍTICA: [Title]
- **Severidade:** CRÍTICA
- **Componente:** [backend/frontend/infra]
- **Descrição:** [impact + reproduction steps]
- **Mitigação:** [fix required + timeline]
- **Status:** [OPEN / FIXED / WAIVED]

### ALTA: [Title]
...

## Teste Coverage

- [ ] Autenticação & Autorização: X/X items
- [ ] Data Minimization: X/X items
- [ ] Data Export: X/X items
- [ ] Retention: X/X items
- [ ] Security Headers: X/X items
- [ ] Input Validation: X/X items
- [ ] Logging: X/X items
- [ ] Rate Limiting: X/X items
- [ ] Frontend XSS: X/X items
- [ ] Network/HTTPS: X/X items

## Teste Logs

- [Link to detailed logs / screenshots]

## Assinatura
- Testador: ___________
- Data: ___________
- CTO (Aprovação): ___________
```

### 6.2 Artefatos

- [ ] Full pentest report (PDF + Markdown)
- [ ] Screenshots de exploração (críticas/altas)
- [ ] Reproduction steps (detailed)
- [ ] Remediation recommendations
- [ ] Re-test results (pós-fix)

---

## 7. Timeline e Responsabilidades

| Fase | Tarefa | Responsável | Duração | Status |
|------|--------|-------------|---------|--------|
| Prep | Preparar ambiente de staging | DevOps | 1-2 dias | 🔄 |
| Prep | Provisionar credenciais de teste | Security | 1 dia | 🔄 |
| Prep | Criar dados de teste LGPD | Backend | 2 dias | 🔄 |
| Test | Executar pentest | Pentest Team | 1-2 semanas | ⏳ |
| Test | Daily standups | All | Ongoing | ⏳ |
| Fix | Fix críticas | Backend | ASAP | ⏳ |
| Fix | Fix altas (mitigadas) | Backend | 1-2 sprints | ⏳ |
| Retest | Re-teste em staging | Pentest Team | 2-3 dias | ⏳ |
| Review | Aprovação CTO + Legal | CTO/Legal | 1 dia | ⏳ |

---

## 8. Escala de Severidade

| Severidade | Impacto | Exploração | Exemplo | Ação |
|------------|---------|------------|---------|------|
| **CRÍTICA** | Confidentiality, Integrity, Availability | Trivial | SQL injection em LGPD API | BLOQUEIA PRODUÇÃO |
| **ALTA** | Confidentiality ou Integrity | Média | Weak auth, PII em logs | Mitigação + Aprovação |
| **MÉDIA** | Confidentiality ou Availability | Difícil | Missing headers, verbose errors | Próxima sprint |
| **BAIXA** | Limitado/Nenhum | Muito difícil | Typos, unused code | Backlog |

---

## 9. Pentest Preparation Checklist

Antes de iniciar pentest, validar:

- [ ] **Staging está pronto:**
  - [ ] Ambiente é idêntico a produção (mesmas versões, configs)
  - [ ] HTTPS está ativo em staging
  - [ ] Dados de teste são realistas (fake CPFs, fake biometric templates)
  - [ ] Backup de staging foi feito (para reset pós-teste)

- [ ] **Credenciais são fornecidas:**
  - [ ] Usuário teste com role "user"
  - [ ] Usuário teste com role "CTO"
  - [ ] Usuário teste inválido (para negative testing)
  - [ ] JWT tokens para testing
  - [ ] AWS credentials para S3 testing (read-only)

- [ ] **Documentação é completa:**
  - [ ] Architecture diagram
  - [ ] API documentation (Swagger)
  - [ ] Database schema
  - [ ] Deployment topology
  - [ ] Security controls implemented

- [ ] **Time está disponível:**
  - [ ] Backend engineer para pair debugging
  - [ ] DevOps para infra questions
  - [ ] Security engineer para risk assessment
  - [ ] DPO para legal questions

- [ ] **Comunicação:**
  - [ ] NDA assinado
  - [ ] Escopo documentado e aprovado
  - [ ] Timeline acordado
  - [ ] Contact person designado

---

## 10. Responsabilidades Pós-Pentest

### 10.1 Se CRÍTICA ou ALTA encontrado

1. **Imediatamente (< 4 horas):**
   - [ ] Notificar CTO, Head of Security, Head of Legal
   - [ ] Criar JIRA ticket com severidade máxima
   - [ ] Limpar dados de teste (se PII foi comprometido)

2. **Dia 1:**
   - [ ] Iniciar fix
   - [ ] Entender root cause
   - [ ] Avaliar impacto em produção (retrospective)

3. **Dia 2-3:**
   - [ ] Implementar fix em staging
   - [ ] Validar fix
   - [ ] Pentest fazer re-teste

4. **Dia 4-7:**
   - [ ] Se aprovado, liberar para produção
   - [ ] Se reprovado, iterar

### 10.2 Se aprovado (MÉDIAS/BAIXAS apenas)

1. **Imediatamente:**
   - [ ] Revisar relatório final
   - [ ] CTO/Head of Security assinam aprovação
   - [ ] Comunicar à equipe jurídica

2. **Próxima sprint:**
   - [ ] Planejar fix de MÉDIAS
   - [ ] Backlog de BAIXAS

3. **Pré-GA:**
   - [ ] Verificar que todos os fixes foram implementados

---

## 11. Contatos

| Papel | Responsabilidade |
|-------|------------------|
| Pentest Lead | Coordena todos testes + Relatório final |
| Backend Lead | Fornece credenciais + Pair debugging |
| Security Lead | Oversight + Risk assessment |
| CTO | Aprovação final |
| Head of Legal | Revisão legal dos findings |
| DPO | LGPD implications assessment |

---

## 12. Documentação Relacionada

- [LGPD Production Release Checklist](lgpd-production-release-checklist.md)
- [LGPD Config Validation (Staging/Prod)](lgpd-staging-production-config-validation.md)
- [LGPD Rollback Runbook](lgpd-rollback-validation-runbook.md)
- [Security Code Review Guidelines](../security/security-code-review-guidelines.md)

---

**Documento de:** Security + DevOps  
**Última revisão:** 2026-05-25  
**Próxima revisão:** Após execução de pentest  
**Status:** ⚠️ PENDENTE EXECUÇÃO (BLOQUEADOR PARA PRODUÇÃO)

Para questões sobre escopo ou metodologia, contactar: security@kronos.com
