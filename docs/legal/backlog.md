Lucas, segue um **backlog novo e executável**, considerando as branches atuais:

```text
Back-end:  LsaBarbosa/Kronos-Tech-Solutions-KTS
Branch:    feature/lgpd-compliance

Front-end: LsaBarbosa/Kronos-Tech-Solution-User-Plataform
Branch:    feature/lgpd-compliance
```

O backlog existente no back-end ainda está desatualizado no cabeçalho, pois referencia `feature/s3-document` e `fix-colaborador`, não as branches atuais.

Também considerei os pontos já identificados:

* O `RetentionPolicyService` ainda está como primeira etapa e declara que **não executa ações destrutivas reais**, apenas marca políticas como executadas.
* O front-end possui `TermsAcceptanceGate`, que atualmente controla o aceite biométrico de forma ampla antes das rotas protegidas.
* A ANPD lista como direitos do titular: informação, confirmação/acesso, correção, bloqueio/exclusão/portabilidade, eliminação, revogação de consentimento e informações sobre compartilhamento. ([Serviços e Informações do Brasil][1])
* A ANPD orienta que o RIPD documente tratamentos de alto risco, tipos de dados, metodologia, segurança, riscos e medidas de mitigação. ([Serviços e Informações do Brasil][2])

---

# Backlog LGPD — Conclusão da branch `feature/lgpd-compliance`

## Objetivo

Concluir a frente LGPD do Kronos tornando a implementação tecnicamente defensável para produção, com foco em:

```text
1. Consentimento biométrico granular
2. Fluxos biométricos isolados
3. Retenção real
4. Anonimização por domínio
5. Painel administrativo LGPD
6. SLA e histórico completo de solicitações
7. RIPD e inventário de tratamento
8. Testes e validação final
```

---

# Definição de pronto geral

Uma sprint só deve ser considerada concluída quando cumprir:

```text
Back-end:
- ./gradlew clean test
- ./gradlew bootJar
- Flyway validando migrations
- Testes unitários dos services alterados
- Testes WebMvc dos controllers alterados
- Nenhum log com CPF completo, token, senha, base64 de face ou storage path sensível

Front-end:
- npm ci
- npm run lint
- npm run test
- npm run build
- Fluxos manuais validados no navegador

Banco:
- Toda alteração estrutural via Flyway
- Sem alterar migration já aplicada em produção
- Toda alteração destrutiva com dry-run antes de apply

Segurança:
- Isolamento por tenant preservado
- CTO, MANAGER e EMPLOYEE testados separadamente
- Endpoints sensíveis exigindo autenticação e autorização
```

---

# Sprint 0 — Preparação e saneamento da branch

## Objetivo

Preparar as branches atuais para receber as correções sem perder o trabalho já feito pelo Codex.

---

## Feature LGPD-000 — Atualizar documentação de branch e criar baseline técnico

### Tipo

```text
Back-end: sim
Front-end: sim
Banco: não
Prioridade: P0
```

### Problema

O backlog existente no back-end ainda menciona branches antigas. Isso pode induzir o Codex a aplicar mudanças em contexto errado.

### Arquivos prováveis

```text
Back-end:
kronos_lgpd_backlog.md
docs/legal/lgpd-implementation-report.md
docs/legal/lgpd-production-checklist.md

Front-end:
backlog.md
docs/sprint.md
VALIDACAO_BACKLOG_VISUAL.md
```

### Passo a passo

1. Atualizar o cabeçalho do backlog para:

```md
Produto: Kronos
Back-end: Kronos-Tech-Solutions-KTS
Branch back-end: feature/lgpd-compliance
Front-end: Kronos-Tech-Solution-User-Plataform
Branch front-end: feature/lgpd-compliance
```

2. Criar uma seção chamada:

```md
## Estado atual da branch feature/lgpd-compliance
```

3. Registrar que já existem:

```text
- LgpdController
- LgpdService
- LgpdRequest
- LgpdRequestHistory
- LegalConsent
- LegalText
- SecurityIncident
- RetentionPolicy
- EmployeeAnonymizationService
- PrivacyCenter
- LgpdRequestForm
- LgpdRequestsList
- BiometricConsentCard
- RevokeBiometricConsentDialog
```

4. Registrar pendências críticas:

```text
- Retenção ainda noop
- Consentimento biométrico ainda bloqueia acesso global
- Falta painel administrativo LGPD
- Falta SLA formal
- Falta inventário de tratamento
- Falta RIPD
- Anonimização ainda incompleta por domínio
```

### Critérios de aceite

```text
- Backlog atualizado com branches corretas
- Documentação não referencia mais feature/s3-document nem fix-colaborador como branches atuais
- README ou docs/legal apontam para o backlog atualizado
```

---

# Sprint 1 — Consentimento biométrico granular

## Objetivo

Remover o bloqueio global por consentimento biométrico e aplicar exigência de aceite apenas nos fluxos que realmente usam biometria.

---

## Feature LGPD-101 — Remover bloqueio global por consentimento biométrico no front-end

### Tipo

```text
Back-end: não
Front-end: sim
Banco: não
Prioridade: P0
```

### Problema

O `TermsAcceptanceGate` envolve as rotas protegidas no `App.tsx`, exigindo aceite biométrico para acesso geral à plataforma. Isso torna o consentimento biométrico uma condição ampla de uso, em vez de uma permissão específica para fluxos biométricos.

### Arquivos prováveis

```text
src/App.tsx
src/components/TermsAcceptanceGate.tsx
src/components/BiometricFeatureGate.tsx        # novo
src/config/app-routes.ts
src/components/checkin/CheckinModal.tsx
src/components/checkin/CheckinCameraStep.tsx
src/components/FaceLoginModal.tsx
src/components/privacy/BiometricConsentCard.tsx
```

### Passo a passo

1. No `App.tsx`, remover o `TermsAcceptanceGate` como wrapper geral das rotas autenticadas.

   Hoje o fluxo conceitual está assim:

```tsx
<ProtectedRoute>
  <TermsAcceptanceGate>
    <Dashboard />
    <Documentos />
    <PrivacyCenter />
    ...
  </TermsAcceptanceGate>
</ProtectedRoute>
```

2. Alterar para:

```tsx
<ProtectedRoute>
  <Dashboard />
  <Documentos />
  <PrivacyCenter />
  ...
</ProtectedRoute>
```

3. Criar um novo componente:

```text
src/components/BiometricFeatureGate.tsx
```

4. Responsabilidade do novo componente:

```text
- Consultar checkTermsStatus()
- Se aceito: renderizar children
- Se não aceito: mostrar modal/card de aceite biométrico
- Permitir cancelar sem deslogar o usuário
- Explicar que o bloqueio é apenas daquele recurso biométrico
```

5. Aplicar `BiometricFeatureGate` somente em:

```text
- Registro de ponto facial
- Login facial
- Cadastro/recadastro de face
- Qualquer validação facial futura
```

6. Não aplicar em:

```text
- Dashboard
- PrivacyCenter
- Documentos
- Espelho de ponto
- Solicitações LGPD
- Perfil do usuário
- Logout
- Exportação de dados
```

### Critérios de aceite

```text
- Usuário autenticado sem aceite biométrico acessa Dashboard
- Usuário sem aceite biométrico acessa PrivacyCenter
- Usuário sem aceite biométrico cria solicitação LGPD
- Usuário sem aceite biométrico exporta dados
- Usuário sem aceite biométrico não consegue usar ponto facial
- Usuário sem aceite biométrico não consegue usar login facial
- Usuário consegue cancelar o aceite biométrico sem ser deslogado
```

### Testes obrigatórios

```text
src/components/BiometricFeatureGate.test.tsx
src/App.test.tsx
src/components/TermsAcceptanceGate.test.tsx
src/components/checkin/CheckinModal.test.tsx
src/components/FaceLoginModal.test.tsx
```

---

## Feature LGPD-102 — Remover bloqueio global por consentimento biométrico no back-end

### Tipo

```text
Back-end: sim
Front-end: não
Banco: não
Prioridade: P0
```

### Problema

Mesmo que o front-end seja corrigido, o back-end não deve bloquear endpoints gerais com base em aceite biométrico.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/out/security/TermsValidationFilter.java
src/main/java/com/kts/kronos/config/SecurityConfig.java
src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java
src/main/java/com/kts/kronos/application/service/AuthService.java
src/main/java/com/kts/kronos/application/service/TimeRecordService.java
src/test/java/com/kts/kronos/adapter/out/security/TermsValidationFilterTest.java
src/test/java/com/kts/kronos/application/security/BiometricProtectionServiceTest.java
src/test/java/com/kts/kronos/application/service/AuthServiceTest.java
src/test/java/com/kts/kronos/application/service/TimeRecordServiceTest.java
```

### Passo a passo

1. Revisar `TermsValidationFilter`.

2. Remover qualquer lógica que bloqueie todas as requisições autenticadas quando `termsAccepted=false`.

3. Transformar o filtro em um destes modelos:

### Modelo recomendado

Remover o `TermsValidationFilter` do fluxo global e deixar a validação biométrica dentro de `BiometricProtectionService`.

### Modelo alternativo

Manter o filtro, mas ele deve atuar somente em paths biométricos:

```text
POST /records/checkin              quando payload contém faceImageBase64
POST /auth/login-face
POST /employee/register-face
POST /employee/update-face         se existir
```

4. No `BiometricProtectionService`, criar métodos explícitos:

```java
void requireActiveBiometricConsent(UUID employeeId);
void protectFaceLogin(String faceImageBase64, Boolean livenessPassed);
void protectCheckIn(UUID employeeId, String faceImageBase64, Boolean livenessPassed);
void protectFaceEnrollment(UUID employeeId, String faceImageBase64, Boolean livenessPassed);
```

5. `protectCheckIn` deve validar consentimento apenas se o check-in realmente exigir biometria.

6. `AuthService.login(...)` com senha não deve validar consentimento biométrico.

7. `AuthService.loginFace(...)` deve validar consentimento biométrico antes de autenticar por face.

8. `TimeRecordService.registerTime(...)` deve validar consentimento apenas se o fluxo for facial.

9. Criar exceção específica:

```java
BiometricConsentRequiredException
```

10. Mapear para HTTP:

```text
409 Conflict ou 403 Forbidden
code: BIOMETRIC_CONSENT_REQUIRED
message: Consentimento biométrico necessário para este recurso.
```

### Critérios de aceite

```text
- Login por senha funciona sem consentimento biométrico
- Endpoints /lgpd/** funcionam sem consentimento biométrico
- Endpoints /documents/** funcionam sem consentimento biométrico
- POST /auth/login-face exige consentimento biométrico ativo
- POST /records/checkin com biometria exige consentimento ativo
- Revogação biométrica não invalida acesso por senha
```

---

## Feature LGPD-103 — Ajustar experiência após revogação biométrica

### Tipo

```text
Back-end: sim
Front-end: sim
Banco: não
Prioridade: P0
```

### Problema

A revogação deve facilitar o exercício do direito do titular. A ANPD informa que o titular pode revogar consentimento a qualquer momento e que o controlador deve facilitar esse procedimento. ([Serviços e Informações do Brasil][1])

### Arquivos prováveis

```text
Front:
src/components/privacy/BiometricConsentCard.tsx
src/components/privacy/RevokeBiometricConsentDialog.tsx
src/context/AuthContext.tsx
src/service/terms.service.ts

Back:
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/main/java/com/kts/kronos/adapter/in/web/http/TermsController.java
src/main/java/com/kts/kronos/adapter/out/security/JwtUtils.java
```

### Passo a passo

1. Após `DELETE /terms/revoke-biometric`, o back-end deve:

```text
- revogar LegalConsent
- deletar imagem facial
- deletar template Rekognition
- limpar faceS3ObjectKey
- gerar novo cookie com termsAccepted=false
```

2. O front-end deve:

```text
- atualizar sessão
- permanecer na tela atual
- não redirecionar para login
- exibir status “Consentimento revogado”
- desabilitar apenas recursos biométricos
```

3. Criar teste onde:

```text
Usuário autenticado
Consentimento ativo
Revoga biometria
Continua autenticado
Acessa PrivacyCenter
Não consegue abrir check-in facial
```

### Critérios de aceite

```text
- Revogar biometria não derruba a sessão
- Revogar biometria não bloqueia PrivacyCenter
- Revogar biometria não bloqueia Dashboard
- Face login fica indisponível
- Check-in facial fica indisponível
```

---

# Sprint 2 — Retenção real de dados

## Objetivo

Substituir a implementação `noop` por um motor real de retenção com dry-run, execução segura, auditoria e política por domínio.

---

## Feature LGPD-201 — Criar arquitetura de retenção por domínio

### Tipo

```text
Back-end: sim
Front-end: não
Banco: sim
Prioridade: P0
```

### Problema

A retenção atual não executa exclusão ou anonimização real. O próprio serviço diz que ações destrutivas ainda precisam ser implementadas.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/RetentionPolicyService.java
src/main/java/com/kts/kronos/application/service/retention/RetentionPolicyExecutor.java
src/main/java/com/kts/kronos/application/service/retention/RetentionDomainProcessor.java
src/main/java/com/kts/kronos/application/service/retention/TokenRetentionProcessor.java
src/main/java/com/kts/kronos/application/service/retention/MessageRetentionProcessor.java
src/main/java/com/kts/kronos/application/service/retention/DocumentRetentionProcessor.java
src/main/java/com/kts/kronos/application/service/retention/AuditLogRetentionProcessor.java
src/main/java/com/kts/kronos/application/service/retention/BiometricRetentionProcessor.java
src/main/resources/db/migration/V9__create_retention_execution_log.sql
```

### Nova tabela sugerida

```sql
CREATE TABLE tb_retention_execution_log (
    execution_id UUID PRIMARY KEY,
    policy_code VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL,
    scanned_count BIGINT NOT NULL DEFAULT 0,
    affected_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    notes TEXT NULL
);
```

### Passo a passo

1. Criar enum:

```java
public enum RetentionResourceType {
    BLACKLISTED_TOKEN,
    PASSWORD_RESET_TOKEN,
    MESSAGE,
    DOCUMENT,
    AUDIT_LOG,
    LEGAL_CONSENT,
    BIOMETRIC_ARTIFACT,
    LGPD_REQUEST
}
```

2. Criar enum:

```java
public enum RetentionAction {
    DELETE,
    ANONYMIZE,
    PSEUDONYMIZE,
    PRESERVE
}
```

3. Criar interface:

```java
public interface RetentionDomainProcessor {
    RetentionResourceType supports();
    RetentionExecutionResult execute(RetentionPolicy policy, RetentionExecutionMode mode);
}
```

4. Criar `RetentionExecutionMode` com:

```text
DRY_RUN
APPLY
```

5. `RetentionPolicyService.executeEnabledPolicies()` deve:

```text
- buscar políticas habilitadas
- abrir execution log
- localizar processor por resourceType
- executar em DRY_RUN ou APPLY
- registrar scanned/affected/skipped/errors
- registrar auditoria
- atualizar lastExecutedAt
```

6. Proibir execução `APPLY` se a política estiver sem:

```text
resourceType
retentionDays
action
preserveLaborData
preserveFiscalData
```

7. Criar testes unitários para cada processor.

### Critérios de aceite

```text
- RetentionPolicyService não contém mais apply_noop
- Dry-run não altera dados
- Apply altera dados conforme processor
- Toda execução gera log
- Toda execução registra métrica
- Erro em um domínio não deve corromper outros domínios
```

---

## Feature LGPD-202 — Implementar retenção de tokens e segurança

### Tipo

```text
Back-end: sim
Banco: não
Prioridade: P0
```

### Domínios

```text
- BLACKLISTED_TOKEN
- PASSWORD_RESET_TOKEN
```

### Passo a passo

1. Criar `TokenRetentionProcessor`.

2. Para `BLACKLISTED_TOKEN`:

```text
- deletar tokens com expiration anterior a now
```

3. Para `PASSWORD_RESET_TOKEN`:

```text
- deletar tokens expirados
- deletar tokens usados se houver flag
```

4. Não usar anonimização; aqui a ação correta é exclusão.

5. Criar métodos nos repositories:

```java
long countExpiredBefore(Instant cutoff);
int deleteExpiredBefore(Instant cutoff);
```

### Critérios de aceite

```text
- Dry-run retorna quantidade sem deletar
- Apply deleta tokens expirados
- Não deleta token válido
- Testes cobrem token expirado e token vigente
```

---

## Feature LGPD-203 — Implementar retenção de mensagens

### Tipo

```text
Back-end: sim
Banco: sim, se precisar adicionar deletedAt
Prioridade: P0
```

### Regra sugerida

```text
Mensagens operacionais antigas:
- aplicar soft delete ou hard delete após prazo configurado
- preservar se vinculada a incidente, solicitação LGPD ou auditoria legal
```

### Passo a passo

1. Avaliar entidade `Message`.

2. Adicionar campos, se inexistentes:

```text
deletedAt
deletedBySystem
retentionPolicyCode
```

3. Criar `MessageRetentionProcessor`.

4. Selecionar mensagens:

```text
createdAt < now - retentionDays
AND não vinculadas a processo legal
AND não marcadas como preservadas
```

5. Para `DRY_RUN`, apenas contar.

6. Para `APPLY`, executar soft delete.

7. Criar job idempotente.

### Critérios de aceite

```text
- Mensagem antiga é removida/ocultada conforme política
- Mensagem recente é preservada
- Mensagem vinculada a processo legal é preservada
- Listagens do front não exibem mensagens removidas
```

---

## Feature LGPD-204 — Implementar retenção de documentos

### Tipo

```text
Back-end: sim
Banco: sim
Prioridade: P0
```

### Domínios de documento

```text
BIOMETRIC_CONSENT_TERM
MEDICAL_CERTIFICATE
TIME_RECORD_ATTACHMENT
EMPLOYEE_DOCUMENT
LEGAL_REPORT
```

### Regra

```text
- Documentos fiscais/trabalhistas: preservar pelo prazo legal definido
- Termo biométrico: preservar como evidência após revogação, conforme política
- Imagem facial/template: excluir após revogação
- Documentos comuns: excluir/anomizar metadados após prazo
```

### Passo a passo

1. Criar `DocumentRetentionProcessor`.

2. Separar documento de arquivo físico:

```text
tb_document = metadado
S3/local storage = arquivo físico
```

3. Para documentos removíveis:

```text
- deletar arquivo físico
- marcar tb_document como deletedByRetention = true
- limpar checksum se não for necessário
- preservar apenas documentId, type, uploadedAt e reason
```

4. Para documentos legais preservados:

```text
- não deletar
- registrar skipped_count
- registrar motivo preserveLaborData/preserveFiscalData
```

5. Criar campo:

```sql
ALTER TABLE tb_document ADD COLUMN deleted_by_retention BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_document ADD COLUMN retention_deleted_at TIMESTAMP NULL;
ALTER TABLE tb_document ADD COLUMN retention_policy_code VARCHAR(100) NULL;
```

### Critérios de aceite

```text
- Documento removível antigo é removido do storage
- Documento trabalhista/fiscal é preservado
- Documento removido não aparece para usuário
- Auditoria registra execução
```

---

## Feature LGPD-205 — Implementar retenção de logs

### Tipo

```text
Back-end: sim
Banco: sim
Prioridade: P1
```

### Regra

```text
Logs recentes:
- preservar

Logs antigos:
- anonimizar IP
- limpar userAgent detalhado
- sanitizar details
- preservar action, timestamp, severity, resourceType
```

### Passo a passo

1. Criar `AuditLogRetentionProcessor`.

2. Adicionar método no `SensitiveDataMasker`:

```java
sanitizeIp(String ip)
sanitizeUserAgent(String userAgent)
sanitizeAuditDetails(String details)
```

3. Para logs vencidos:

```text
ipAddress = null ou hash irreversível
userAgent = "ANONYMIZED"
details = sanitizeDetails(details)
```

4. Não deletar logs de segurança críticos antes do prazo configurado.

### Critérios de aceite

```text
- Logs antigos perdem dados pessoais diretos
- Logs continuam úteis para auditoria estatística
- Logs recentes continuam completos
- Não há CPF/e-mail/token em details após sanitização
```

---

# Sprint 3 — Anonimização por domínio

## Objetivo

Tornar a anonimização consistente em todos os domínios do sistema, preservando dados obrigatórios por lei e removendo excesso de dados pessoais.

---

## Feature LGPD-301 — Criar `AnonymizationPlan` por colaborador

### Tipo

```text
Back-end: sim
Banco: sim
Prioridade: P0
```

### Problema

O serviço atual anonimiza apenas parte do cadastro do colaborador. Ele remove face, revoga consentimento e desativa usuário, mas ainda deixa diversos vínculos e dados correlacionáveis.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/EmployeeAnonymizationService.java
src/main/java/com/kts/kronos/application/service/anonymization/AnonymizationPlan.java
src/main/java/com/kts/kronos/application/service/anonymization/AnonymizationDomainProcessor.java
src/main/java/com/kts/kronos/application/service/anonymization/EmployeeDataAnonymizer.java
src/main/java/com/kts/kronos/application/service/anonymization/UserDataAnonymizer.java
src/main/java/com/kts/kronos/application/service/anonymization/DocumentDataAnonymizer.java
src/main/java/com/kts/kronos/application/service/anonymization/TimeRecordDataAnonymizer.java
src/main/java/com/kts/kronos/application/service/anonymization/MessageDataAnonymizer.java
src/main/java/com/kts/kronos/application/service/anonymization/AuditLogDataAnonymizer.java
src/main/resources/db/migration/V10__create_anonymization_execution_log.sql
```

### Passo a passo

1. Criar `AnonymizationPlan`.

```java
public record AnonymizationPlan(
    UUID employeeId,
    UUID companyId,
    UUID requestedByUserId,
    String reason,
    boolean preserveLaborData,
    boolean preserveFiscalData,
    boolean deleteBiometricArtifacts,
    boolean anonymizeDocuments,
    boolean anonymizeMessages,
    boolean anonymizeAuditLogs
) {}
```

2. Criar tabela de execução:

```sql
CREATE TABLE tb_anonymization_execution_log (
    execution_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    company_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    employee_processed BOOLEAN DEFAULT FALSE,
    user_processed BOOLEAN DEFAULT FALSE,
    biometric_processed BOOLEAN DEFAULT FALSE,
    document_processed BOOLEAN DEFAULT FALSE,
    time_record_processed BOOLEAN DEFAULT FALSE,
    message_processed BOOLEAN DEFAULT FALSE,
    audit_processed BOOLEAN DEFAULT FALSE,
    notes TEXT NULL
);
```

3. `EmployeeAnonymizationService.anonymize(...)` deve montar o plano e executar processors em ordem:

```text
1. Biometria
2. Consentimentos
3. Usuário
4. Cadastro employee
5. Documentos
6. Mensagens
7. Logs
8. Registros de ponto
```

4. Implementar execução transacional com cuidado:

```text
- banco em transação
- deleção externa de S3/Rekognition com compensação/log
- se storage falhar, não mascarar como sucesso
```

### Critérios de aceite

```text
- Existe plano explícito de anonimização
- Cada domínio registra status
- Falha parcial é visível
- Reexecução é idempotente
```

---

## Feature LGPD-302 — Corrigir anonimização do CPF e identificadores

### Tipo

```text
Back-end: sim
Banco: talvez
Prioridade: P0
```

### Problema

CPF anonimizado não deve quebrar constraint/tamanho de coluna e não deve parecer CPF real.

### Passo a passo

1. Verificar tamanho atual da coluna `cpf`.

2. Definir padrão seguro:

```text
Opção A:
cpf = null, se coluna permitir

Opção B:
cpf = hash curto compatível com coluna
Exemplo: "ANON" + 7 caracteres = 11 chars

Opção C:
cpf = "00000000000" somente se houver índice único tratado corretamente
```

3. Recomendação técnica:

```java
private String buildAnonymizedCpf(UUID employeeId, String cpf) {
    String hash = sha256(employeeId + ":" + cpf);
    return "ANON" + hash.substring(0, 7); // total 11
}
```

4. Se CPF for usado em índice único, garantir unicidade:

```text
ANON + hash determinístico por employeeId
```

5. Adicionar teste para tamanho do CPF.

### Critérios de aceite

```text
- CPF anonimizado não excede tamanho da coluna
- CPF anonimizado não é CPF real válido
- CPF anonimizado é único por colaborador
- Teste cobre coluna varchar(11)
```

---

## Feature LGPD-303 — Anonimizar cadastro do colaborador

### Tipo

```text
Back-end: sim
Prioridade: P0
```

### Regra

```text
Antes:
fullName, cpf, pis, email, phone, address, salary, jobPosition

Depois:
fullName = ANONYMIZED-{employeeId}
cpf = ANONxxxxxxx
pis = null
email = anon-{employeeId}@deleted.local
phone = null
address = null
active = false
deletedAt = now
deletedBy = actor
deactivationReason = LGPD_ANONYMIZATION
```

### Atenção

Avaliar se `salary` e `jobPosition` devem ser preservados para relatórios trabalhistas/fiscais. Se preservados, documentar no plano.

### Critérios de aceite

```text
- Nome não identifica pessoa
- CPF não identifica pessoa
- PIS removido
- E-mail removido
- Telefone removido
- Endereço removido
- Colaborador desativado
```

---

## Feature LGPD-304 — Anonimizar usuário vinculado

### Tipo

```text
Back-end: sim
Prioridade: P0
```

### Passo a passo

1. Desativar usuário.

2. Alterar username para não identificável, se o username for e-mail ou CPF.

3. Se `username` tiver constraint única:

```text
username = deleted-{userId}
```

4. Invalidar sessões/tokens ativos.

5. Blacklist de token atual se aplicável.

### Critérios de aceite

```text
- Usuário anonimizado não consegue login
- Username não contém e-mail/CPF
- Sessões anteriores deixam de funcionar
```

---

## Feature LGPD-305 — Tratar biometria na anonimização

### Tipo

```text
Back-end: sim
Prioridade: P0
```

### Passo a passo

1. Se `faceS3ObjectKey` existir:

```text
- deletar imagem do storage
- limpar faceS3ObjectKey
```

2. Deletar templates no provider facial:

```java
faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);
```

3. Revogar consentimento biométrico ativo.

4. Preservar termo de aceite apenas como evidência legal, se a política assim definir.

### Critérios de aceite

```text
- Imagem facial removida
- Template facial removido
- Consentimento ativo revogado
- Login facial não funciona mais
```

---

## Feature LGPD-306 — Tratar registros de ponto preservados

### Tipo

```text
Back-end: sim
Banco: talvez
Prioridade: P0
```

### Problema

Registros de ponto podem ter obrigação trabalhista/fiscal. Não devem ser apagados sem política legal, mas também não devem expor mais dados pessoais que o necessário.

### Passo a passo

1. Não deletar `TimeRecord` por padrão.

2. Preservar:

```text
timeRecordId
startWork
endWork
statusRecord
nsrCheckin
nsrCheckout
companyId
```

3. Avaliar geolocalização:

```text
latitude/longitude/endLatitude/endLongitude
```

4. Para colaborador anonimizado:

```text
- ocultar geolocalização precisa em exportações futuras
- manter somente flag geolocationPresent
- se necessário, arredondar coordenadas ou remover
```

5. Adicionar campo opcional:

```sql
ALTER TABLE tb_time_record ADD COLUMN anonymized_employee BOOLEAN DEFAULT FALSE;
```

### Critérios de aceite

```text
- Registro fiscal/trabalhista preservado
- Dados pessoais excessivos removidos ou ocultados
- Exportação LGPD de colaborador anonimizado não expõe geolocalização precisa
```

---

# Sprint 4 — Painel administrativo LGPD

## Objetivo

Criar interface para CTO/MANAGER tratar solicitações LGPD, acompanhar SLA, consultar histórico e executar ações controladas.

---

## Feature LGPD-401 — Criar rotas e menu administrativo LGPD

### Tipo

```text
Back-end: não
Front-end: sim
Prioridade: P0
```

### Arquivos prováveis

```text
src/config/app-routes.ts
src/components/Sidebar.tsx
src/pages/admin/AdminLgpdRequests.tsx
src/pages/admin/AdminLgpdRequestDetails.tsx
src/service/lgpd-admin.service.ts
src/types/lgpd.ts
```

### Rotas sugeridas

```text
/lgpd/admin/requests
/lgpd/admin/requests/:requestId
/lgpd/admin/incidents
/lgpd/admin/retention
/lgpd/admin/inventory
```

### Permissão

```text
CTO:
- vê todas as empresas

MANAGER:
- vê solicitações da própria empresa

EMPLOYEE:
- não vê painel administrativo
```

### Passo a passo

1. Criar entrada no menu:

```text
Privacidade e LGPD
  - Minhas solicitações
  - Administração LGPD
```

2. Exibir “Administração LGPD” apenas para:

```text
CTO
MANAGER
```

3. Criar tela inicial com cards:

```text
Solicitações abertas
Solicitações vencendo
Solicitações vencidas
Incidentes abertos
Retenção pendente
```

### Critérios de aceite

```text
- CTO enxerga menu administrativo
- MANAGER enxerga menu administrativo
- EMPLOYEE não enxerga menu administrativo
- Acesso direto por URL respeita RoleRoute
```

---

## Feature LGPD-402 — Tela de listagem administrativa de solicitações

### Tipo

```text
Back-end: ajustar se necessário
Front-end: sim
Prioridade: P0
```

### Endpoint atual

```text
GET /lgpd/requests
```

### Melhorias necessárias no back

Adicionar filtros:

```text
companyId
employeeId
type
status
createdFrom
createdTo
dueBefore
overdue
page
size
sort
```

### Passo a passo back-end

1. Alterar `LgpdController.listRequests(...)`.

2. Adicionar paginação com `Pageable`.

3. No `LgpdService`, manter tenant isolation:

```text
CTO: todas ou por companyId
MANAGER: apenas sua companyId
EMPLOYEE: apenas próprias solicitações
```

4. Retornar `Page<LgpdRequestResponse>`.

### Passo a passo front-end

1. Criar tabela com colunas:

```text
ID
Colaborador
Empresa
Tipo
Status
Data de abertura
Prazo
Responsável
Última atualização
Ações
```

2. Criar filtros:

```text
Status
Tipo
Vencidas
Empresa, apenas CTO
Colaborador
Período
```

3. Ações:

```text
Ver detalhes
Alterar status
Atribuir responsável
Adicionar nota
```

### Critérios de aceite

```text
- CTO lista todas as solicitações
- MANAGER lista apenas solicitações da própria empresa
- EMPLOYEE não acessa endpoint administrativo
- Tabela possui paginação
- Filtros funcionam
```

---

## Feature LGPD-403 — Tela de detalhe da solicitação

### Tipo

```text
Back-end: sim
Front-end: sim
Prioridade: P0
```

### Informações na tela

```text
Dados da solicitação:
- requestId
- tipo
- status
- descrição
- colaborador
- empresa
- criado em
- prazo
- responsável
- resolução
- anexos/evidências

Histórico:
- data/hora
- usuário
- status anterior
- status novo
- nota
- ação
```

### Passo a passo back-end

1. Criar endpoint:

```text
GET /lgpd/requests/{requestId}/details
```

2. Response sugerido:

```java
public record LgpdRequestDetailsResponse(
    LgpdRequestResponse request,
    EmployeeSummaryResponse employee,
    CompanySummaryResponse company,
    UserSummaryResponse assignedTo,
    List<LgpdRequestHistoryResponse> history,
    List<LgpdRequestEvidenceResponse> evidences
) {}
```

3. Criar testes WebMvc para:

```text
CTO
MANAGER mesma empresa
MANAGER empresa diferente
EMPLOYEE titular
EMPLOYEE terceiro
```

### Passo a passo front-end

1. Criar `AdminLgpdRequestDetails.tsx`.

2. Consumir:

```text
GET /lgpd/requests/{requestId}/details
GET /lgpd/requests/{requestId}/history
```

3. Criar timeline visual.

4. Criar form de atualização:

```text
Novo status
Nota interna
Nota visível ao titular
Responsável
```

### Critérios de aceite

```text
- Detalhe exibe dados completos
- Histórico é exibido em ordem cronológica
- Acesso respeita tenant
- Notas internas não aparecem para titular
- Notas públicas aparecem para titular
```

---

# Sprint 5 — SLA e histórico completo de atendimento

## Objetivo

Formalizar o atendimento das solicitações LGPD com prazo, responsável, trilha de eventos, evidências e comunicação ao titular.

---

## Feature LGPD-501 — Adicionar SLA em `LgpdRequest`

### Tipo

```text
Back-end: sim
Front-end: sim
Banco: sim
Prioridade: P0
```

### Campos novos

```sql
ALTER TABLE tb_lgpd_request
ADD COLUMN due_at TIMESTAMP NULL,
ADD COLUMN assigned_to_user_id UUID NULL,
ADD COLUMN priority VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
ADD COLUMN closed_reason VARCHAR(100) NULL,
ADD COLUMN public_resolution_notes TEXT NULL,
ADD COLUMN internal_notes TEXT NULL;
```

### Regra inicial de SLA

A página da ANPD informa que confirmação e acesso devem ser providenciados imediatamente e que pedidos envolvendo origem dos dados, inexistência de registro, critérios e finalidade devem ser atendidos em até 15 dias. ([Serviços e Informações do Brasil][1])

Para o Kronos, usar regra conservadora:

```text
CONFIRM_PROCESSING: 15 dias corridos
ACCESS: 15 dias corridos
CORRECTION: 15 dias corridos
ANONYMIZATION: 15 dias corridos
BLOCKING: 15 dias corridos
DELETION: 15 dias corridos
PORTABILITY: 15 dias corridos
CONSENT_REVOCATION: imediato ou até 2 dias úteis
SHARING_INFORMATION: 15 dias corridos
```

### Passo a passo

1. Criar `LgpdSlaPolicyService`.

2. Método:

```java
Instant calculateDueAt(LgpdRequestType type, Instant createdAt);
```

3. No `LgpdService.createRequest(...)`, preencher `dueAt`.

4. Adicionar status derivado:

```text
OPEN
IN_ANALYSIS
WAITING_CONTROLLER
WAITING_LEGAL_REVIEW
COMPLETED
REJECTED
PARTIALLY_COMPLETED
OVERDUE, derivado, não necessariamente persistido
```

5. No response, incluir:

```text
dueAt
overdue
daysRemaining
assignedToUserId
priority
```

### Critérios de aceite

```text
- Toda solicitação nasce com dueAt
- Solicitação vencida é identificada
- Front mostra prazo
- Admin filtra por vencidas
- Titular vê prazo de atendimento
```

---

## Feature LGPD-502 — Histórico completo com eventos formais

### Tipo

```text
Back-end: sim
Front-end: sim
Banco: sim
Prioridade: P0
```

### Novo modelo de histórico

Adicionar campos em `tb_lgpd_request_history`:

```sql
ALTER TABLE tb_lgpd_request_history
ADD COLUMN event_type VARCHAR(50) NULL,
ADD COLUMN previous_status VARCHAR(50) NULL,
ADD COLUMN new_status VARCHAR(50) NULL,
ADD COLUMN public_note TEXT NULL,
ADD COLUMN internal_note TEXT NULL,
ADD COLUMN actor_user_id UUID NULL,
ADD COLUMN visible_to_data_subject BOOLEAN NOT NULL DEFAULT TRUE;
```

### Eventos

```text
REQUEST_CREATED
STATUS_CHANGED
ASSIGNED
NOTE_ADDED
EVIDENCE_ATTACHED
EXPORT_GENERATED
ANONYMIZATION_EXECUTED
REQUEST_REJECTED
REQUEST_COMPLETED
SLA_RECALCULATED
```

### Passo a passo

1. Criar enum `LgpdRequestEventType`.

2. Substituir histórico textual simples por evento formal.

3. Criar método:

```java
lgpdRequestHistoryProvider.appendEvent(...)
```

4. Toda alteração de status deve gravar:

```text
status anterior
status novo
ator
data
nota pública
nota interna
```

5. No endpoint do titular, retornar apenas eventos `visibleToDataSubject=true`.

6. No endpoint admin, retornar todos os eventos.

### Critérios de aceite

```text
- Criar solicitação gera REQUEST_CREATED
- Alterar status gera STATUS_CHANGED
- Atribuir responsável gera ASSIGNED
- Finalizar gera REQUEST_COMPLETED
- Titular não vê nota interna
- Admin vê histórico completo
```

---

## Feature LGPD-503 — Ações administrativas da solicitação

### Tipo

```text
Back-end: sim
Front-end: sim
Banco: talvez
Prioridade: P0
```

### Endpoints sugeridos

```text
PATCH /lgpd/requests/{requestId}/assign
POST  /lgpd/requests/{requestId}/notes
POST  /lgpd/requests/{requestId}/complete
POST  /lgpd/requests/{requestId}/reject
POST  /lgpd/requests/{requestId}/execute-anonymization
POST  /lgpd/requests/{requestId}/generate-export
```

### Passo a passo

1. Criar DTOs:

```text
AssignLgpdRequestRequest
AddLgpdRequestNoteRequest
CompleteLgpdRequestRequest
RejectLgpdRequestRequest
```

2. Regra para rejeição:

```text
- rejection reason obrigatório
- public note obrigatória
- internal note opcional
```

3. Regra para conclusão:

```text
- public resolution note obrigatória
- evidência opcional
```

4. Para `ANONYMIZATION`, não executar direto sem confirmação:

```text
- tela deve mostrar impacto
- usuário admin confirma
- backend registra evento
- executa EmployeeAnonymizationService
```

### Critérios de aceite

```text
- Admin atribui solicitação
- Admin adiciona nota
- Admin conclui
- Admin rejeita com justificativa
- Titular vê resolução pública
- Histórico completo é preservado
```

---

# Sprint 6 — RIPD e inventário de tratamento

## Objetivo

Criar documentação técnica e dados estruturados para demonstrar finalidade, base legal, dados tratados, retenção, compartilhamento, riscos e medidas de mitigação.

---

## Feature LGPD-601 — Criar inventário de tratamento de dados

### Tipo

```text
Back-end: sim
Front-end: sim
Banco: sim
Docs: sim
Prioridade: P0
```

### Fundamentação

A ANPD recomenda que o RIPD descreva tipos de dados tratados, operações de tratamento, finalidades, hipóteses legais, necessidade, proporcionalidade, riscos e medidas de mitigação. ([Serviços e Informações do Brasil][2])

### Nova tabela

```sql
CREATE TABLE tb_data_processing_inventory (
    inventory_id UUID PRIMARY KEY,
    process_code VARCHAR(100) NOT NULL UNIQUE,
    process_name VARCHAR(200) NOT NULL,
    data_category VARCHAR(100) NOT NULL,
    data_fields TEXT NOT NULL,
    data_subject_category VARCHAR(100) NOT NULL,
    purpose TEXT NOT NULL,
    legal_basis VARCHAR(100) NOT NULL,
    sensitive_data BOOLEAN NOT NULL DEFAULT FALSE,
    source_system VARCHAR(100) NOT NULL,
    storage_location VARCHAR(200) NULL,
    retention_policy_code VARCHAR(100) NULL,
    external_sharing TEXT NULL,
    international_transfer BOOLEAN NOT NULL DEFAULT FALSE,
    security_measures TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL
);
```

### Processos mínimos do Kronos

```text
AUTH_PASSWORD_LOGIN
AUTH_FACE_LOGIN
FACE_ENROLLMENT
TIME_RECORD_CHECKIN
TIME_RECORD_GEOLOCATION
EMPLOYEE_MANAGEMENT
DOCUMENT_MANAGEMENT
LEGAL_REPORT_AFD
LEGAL_REPORT_AEJ
LEGAL_REPORT_POINT_MIRROR
PASSWORD_RECOVERY
MESSAGE_MANAGEMENT
LGPD_REQUEST_MANAGEMENT
SECURITY_INCIDENT_MANAGEMENT
AUDIT_LOGGING
```

### Passo a passo

1. Criar entidade `DataProcessingInventoryEntity`.

2. Criar domain model `DataProcessingInventory`.

3. Criar repository/provider.

4. Criar migration com registros iniciais.

5. Criar endpoint admin:

```text
GET /lgpd/inventory
GET /lgpd/inventory/{processCode}
POST /lgpd/inventory
PATCH /lgpd/inventory/{inventoryId}
```

6. Criar tela front:

```text
/lgpd/admin/inventory
```

7. Exibir tabela:

```text
Processo
Dados tratados
Dado sensível?
Finalidade
Base legal
Retenção
Compartilhamento
Status
```

### Critérios de aceite

```text
- Inventário mínimo existe no banco
- Admin consegue listar
- CTO consegue editar
- MANAGER pode consultar, se autorizado
- Privacy Center pode mostrar versão pública simplificada
```

---

## Feature LGPD-602 — Criar documento RIPD inicial

### Tipo

```text
Docs: sim
Back-end: opcional
Front-end: opcional
Prioridade: P0
```

### Arquivos novos

```text
docs/legal/RIPD-biometria-geolocalizacao.md
docs/legal/inventario-tratamento-dados.md
docs/legal/matriz-base-legal.md
docs/legal/matriz-retencao-dados.md
```

### Estrutura do RIPD

```md
# RIPD — Biometria Facial e Geolocalização no Kronos

## 1. Identificação
- Controlador
- Operador
- Encarregado/DPO
- Sistema
- Versão
- Data

## 2. Escopo
- Login facial
- Registro de ponto facial
- Geolocalização no registro de ponto
- Documentos trabalhistas/fiscais

## 3. Dados pessoais tratados
- Identificação
- Contato
- Dados trabalhistas
- Dados biométricos
- Geolocalização
- Logs técnicos

## 4. Dados sensíveis
- Biometria facial
- Eventuais documentos médicos/anexos

## 5. Finalidades
- Autenticação
- Registro de ponto
- Auditoria
- Cumprimento legal
- Segurança

## 6. Hipóteses legais
- Consentimento, quando aplicável
- Obrigação legal/regulatória
- Execução de contrato
- Exercício regular de direitos
- Legítimo interesse, se usado, com análise específica

## 7. Fluxo de dados
- Coleta
- Armazenamento
- Processamento
- Compartilhamento
- Retenção
- Eliminação

## 8. Compartilhamentos
- AWS S3
- AWS Rekognition, se usado
- Serviço de e-mail
- Provedor de geolocalização
- Infra/VPS

## 9. Riscos
- Vazamento de biometria
- Uso indevido de geolocalização
- Reidentificação
- Acesso indevido por manager
- Retenção excessiva
- Falha de revogação

## 10. Medidas de mitigação
- Cookie HttpOnly
- CSRF
- Tenant isolation
- Mascaramento de logs
- Revogação biométrica
- Retenção por domínio
- Controle de acesso
- Auditoria

## 11. Risco residual
- Baixo/Médio/Alto por item

## 12. Aprovações
- Responsável técnico
- Responsável jurídico
- Encarregado
```

### Critérios de aceite

```text
- Documento RIPD criado
- Inventário inicial criado
- Matriz de retenção criada
- Matriz de base legal criada
- Documentação referenciada no README
```

---

## Feature LGPD-603 — Criar versão pública da política de privacidade

### Tipo

```text
Back-end: opcional
Front-end: sim
Docs: sim
Prioridade: P1
```

### Arquivos prováveis

```text
docs/legal/politica-de-privacidade-publica.md
src/pages/PrivacyPolicy.tsx
src/config/app-routes.ts
src/components/Sidebar.tsx
```

### Conteúdo mínimo

```text
- Quem controla os dados
- Quais dados são tratados
- Para quais finalidades
- Bases legais
- Uso de biometria
- Uso de geolocalização
- Compartilhamento com terceiros
- Retenção
- Direitos do titular
- Como exercer direitos
- Contato do encarregado
- Versão e data
```

### Critérios de aceite

```text
- Usuário acessa política sem precisar aceitar biometria
- Política mostra versão/data
- Link disponível no PrivacyCenter
```

---

# Sprint 7 — Ajustes finais de segurança, QA e release

## Objetivo

Garantir que a implementação está estável, testada e pronta para merge controlado.

---

## Feature LGPD-701 — Testes end-to-end de fluxos LGPD

### Tipo

```text
Back-end: sim
Front-end: sim
Prioridade: P0
```

### Cenários obrigatórios

```text
1. Usuário sem biometria acessa plataforma por senha
2. Usuário sem biometria acessa PrivacyCenter
3. Usuário sem biometria tenta check-in facial e é bloqueado somente nesse fluxo
4. Usuário aceita biometria e consegue check-in facial
5. Usuário revoga biometria e continua logado
6. Usuário cria solicitação LGPD
7. Manager vê solicitação da própria empresa
8. Manager não vê solicitação de outra empresa
9. CTO vê todas
10. Admin conclui solicitação
11. Titular vê histórico público
12. Retenção dry-run não altera dados
13. Retenção apply altera dados permitidos
14. Anonimização remove biometria e dados diretos
```

### Critérios de aceite

```text
- Testes automatizados ou roteiro manual documentado
- Evidências em docs/legal/lgpd-validation-report.md
```

---

## Feature LGPD-702 — Checklist de release LGPD

### Tipo

```text
Docs: sim
Prioridade: P0
```

### Arquivo

```text
docs/legal/lgpd-release-checklist.md
```

### Checklist

```md
# LGPD Release Checklist

## Consentimento biométrico
- [ ] Não bloqueia plataforma inteira
- [ ] Bloqueia apenas fluxo facial
- [ ] Revogação funciona
- [ ] Revogação não encerra sessão por senha

## Retenção
- [ ] Dry-run funciona
- [ ] Apply funciona
- [ ] Dados fiscais/trabalhistas preservados
- [ ] Logs de execução registrados

## Anonimização
- [ ] CPF compatível com banco
- [ ] Biometria removida
- [ ] Usuário desativado
- [ ] Documentos tratados
- [ ] Logs sanitizados

## Solicitações
- [ ] SLA calculado
- [ ] Histórico completo
- [ ] Notas públicas/internas separadas
- [ ] Painel admin funcionando

## Documentação
- [ ] RIPD criado
- [ ] Inventário criado
- [ ] Política pública criada
- [ ] Matriz de retenção criada
```

---

# Ordem recomendada de execução

```text
1. Sprint 0 — Preparação
2. Sprint 1 — Consentimento biométrico granular
3. Sprint 5 — SLA e histórico
4. Sprint 4 — Painel administrativo LGPD
5. Sprint 2 — Retenção real
6. Sprint 3 — Anonimização por domínio
7. Sprint 6 — RIPD e inventário
8. Sprint 7 — QA e release
```

Motivo da ordem:

```text
- Primeiro remover o maior risco: bloqueio global por biometria
- Depois tornar solicitações LGPD operacionalmente tratáveis
- Depois implementar retenção/anonimização, que são mais arriscadas
- Por fim documentar e validar o pacote completo
```

---

# Prompt para Codex aplicar este backlog

```text
Você está trabalhando no projeto Kronos.

Repositórios e branches:
- Back-end: LsaBarbosa/Kronos-Tech-Solutions-KTS
- Branch back-end: feature/lgpd-compliance
- Front-end: LsaBarbosa/Kronos-Tech-Solution-User-Plataform
- Branch front-end: feature/lgpd-compliance

Objetivo:
Concluir a implementação LGPD corrigindo os seguintes pontos:
1. Remover bloqueio global por consentimento biométrico.
2. Aplicar consentimento biométrico apenas em fluxos biométricos.
3. Implementar retenção real, substituindo noop.
4. Corrigir anonimização por domínio.
5. Criar painel administrativo LGPD.
6. Adicionar SLA e histórico completo de atendimento.
7. Criar RIPD e inventário de tratamento.

Regras:
- Não alterar migrations antigas já aplicadas.
- Criar novas migrations Flyway.
- Não remover dados trabalhistas/fiscais sem política explícita.
- Não logar CPF completo, token, senha, base64 de face, storage path sensível ou segredo.
- Respeitar tenant isolation.
- Validar CTO, MANAGER e EMPLOYEE.
- No front-end, usar api.ts e api-routes.ts.
- No back-end, manter arquitetura hexagonal.
- Para cada feature, adicionar testes unitários/WebMvc/front.
- Rodar testes e build antes de concluir.

Comece pela Sprint 1:
- Remover TermsAcceptanceGate como bloqueio global.
- Criar BiometricFeatureGate.
- Aplicar gate apenas em login facial, check-in facial e cadastro facial.
- Ajustar TermsValidationFilter/BiometricProtectionService no back-end.
- Garantir que login por senha, PrivacyCenter, documentos e solicitações LGPD funcionem sem aceite biométrico.
```

---

# Marco mínimo para considerar merge

Antes de mergear para `main`, eu exigiria pelo menos:

```text
Obrigatório:
- Sprint 0
- Sprint 1
- Sprint 5
- Sprint 4 parcial: listagem + detalhe + atualização de status
- Sprint 2 parcial: retenção real para tokens, documentos e logs
- Sprint 3 parcial: anonimização de cadastro, usuário e biometria
- Sprint 6: RIPD e inventário inicial
- Sprint 7: checklist e testes principais
```

Sem isso, a branch continua sendo uma boa base técnica, mas ainda não fecha os principais riscos levantados na análise.

[1]: https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares "Direito dos Titulares"
[2]: https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd "Relatório de Impacto à Proteção de Dados Pessoais (RIPD)"
