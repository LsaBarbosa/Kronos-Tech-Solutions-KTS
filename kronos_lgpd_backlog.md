# Backlog LGPD Kronos

## Projeto

```text
Produto: Kronos
Back-end de referência: Kronos-Tech-Solutions-KTS
Branch back-end: feature/s3-document
Front-end de referência: Kronos-Tech-Solution-User-Plataform
Branch front-end: fix-colaborador
Objetivo: elevar a aderência técnica e operacional do Kronos à LGPD
```

---

## 1. Objetivo deste backlog

Este backlog detalha as tarefas necessárias para transformar as melhorias já existentes nas branches atuais em uma estrutura robusta de LGPD.

A branch `feature/s3-document` já possui melhorias relevantes, como:

- aceite biométrico;
- revogação biométrica no back-end;
- exclusão de imagem facial;
- exclusão de templates do Rekognition;
- cookie HttpOnly;
- CSRF;
- autorização centralizada por domínio;
- validação mais rígida de documentos;
- documentação inicial de retenção.

A branch `fix-colaborador` já possui melhorias relevantes, como:

- `AuthProvider`;
- `ProtectedRoute`;
- `api.ts` centralizado;
- `withCredentials`;
- CSRF no front-end;
- `TermsAcceptanceGate`;
- rotas centralizadas em `api-routes.ts`.

Mesmo assim, ainda falta o módulo LGPD formal:

- consentimento versionado;
- texto legal servido pelo back-end;
- revogação biométrica no front;
- exportação de dados;
- solicitações do titular;
- anonimização;
- retenção executável;
- incidentes de segurança;
- auditoria LGPD ampla;
- telas de privacidade.

---

## 2. Convenções para desenvolvimento

### 2.1 Padrão de task

Cada task deste backlog contém:

```text
ID
Título
Objetivo
Escopo
Arquivos prováveis
Passos técnicos
Critérios de aceite
Testes obrigatórios
Observações para o Codex
```

### 2.2 Prioridade

```text
P0 = obrigatório antes de considerar LGPD minimamente defensável
P1 = necessário para maturidade operacional
P2 = melhoria enterprise / endurecimento
```

### 2.3 Regras para o Codex

Ao desenvolver cada task:

1. Não misturar múltiplos épicos na mesma alteração.
2. Não alterar regras fiscais/trabalhistas sem task explícita.
3. Não remover dados históricos sem validação de retenção.
4. Não logar CPF completo, token, senha, base64 de face, storage path sensível ou segredo.
5. Não criar endpoint público sem rate limit ou justificativa.
6. Sempre adicionar teste unitário ou de integração quando houver alteração de regra.
7. Sempre manter compatibilidade com arquitetura hexagonal do back-end.
8. Sempre usar `api.ts` no front-end para chamadas HTTP internas.
9. Sempre respeitar isolamento por tenant.
10. Sempre validar comportamento para `CTO`, `MANAGER` e colaborador comum.

---

## 3. Roadmap sugerido

```text
Fase 0 - Preparação e saneamento imediato
Fase 1 - Consentimento biométrico versionado
Fase 2 - Revogação biométrica end-to-end
Fase 3 - Documentos, evidências e auditoria
Fase 4 - Solicitações LGPD e exportação de dados
Fase 5 - Anonimização, bloqueio e retenção
Fase 6 - Incidentes de segurança
Fase 7 - Front-end Privacy Center
Fase 8 - Testes, hardening e documentação final
```

---

# EPIC 0 — Preparação e saneamento imediato

## LGPD-0001 — Remover CPF do nome do arquivo do termo biométrico

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** não

### Objetivo

Evitar que o CPF do colaborador apareça no nome do arquivo PDF do termo biométrico.

Hoje o arquivo é gerado com padrão semelhante a:

```java
Termo_Aceite_Biometria_{cpf}.pdf
```

Isso expõe dado pessoal sensível em metadados, logs, listagens, storage path e possíveis downloads.

### Escopo

Alterar a geração do nome do arquivo no aceite biométrico.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/test/java/com/kts/kronos/application/service/AcceptTermsServiceTest.java
```

### Passos técnicos

1. Localizar a criação do filename no método `acceptBiometricTerms`.
2. Substituir CPF por `employeeId` ou por nome fixo sem identificador pessoal.
3. Sugestão:

```java
var filename = "Termo_Aceite_Biometria.pdf";
```

ou:

```java
var filename = String.format(
    "Termo_Aceite_Biometria_%s.pdf",
    employee.employeeId()
);
```

4. Ajustar teste que valide o nome do arquivo.
5. Garantir que a busca do documento recém-gerado continue funcionando.

### Critérios de aceite

- O nome do arquivo não contém CPF.
- O upload do termo continua funcionando.
- O documento ainda é encontrado após o upload.
- Nenhum teste existente quebra.

### Testes obrigatórios

- Deve gerar termo biométrico sem CPF no nome.
- Deve continuar salvando documento `BIOMETRIC_CONSENT_TERM`.
- Deve registrar auditoria do aceite.

### Observações para o Codex

Não alterar o conteúdo do PDF nesta task.  
Não alterar a regra de aceite.  
Apenas remover CPF do filename.

---

## LGPD-0002 — Remover storage path completo do detalhe de auditoria do aceite biométrico

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** não

### Objetivo

Evitar que o caminho completo do storage seja gravado em `AuditLog.details`.

Storage path pode conter estrutura interna, tenant, employeeId e nome de arquivo. Isso não deve ser exposto em logs/auditoria textual livre.

### Escopo

Alterar o detalhe do audit log no aceite biométrico.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/test/java/com/kts/kronos/application/service/AcceptTermsServiceTest.java
```

### Passos técnicos

1. Localizar o trecho:

```java
"Documento gerado e armazenado em: " + persistedDocument.storagePath()
```

2. Substituir por mensagem sem path:

```java
String.format(
    "Consentimento biométrico registrado. documentId=%s, documentType=%s",
    persistedDocument.documentId(),
    persistedDocument.type()
)
```

3. Ajustar testes.
4. Garantir que não haja `storagePath` no `details`.

### Critérios de aceite

- AuditLog de aceite não contém `storagePath`.
- AuditLog contém `documentId`.
- AuditLog contém ação `ACEITE_TERMOS_BIOMETRIA`.
- Não há quebra no fluxo de aceite.

### Testes obrigatórios

- Verificar que `details` não contém `company/`, `employee/`, `storagePath` ou caminho de bucket.
- Verificar que `details` contém `documentId`.

### Observações para o Codex

Não remover a auditoria.  
A task é apenas para reduzir vazamento de informação no campo `details`.

---

## LGPD-0003 — Criar constante de ações de auditoria LGPD

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** não

### Objetivo

Evitar strings soltas como:

```text
ACEITE_TERMOS_BIOMETRIA
REVOGACAO_TERMOS_BIOMETRIA
```

Criar enum ou classe de constantes para eventos LGPD/auditoria.

### Escopo

Criar uma enum para ações auditáveis.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/enuns/AuditAction.java
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/test/java/com/kts/kronos/application/service/AcceptTermsServiceTest.java
```

### Passos técnicos

1. Criar enum:

```java
public enum AuditAction {
    BIOMETRIC_CONSENT_ACCEPTED,
    BIOMETRIC_CONSENT_REVOKED,
    BIOMETRIC_FACE_DELETED,
    BIOMETRIC_TEMPLATE_DELETED,
    DOCUMENT_UPLOADED,
    DOCUMENT_DOWNLOADED,
    DOCUMENT_DELETED,
    LGPD_REQUEST_CREATED,
    LGPD_DATA_EXPORTED,
    LGPD_DATA_ANONYMIZED
}
```

2. Alterar chamadas de `AuditLog.create(...)` para usar `AuditAction.name()`.
3. Ajustar testes.

### Critérios de aceite

- Não há strings hardcoded para aceite/revogação biométrica.
- Testes continuam passando.
- O nome da action fica padronizado.

### Testes obrigatórios

- Aceite biométrico registra action `BIOMETRIC_CONSENT_ACCEPTED`.
- Revogação biométrica registra action `BIOMETRIC_CONSENT_REVOKED`.

### Observações para o Codex

Não alterar a estrutura da tabela `tb_audit_logs` nesta task.

---

# EPIC 1 — Consentimento biométrico versionado

## LGPD-0101 — Criar migration da tabela `tb_legal_consent`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** sim

### Objetivo

Criar uma tabela formal de consentimentos legais.

Hoje o sistema usa a existência de documento `BIOMETRIC_CONSENT_TERM` para inferir aceite. Isso é insuficiente para LGPD porque documento é evidência, mas não deve ser a única fonte de estado jurídico.

### Escopo

Criar migration Flyway com a tabela `tb_legal_consent`.

### Arquivos prováveis

```text
src/main/resources/db/migration/V3__create_legal_consent.sql
src/test/java/com/kts/kronos/adapter/out/persistence/FlywayMigrationTest.java
```

### DDL sugerido

```sql
CREATE TABLE tb_legal_consent (
    consent_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES tb_employee(employee_id),
    user_id UUID REFERENCES tb_user(user_id),
    consent_type VARCHAR(80) NOT NULL,
    legal_basis VARCHAR(80) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    version VARCHAR(30) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    ip_address VARCHAR(80),
    user_agent TEXT,
    evidence_document_id UUID REFERENCES tb_document(document_id),
    evidence_hash_sha256 VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_legal_consent_employee_type
    ON tb_legal_consent(employee_id, consent_type);

CREATE INDEX idx_legal_consent_active
    ON tb_legal_consent(employee_id, consent_type)
    WHERE revoked_at IS NULL;

CREATE UNIQUE INDEX uix_legal_consent_active_by_employee_type
    ON tb_legal_consent(employee_id, consent_type)
    WHERE revoked_at IS NULL;
```

### Passos técnicos

1. Criar arquivo de migration com numeração sequencial correta.
2. Validar se `uuid_generate_v4()` já existe na migration inicial.
3. Criar índices.
4. Rodar teste de migration.
5. Garantir que a migration funciona em PostgreSQL.

### Critérios de aceite

- Tabela criada com sucesso.
- Existe índice para consentimento ativo.
- Não permite dois consentimentos ativos do mesmo tipo para o mesmo colaborador.
- Flyway sobe limpo nos testes.

### Testes obrigatórios

- `FlywayMigrationTest` passa.
- Teste de unicidade parcial para consentimento ativo, se houver teste de repositório.

### Observações para o Codex

Não alterar `AcceptTermsService` nesta task.  
Esta task é apenas banco.

---

## LGPD-0102 — Criar enum `ConsentType`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** não

### Objetivo

Padronizar os tipos de consentimento.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/enuns/ConsentType.java
```

### Implementação sugerida

```java
public enum ConsentType {
    BIOMETRIC_AUTHENTICATION,
    BIOMETRIC_TIME_RECORD,
    PRIVACY_POLICY,
    TERMS_OF_USE
}
```

### Critérios de aceite

- Enum criado.
- Compilação passa.
- Nenhuma regra de negócio alterada.

### Testes obrigatórios

- Não precisa teste unitário isolado, salvo padrão do projeto exigir cobertura de enum.

### Observações para o Codex

Manter nomes em inglês, como o restante do domínio técnico.

---

## LGPD-0103 — Criar enum `LegalBasis`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** não

### Objetivo

Padronizar bases legais usadas pela aplicação.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/enuns/LegalBasis.java
```

### Implementação sugerida

```java
public enum LegalBasis {
    CONSENT,
    LEGAL_OBLIGATION,
    CONTRACT_EXECUTION,
    REGULAR_EXERCISE_OF_RIGHTS,
    FRAUD_PREVENTION,
    LEGITIMATE_INTEREST
}
```

### Critérios de aceite

- Enum criado.
- Compilação passa.
- Nenhuma regra de negócio alterada.

### Observações para o Codex

Não decidir sozinho qual base legal final será usada em todos os fluxos.  
A task apenas cria enum técnico. A definição jurídica final fica documentada em task posterior.

---

## LGPD-0104 — Criar domínio `LegalConsent`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** não

### Objetivo

Criar o modelo de domínio para consentimentos legais.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/LegalConsent.java
```

### Campos sugeridos

```java
public record LegalConsent(
    UUID consentId,
    UUID employeeId,
    UUID userId,
    ConsentType consentType,
    LegalBasis legalBasis,
    String purpose,
    String version,
    Instant grantedAt,
    Instant revokedAt,
    String ipAddress,
    String userAgent,
    UUID evidenceDocumentId,
    String evidenceHashSha256,
    Instant createdAt,
    Instant updatedAt
) {
    public boolean isActive() {
        return revokedAt == null;
    }

    public LegalConsent revoke(Instant revokedAt) {
        return new LegalConsent(
            consentId,
            employeeId,
            userId,
            consentType,
            legalBasis,
            purpose,
            version,
            grantedAt,
            revokedAt,
            ipAddress,
            userAgent,
            evidenceDocumentId,
            evidenceHashSha256,
            createdAt,
            Instant.now()
        );
    }
}
```

### Critérios de aceite

- Modelo criado.
- Possui método `isActive()`.
- Possui método de revogação imutável.
- Compilação passa.

### Testes obrigatórios

```text
LegalConsentTest
- shouldReturnActiveWhenRevokedAtIsNull
- shouldReturnInactiveWhenRevokedAtIsPresent
- shouldCreateRevokedCopy
```

### Observações para o Codex

Seguir o padrão de records/modelos já usado no projeto.

---

## LGPD-0105 — Criar entidade JPA `LegalConsentEntity`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** sim

### Objetivo

Mapear a tabela `tb_legal_consent` para JPA.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/out/persistence/entity/LegalConsentEntity.java
```

### Passos técnicos

1. Criar entidade com `@Entity`.
2. Usar `@Table(name = "tb_legal_consent")`.
3. Mapear enums com `@Enumerated(EnumType.STRING)`.
4. Usar `Instant` ou `OffsetDateTime` para `TIMESTAMPTZ`.
5. Mapear colunas exatamente como a migration.
6. Evitar relacionamento JPA complexo inicialmente; usar UUID nos campos.

### Critérios de aceite

- Entity compila.
- Nome das colunas bate com migration.
- Enums persistem como string.
- Não há cascade perigoso.

### Testes obrigatórios

- Teste de round trip, se o projeto já tiver padrão de entity tests.
- Persistir consentimento ativo.
- Persistir consentimento revogado.

### Observações para o Codex

Não usar cascade delete para employee/user/document.

---

## LGPD-0106 — Criar repository `LegalConsentRepository`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** sim

### Objetivo

Permitir consultar, salvar e revogar consentimentos.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/out/persistence/LegalConsentRepository.java
```

### Métodos sugeridos

```java
Optional<LegalConsentEntity> findByEmployeeIdAndConsentTypeAndRevokedAtIsNull(
    UUID employeeId,
    ConsentType consentType
);

List<LegalConsentEntity> findByEmployeeIdOrderByGrantedAtDesc(UUID employeeId);

boolean existsByEmployeeIdAndConsentTypeAndRevokedAtIsNull(
    UUID employeeId,
    ConsentType consentType
);
```

### Critérios de aceite

- Repository compila.
- Métodos usam consentimento ativo via `revokedAt is null`.
- Não consulta por documento como fonte primária.

### Testes obrigatórios

- Deve encontrar consentimento ativo.
- Não deve retornar consentimento revogado como ativo.
- Deve listar histórico por colaborador.

---

## LGPD-0107 — Criar mapper `LegalConsentMapper`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não

### Objetivo

Converter entre domínio e entidade.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/out/persistence/mapper/LegalConsentMapper.java
```

### Métodos sugeridos

```java
LegalConsent toDomain(LegalConsentEntity entity);
LegalConsentEntity toEntity(LegalConsent domain);
```

### Critérios de aceite

- Mapper cobre todos os campos.
- Não perde `revokedAt`.
- Não perde `evidenceDocumentId`.
- Testes passam.

### Testes obrigatórios

- Entity -> Domain.
- Domain -> Entity.
- Round trip com consentimento ativo.
- Round trip com consentimento revogado.

---

## LGPD-0108 — Criar provider `LegalConsentProvider`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não

### Objetivo

Manter arquitetura hexagonal e evitar uso direto do repository na aplicação.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/port/out/provider/LegalConsentProvider.java
src/main/java/com/kts/kronos/adapter/out/persistence/impl/LegalConsentProviderImpl.java
```

### Interface sugerida

```java
public interface LegalConsentProvider {
    LegalConsent save(LegalConsent consent);
    Optional<LegalConsent> findActive(UUID employeeId, ConsentType type);
    boolean existsActive(UUID employeeId, ConsentType type);
    List<LegalConsent> findAllByEmployeeId(UUID employeeId);
}
```

### Critérios de aceite

- Application layer não usa repository direto.
- Provider implementado.
- Testes passam.

### Testes obrigatórios

- Save.
- Find active.
- Exists active.
- List history.

---

## LGPD-0109 — Alterar aceite biométrico para gravar `tb_legal_consent`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não  
**Banco:** sim

### Objetivo

Ao aceitar o termo biométrico, o sistema deve salvar:

1. PDF de evidência em `tb_document`;
2. registro jurídico em `tb_legal_consent`;
3. auditoria.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/main/java/com/kts/kronos/application/port/out/provider/LegalConsentProvider.java
src/test/java/com/kts/kronos/application/service/AcceptTermsServiceTest.java
```

### Passos técnicos

1. Injetar `LegalConsentProvider`.
2. Antes do aceite, verificar `legalConsentProvider.existsActive(employeeId, BIOMETRIC_AUTHENTICATION)` ou tipo definido.
3. Gerar PDF.
4. Salvar documento.
5. Calcular hash SHA-256 do PDF.
6. Salvar `LegalConsent`.
7. Registrar audit log.
8. Retornar normalmente.

### Critérios de aceite

- Aceite cria documento.
- Aceite cria consentimento ativo.
- Aceite não permite duplicidade de consentimento ativo.
- Aceite registra hash do PDF.
- Aceite registra versão do termo.
- Aceite registra IP e user-agent.

### Testes obrigatórios

```text
shouldCreateLegalConsentWhenAcceptingBiometricTerms
shouldNotCreateDuplicatedActiveConsent
shouldStoreEvidenceDocumentId
shouldStoreEvidenceHash
shouldStoreIpAndUserAgent
```

### Observações para o Codex

Não remover ainda a lógica antiga de documento se outros filtros dependerem dela.  
Mas o estado jurídico deve passar a vir da tabela `tb_legal_consent`.

---

## LGPD-0110 — Alterar `hasAcceptedBiometricTerm` para consultar consentimento ativo

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** não

### Objetivo

Parar de usar existência de documento como fonte de verdade.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/test/java/com/kts/kronos/application/service/AcceptTermsServiceTest.java
```

### Regra nova

Antes:

```java
return documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM);
```

Depois:

```java
return legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION);
```

### Critérios de aceite

- `GET /terms/status` retorna `true` quando existe consentimento ativo.
- Retorna `false` quando o consentimento foi revogado.
- Documento antigo sem consentimento ativo não deve liberar acesso.

### Testes obrigatórios

- Documento existe, mas consentimento não existe: retorna false.
- Consentimento ativo existe: retorna true.
- Consentimento revogado existe: retorna false.

---

## LGPD-0111 — Criar migration de backfill para consentimentos biométricos existentes

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Criar consentimentos em `tb_legal_consent` para usuários que já possuem documento `BIOMETRIC_CONSENT_TERM`.

### Escopo

Criar script SQL idempotente.

### Arquivos prováveis

```text
src/main/resources/db/migration/V4__backfill_biometric_legal_consent.sql
```

### Regra sugerida

Para cada documento `BIOMETRIC_CONSENT_TERM`, criar consentimento se ainda não existir consentimento ativo.

Campos:

```text
employee_id = tb_document.employee_id
consent_type = BIOMETRIC_AUTHENTICATION
legal_basis = CONSENT
purpose = Autenticação biométrica e validação de identidade
version = legacy-unknown ou versão atual definida
granted_at = tb_document.uploaded_at
evidence_document_id = tb_document.document_id
```

### Critérios de aceite

- Migration pode rodar mais de uma vez sem duplicar.
- Não quebra se não houver documentos.
- Cria consentimentos apenas quando necessário.

### Testes obrigatórios

- Teste de migration com documento legado.
- Teste sem documento legado.
- Teste com consentimento já existente.

### Observações para o Codex

Não inventar data de aceite se não existir; usar `uploaded_at` como aproximação técnica documentada.

---

# EPIC 2 — Texto legal versionado servido pelo back-end

## LGPD-0201 — Criar tabela `tb_legal_text`

**Prioridade:** P0  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Centralizar os textos legais no back-end para evitar divergência entre front, PDF e banco.

### Arquivos prováveis

```text
src/main/resources/db/migration/V5__create_legal_text.sql
```

### DDL sugerido

```sql
CREATE TABLE tb_legal_text (
    legal_text_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_type VARCHAR(80) NOT NULL,
    version VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    content_hash_sha256 VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uix_legal_text_type_version
    ON tb_legal_text(document_type, version);

CREATE UNIQUE INDEX uix_legal_text_active_by_type
    ON tb_legal_text(document_type)
    WHERE active = true;
```

### Critérios de aceite

- Tabela criada.
- Apenas uma versão ativa por tipo.
- Conteúdo possui hash.

### Testes obrigatórios

- Migration sobe.
- Índice parcial impede duas versões ativas.

---

## LGPD-0202 — Criar domínio `LegalText`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Representar textos legais versionados.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/LegalText.java
src/main/java/com/kts/kronos/domain/model/enuns/LegalTextType.java
```

### Enum sugerido

```java
public enum LegalTextType {
    BIOMETRIC_CONSENT_TERM,
    PRIVACY_POLICY,
    TERMS_OF_USE
}
```

### Record sugerido

```java
public record LegalText(
    UUID legalTextId,
    LegalTextType documentType,
    String version,
    String title,
    String content,
    String contentHashSha256,
    boolean active,
    Instant createdAt,
    Instant publishedAt
) {}
```

### Critérios de aceite

- Modelo criado.
- Compilação passa.

---

## LGPD-0203 — Criar repository/provider para `LegalText`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Permitir consultar texto legal ativo por tipo.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/out/persistence/entity/LegalTextEntity.java
src/main/java/com/kts/kronos/adapter/out/persistence/LegalTextRepository.java
src/main/java/com/kts/kronos/adapter/out/persistence/mapper/LegalTextMapper.java
src/main/java/com/kts/kronos/application/port/out/provider/LegalTextProvider.java
src/main/java/com/kts/kronos/adapter/out/persistence/impl/LegalTextProviderImpl.java
```

### Métodos sugeridos

```java
Optional<LegalText> findActiveByType(LegalTextType type);
LegalText save(LegalText legalText);
```

### Critérios de aceite

- Consulta texto ativo.
- Não retorna texto inativo.
- Testes passam.

---

## LGPD-0204 — Criar seed do termo biométrico atual

**Prioridade:** P0  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Inserir a primeira versão ativa do termo biométrico na migration.

### Arquivo provável

```text
src/main/resources/db/migration/V6__seed_biometric_legal_text.sql
```

### Conteúdo mínimo

O texto deve conter:

```text
- identificação do tratamento biométrico
- finalidade
- base legal
- quais dados são tratados
- armazenamento
- uso de provedor externo, se aplicável
- possibilidade de revogação, quando aplicável
- consequências da revogação
- contato/canal LGPD
- versão
```

### Critérios de aceite

- Existe termo ativo `BIOMETRIC_CONSENT_TERM`.
- Existe hash do conteúdo.
- Não há dois termos ativos.

### Observações para o Codex

Não escrever texto jurídico definitivo se ele não existir.  
Inserir texto técnico inicial marcado como versão controlada, por exemplo `2026.05.01`.

---

## LGPD-0205 — Criar endpoint `GET /terms/biometric/current`

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** sim, em task posterior

### Objetivo

Permitir que o front-end busque o texto oficial do termo biométrico.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/in/web/http/TermsController.java
src/main/java/com/kts/kronos/adapter/in/web/dto/terms/LegalTextResponse.java
src/main/java/com/kts/kronos/application/port/in/usecase/LegalTextUseCase.java
src/main/java/com/kts/kronos/application/service/LegalTextService.java
```

### Endpoint

```http
GET /terms/biometric/current
```

### Resposta

```json
{
  "type": "BIOMETRIC_CONSENT_TERM",
  "version": "2026.05.01",
  "title": "Termo de Consentimento Biométrico",
  "content": "...",
  "contentHashSha256": "...",
  "active": true
}
```

### Critérios de aceite

- Endpoint autenticado ou público conforme decisão do projeto.
- Retorna apenas termo ativo.
- Retorna 404 se não houver termo ativo.
- Não retorna termos inativos.

### Testes obrigatórios

- Deve retornar termo ativo.
- Deve retornar 404 sem termo ativo.
- Deve retornar hash.

### Observações para o Codex

Preferencialmente liberar endpoint para usuário autenticado, já que será usado no gate pós-login.

---

## LGPD-0206 — Alterar `TermsAcceptanceGate` para consumir texto do back-end

**Prioridade:** P0  
**Front-end:** sim  
**Back-end:** depende da task LGPD-0205

### Objetivo

Remover texto hardcoded do termo biométrico no front-end.

### Arquivos prováveis

```text
src/components/TermsAcceptanceGate.tsx
src/service/terms.service.ts
src/types/terms.ts
```

### Passos técnicos

1. Criar função:

```ts
export const getCurrentBiometricTerm = async (): Promise<LegalTextResponse> => {
  const response = await api.get(
    buildRoute(API_ROUTES.TERMS, "biometric", "current")
  );

  return response.data;
};
```

2. Criar tipo `LegalTextResponse`.
3. No `TermsAcceptanceGate`, carregar o texto junto com status.
4. Renderizar `title` e `content` vindos da API.
5. Manter exigência de scroll até o fim.
6. Se falhar, bloquear acesso com mensagem clara.

### Critérios de aceite

- Nenhum texto jurídico longo fica hardcoded no componente.
- O termo exibido é o termo retornado pelo back-end.
- O aceite continua funcionando.
- O gate mostra loading enquanto busca status e texto.

### Testes obrigatórios

- Renderiza termo vindo da API.
- Bloqueia aceite antes do scroll.
- Bloqueia aceite antes do checkbox.
- Aceita termo com sucesso.
- Exibe erro se API de termo falhar.

---

## LGPD-0207 — Incluir versão do termo no aceite

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** sim

### Objetivo

Garantir que o aceite registre a versão exata do termo que o usuário visualizou.

### Alteração de contrato

Antes:

```http
POST /terms/accept-biometric
```

Sem body.

Depois:

```http
POST /terms/accept-biometric
Content-Type: application/json

{
  "version": "2026.05.01",
  "contentHashSha256": "..."
}
```

### Arquivos prováveis back-end

```text
src/main/java/com/kts/kronos/adapter/in/web/http/TermsController.java
src/main/java/com/kts/kronos/adapter/in/web/dto/terms/AcceptBiometricTermRequest.java
src/main/java/com/kts/kronos/application/port/in/usecase/AcceptTermsUseCase.java
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
```

### Arquivos prováveis front-end

```text
src/service/terms.service.ts
src/components/TermsAcceptanceGate.tsx
```

### Critérios de aceite

- Front envia versão e hash.
- Back valida se versão/hash correspondem ao termo ativo.
- Back grava versão em `tb_legal_consent`.
- Aceite falha se versão/hash estiverem incorretos.

### Testes obrigatórios

- Aceite com versão correta: 204.
- Aceite com versão inexistente: 400 ou 409.
- Aceite com hash divergente: 409.
- Consentimento salvo com versão correta.

---

# EPIC 3 — Revogação biométrica end-to-end

## LGPD-0301 — Alterar revogação para não apagar PDF de aceite sem retenção

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Na revogação biométrica, apagar imagem facial e template biométrico, mas preservar evidência histórica do aceite, salvo regra específica de retenção.

Hoje o fluxo remove documentos `BIOMETRIC_CONSENT_TERM`. Isso pode eliminar evidência jurídica.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/AcceptTermsService.java
src/test/java/com/kts/kronos/application/service/AcceptTermsServiceTest.java
```

### Regra nova

Na revogação:

| Artefato | Ação |
|---|---|
| Imagem facial S3 | deletar |
| Template Rekognition | deletar |
| `faceS3ObjectKey` | limpar |
| `tb_legal_consent` | marcar `revoked_at` |
| PDF de aceite | manter como evidência |
| Documento visível | pode ficar oculto posteriormente por regra de retenção, mas não deletar nesta task |

### Critérios de aceite

- Revogação não chama `documentProvider.delete` para termo biométrico.
- Revogação marca consentimento como revogado.
- Revogação remove biometria técnica.
- Revogação registra auditoria.

### Testes obrigatórios

- Deve revogar consentimento ativo.
- Não deve deletar documento de evidência.
- Deve deletar imagem facial.
- Deve deletar templates Rekognition.
- Deve limpar `faceS3ObjectKey`.

---

## LGPD-0302 — Implementar `revokeBiometricTerms` no front-end

**Prioridade:** P0  
**Front-end:** sim

### Objetivo

Disponibilizar chamada de revogação no service do front-end.

### Arquivos prováveis

```text
src/service/terms.service.ts
src/config/api-routes.ts
```

### Implementação sugerida

```ts
export const revokeBiometricTerms = async (): Promise<void> => {
  const response = await api.delete(
    buildRoute(API_ROUTES.TERMS, TERMS_PATHS.REVOKE_BIOMETRIC)
  );

  if (response.status !== 204) {
    throw new Error("Falha ao revogar o consentimento biométrico.");
  }

  invalidateCsrfToken();
};
```

### Critérios de aceite

- Service compila.
- Usa `api`.
- Usa `TERMS_PATHS.REVOKE_BIOMETRIC`.
- Invalida CSRF após sucesso.
- Não usa `fetch`.

### Testes obrigatórios

- Deve chamar `DELETE /terms/revoke-biometric`.
- Deve resolver em sucesso com 204.
- Deve lançar erro em status inesperado.

---

## LGPD-0303 — Criar seção de privacidade no perfil do usuário

**Prioridade:** P0  
**Front-end:** sim

### Objetivo

Criar uma seção visual para o usuário consultar e revogar biometria.

### Arquivos prováveis

```text
src/pages/Usuario.tsx
src/components/privacy/BiometricConsentCard.tsx
src/service/terms.service.ts
```

### Componente sugerido

```text
BiometricConsentCard
```

### Conteúdo da tela

Exibir:

```text
Status do consentimento biométrico: Ativo/Inativo
Botão: Revogar consentimento biométrico
Mensagem de impacto
Data do aceite, se disponível futuramente
Versão aceita, se disponível futuramente
```

### Critérios de aceite

- Usuário consegue ver status.
- Usuário consegue iniciar revogação.
- Sistema exibe confirmação antes da revogação.
- Após revogar, sessão é atualizada ou usuário é redirecionado.
- Mensagem explica que login facial e validação biométrica podem parar de funcionar.

### Testes obrigatórios

- Renderiza status ativo.
- Renderiza status inativo.
- Abre diálogo de confirmação.
- Chama service ao confirmar.
- Mostra erro em falha.
- Atualiza tela em sucesso.

---

## LGPD-0304 — Criar modal de confirmação de revogação biométrica

**Prioridade:** P0  
**Front-end:** sim

### Objetivo

Evitar revogação acidental.

### Arquivos prováveis

```text
src/components/privacy/RevokeBiometricConsentDialog.tsx
```

### Texto mínimo

```text
Revogar consentimento biométrico

Ao confirmar, sua imagem facial e os templates biométricos usados para reconhecimento serão removidos dos provedores configurados.
O login facial e validações biométricas deixarão de funcionar.
Seu histórico de aceite poderá ser preservado como evidência legal conforme política de retenção.

Deseja continuar?
```

### Critérios de aceite

- Modal exige confirmação explícita.
- Botão destrutivo visualmente destacado.
- Botão cancelar fecha modal.
- Botão confirmar chama callback.

### Testes obrigatórios

- Cancelar não chama revogação.
- Confirmar chama revogação.
- Loading impede clique duplicado.

---

## LGPD-0305 — Atualizar sessão após revogação biométrica

**Prioridade:** P0  
**Front-end:** sim  
**Back-end:** já retorna cookie novo

### Objetivo

Após revogar, atualizar estado da sessão para refletir que `accepted=false`.

### Arquivos prováveis

```text
src/context/AuthContext.tsx
src/components/privacy/BiometricConsentCard.tsx
```

### Passos técnicos

1. Após `revokeBiometricTerms()`, chamar `checkSession()`.
2. Se o gate bloquear, redirecionar corretamente ou exibir estado controlado.
3. Evitar loop infinito entre perfil e gate.

### Critérios de aceite

- Após revogação, status do termo muda.
- Cookie é atualizado pelo back-end.
- Front não continua exibindo consentimento ativo.
- Usuário não fica em estado inconsistente.

### Testes obrigatórios

- Após revogação, `checkSession` é chamado.
- UI mostra status inativo.
- Falha de sessão é tratada.

---

# EPIC 4 — Documentos, evidências e integridade

## LGPD-0401 — Adicionar `checksum_sha256` em `tb_document`

**Prioridade:** P0  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Permitir comprovar integridade de documentos armazenados.

### Migration sugerida

```sql
ALTER TABLE tb_document
ADD COLUMN checksum_sha256 VARCHAR(128);

CREATE INDEX idx_document_checksum_sha256
    ON tb_document(checksum_sha256);
```

### Arquivos prováveis

```text
src/main/resources/db/migration/V7__add_document_checksum.sql
```

### Critérios de aceite

- Coluna criada.
- Migration passa.
- Campo aceita null temporariamente para documentos antigos.

### Observações para o Codex

Não tornar `NOT NULL` agora para evitar quebra em documentos legados.

---

## LGPD-0402 — Adicionar `checksumSha256` no domínio `Document`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Levar o checksum para o modelo de domínio.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/Document.java
src/main/java/com/kts/kronos/adapter/out/persistence/entity/DocumentEntity.java
src/main/java/com/kts/kronos/adapter/out/persistence/mapper/DocumentMapper.java
```

### Critérios de aceite

- Campo mapeado em entidade.
- Campo mapeado no domínio.
- Mapper cobre ida e volta.
- Testes atualizados.

### Testes obrigatórios

- Mapper preserva checksum.
- Entity round trip preserva checksum.

---

## LGPD-0403 — Calcular checksum no upload de documentos

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Calcular SHA-256 dos bytes do arquivo antes do upload e salvar no metadado.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/DocumentService.java
```

### Passos técnicos

1. Criar utilitário:

```java
String sha256Hex(byte[] bytes)
```

2. Calcular hash em:
   - `uploadDocumentInternal`;
   - `uploadGeneratedDocument`.

3. Salvar no novo campo `checksumSha256`.

### Critérios de aceite

- Upload manual salva checksum.
- Documento gerado salva checksum.
- Hash é calculado sobre bytes exatos enviados ao storage.

### Testes obrigatórios

- Upload PDF salva SHA-256 esperado.
- Documento gerado salva SHA-256 esperado.

---

## LGPD-0404 — Exibir checksum em resposta administrativa de documento

**Prioridade:** P1  
**Back-end:** sim  
**Front-end:** opcional

### Objetivo

Permitir auditoria de integridade documental.

### Regra

- Colaborador comum não precisa ver hash.
- Manager/CTO pode ver em tela administrativa ou endpoint futuro.

### Critérios de aceite

- DTO administrativo inclui checksum.
- DTO comum pode omitir, se preferido.
- Não quebra contratos existentes sem necessidade.

---

## LGPD-0405 — Criar política por tipo documental

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Definir comportamento por `DocumentType`.

### Criar classe

```text
DocumentRetentionPolicyResolver
```

### Regras mínimas

```java
public boolean requiresLegalHold(DocumentType type)
public boolean allowsPhysicalDeletion(DocumentType type)
public boolean shouldUseObjectLock(DocumentType type)
public Duration retentionPeriod(DocumentType type)
```

### Critérios de aceite

- Tipos legais/fiscais podem ter retenção maior.
- Biometria técnica deve permitir exclusão.
- Documento de evidência biométrica não deve ser apagado automaticamente na revogação.
- Testes cobrem cada tipo crítico.

---

# EPIC 5 — Solicitações LGPD

## LGPD-0501 — Criar tabela `tb_lgpd_request`

**Prioridade:** P0  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Registrar solicitações do titular de dados.

### Migration sugerida

```sql
CREATE TABLE tb_lgpd_request (
    request_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_employee_id UUID REFERENCES tb_employee(employee_id),
    target_employee_id UUID REFERENCES tb_employee(employee_id),
    company_id UUID REFERENCES tb_company(company_id),
    request_type VARCHAR(80) NOT NULL,
    status VARCHAR(80) NOT NULL,
    description TEXT,
    response TEXT,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    handled_by_user_id UUID REFERENCES tb_user(user_id)
);

CREATE INDEX idx_lgpd_request_company_status
    ON tb_lgpd_request(company_id, status);

CREATE INDEX idx_lgpd_request_target_employee
    ON tb_lgpd_request(target_employee_id);

CREATE INDEX idx_lgpd_request_opened_at
    ON tb_lgpd_request(opened_at);
```

### Critérios de aceite

- Tabela criada.
- Índices criados.
- Migration passa.

---

## LGPD-0502 — Criar tabela `tb_lgpd_request_history`

**Prioridade:** P0  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Guardar histórico de alterações de status da solicitação.

### Migration sugerida

```sql
CREATE TABLE tb_lgpd_request_history (
    history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL REFERENCES tb_lgpd_request(request_id),
    status VARCHAR(80) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_user_id UUID REFERENCES tb_user(user_id)
);

CREATE INDEX idx_lgpd_request_history_request
    ON tb_lgpd_request_history(request_id, created_at);
```

### Critérios de aceite

- Toda mudança de status poderá ser rastreada.
- Migration passa.

---

## LGPD-0503 — Criar enums de solicitação LGPD

**Prioridade:** P0  
**Back-end:** sim

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/domain/model/enuns/LgpdRequestType.java
src/main/java/com/kts/kronos/domain/model/enuns/LgpdRequestStatus.java
```

### Tipos sugeridos

```java
public enum LgpdRequestType {
    CONFIRM_PROCESSING,
    ACCESS,
    CORRECTION,
    ANONYMIZATION,
    BLOCKING,
    DELETION,
    PORTABILITY,
    CONSENT_REVOCATION,
    SHARING_INFORMATION
}
```

### Status sugeridos

```java
public enum LgpdRequestStatus {
    OPEN,
    IN_ANALYSIS,
    WAITING_CONTROLLER,
    WAITING_LEGAL_REVIEW,
    COMPLETED,
    REJECTED,
    PARTIALLY_COMPLETED
}
```

### Critérios de aceite

- Enums criados.
- Compilação passa.

---

## LGPD-0504 — Criar domínio `LgpdRequest`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Representar solicitação LGPD.

### Arquivo provável

```text
src/main/java/com/kts/kronos/domain/model/LgpdRequest.java
```

### Métodos de domínio

```java
public boolean isClosed()
public LgpdRequest changeStatus(LgpdRequestStatus newStatus, UUID handledByUserId)
public LgpdRequest complete(String response, UUID handledByUserId)
public LgpdRequest reject(String response, UUID handledByUserId)
```

### Critérios de aceite

- Não permite alteração inválida depois de fechado, ou regra equivalente.
- Modelo imutável.
- Testes cobrem transições básicas.

---

## LGPD-0505 — Criar entity, mapper, repository e provider de LGPD request

**Prioridade:** P0  
**Back-end:** sim

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/out/persistence/entity/LgpdRequestEntity.java
src/main/java/com/kts/kronos/adapter/out/persistence/entity/LgpdRequestHistoryEntity.java
src/main/java/com/kts/kronos/adapter/out/persistence/LgpdRequestRepository.java
src/main/java/com/kts/kronos/adapter/out/persistence/LgpdRequestHistoryRepository.java
src/main/java/com/kts/kronos/adapter/out/persistence/mapper/LgpdRequestMapper.java
src/main/java/com/kts/kronos/application/port/out/provider/LgpdRequestProvider.java
src/main/java/com/kts/kronos/adapter/out/persistence/impl/LgpdRequestProviderImpl.java
```

### Provider sugerido

```java
public interface LgpdRequestProvider {
    LgpdRequest save(LgpdRequest request);
    Optional<LgpdRequest> findById(UUID requestId);
    Page<LgpdRequest> findByCompanyId(UUID companyId, LgpdRequestStatus status, Pageable pageable);
    List<LgpdRequestHistory> findHistory(UUID requestId);
    void saveHistory(LgpdRequestHistory history);
}
```

### Critérios de aceite

- CRUD básico funcionando.
- Histórico persiste.
- Paginação por empresa e status.

### Testes obrigatórios

- Criar solicitação.
- Atualizar status.
- Listar por empresa.
- Histórico ordenado por data.

---

## LGPD-0506 — Criar DTOs de solicitação LGPD

**Prioridade:** P0  
**Back-end:** sim

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/CreateLgpdRequest.java
src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdRequestResponse.java
src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/UpdateLgpdRequestStatusRequest.java
src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdRequestHistoryResponse.java
```

### Create request

```java
public record CreateLgpdRequest(
    UUID targetEmployeeId,
    LgpdRequestType requestType,
    String description
) {}
```

### Update status

```java
public record UpdateLgpdRequestStatusRequest(
    LgpdRequestStatus status,
    String response,
    String note
) {}
```

### Critérios de aceite

- Validações com Bean Validation.
- `requestType` obrigatório.
- `description` com tamanho máximo.
- `targetEmployeeId` opcional para colaborador comum, mas obrigatório para manager/CTO em solicitações de terceiros.

---

## LGPD-0507 — Criar `LgpdRequestService`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Implementar regras de criação, listagem e atualização de solicitações.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/port/in/usecase/LgpdRequestUseCase.java
src/main/java/com/kts/kronos/application/service/LgpdRequestService.java
```

### Regras

1. Colaborador comum só pode abrir solicitação para si.
2. Manager pode abrir/consultar solicitações de colaboradores da mesma empresa.
3. CTO pode consultar globalmente.
4. Toda criação deve registrar histórico `OPEN`.
5. Toda mudança de status deve registrar histórico.
6. Solicitação fechada não deve ser alterada, exceto por CTO se regra futura permitir.

### Critérios de aceite

- Criação funciona para próprio usuário.
- Manager não cria solicitação para colaborador de outra empresa.
- Histórico é criado.
- Listagem respeita tenant.

### Testes obrigatórios

```text
employeeCanCreateOwnLgpdRequest
employeeCannotCreateForAnotherEmployee
managerCanCreateForSameCompanyEmployee
managerCannotCreateForOtherCompanyEmployee
statusChangeCreatesHistory
closedRequestCannotBeChanged
```

---

## LGPD-0508 — Criar `LgpdController`

**Prioridade:** P0  
**Back-end:** sim

### Endpoints

```http
POST   /lgpd/requests
GET    /lgpd/requests
GET    /lgpd/requests/{requestId}
PATCH  /lgpd/requests/{requestId}/status
GET    /lgpd/requests/{requestId}/history
```

### Arquivo provável

```text
src/main/java/com/kts/kronos/adapter/in/web/http/LgpdController.java
```

### Autorização

```text
POST /lgpd/requests = ANY_EMPLOYEE
GET /lgpd/requests = MANAGER/CTO ou próprio usuário conforme filtro
PATCH /lgpd/requests/{id}/status = MANAGER/CTO
```

### Critérios de aceite

- Endpoints criados.
- Respostas padronizadas.
- Erros seguem `ProblemDetail`.
- Tenant respeitado.

### Testes obrigatórios

- WebMvc para cada endpoint.
- 403 quando colaborador tenta alterar status.
- 404/403 para acesso cross-tenant.

---

# EPIC 6 — Exportação de dados do titular

## LGPD-0601 — Criar DTO `LgpdDataExportResponse`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Criar estrutura consolidada de exportação.

### Arquivo provável

```text
src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdDataExportResponse.java
```

### Estrutura sugerida

```java
public record LgpdDataExportResponse(
    EmployeeData employee,
    UserData user,
    CompanyData company,
    List<DocumentData> documents,
    List<TimeRecordData> timeRecords,
    List<MessageData> messages,
    List<AuditLogData> auditLogs,
    List<LegalConsentData> consents,
    BiometricData biometric
) {}
```

### Critérios de aceite

- DTO compila.
- Não expõe senha/hash de senha.
- Não expõe token.
- Não expõe segredo interno.
- Não inclui bytes de documentos, apenas metadados inicialmente.

---

## LGPD-0602 — Criar `LgpdDataExportUseCase`

**Prioridade:** P0  
**Back-end:** sim

### Arquivo provável

```text
src/main/java/com/kts/kronos/application/port/in/usecase/LgpdDataExportUseCase.java
```

### Método

```java
LgpdDataExportResponse exportEmployeeData(UUID employeeId);
```

### Critérios de aceite

- Interface criada.
- Será implementada na task seguinte.

---

## LGPD-0603 — Implementar `LgpdDataExportService`

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Consolidar dados do titular.

### Arquivo provável

```text
src/main/java/com/kts/kronos/application/service/LgpdDataExportService.java
```

### Dados a coletar

| Domínio | Fonte provável |
|---|---|
| Employee | `EmployeeProvider` |
| User | `UserProvider` |
| Company | `CompanyProvider` |
| Documents | `DocumentProvider` |
| TimeRecords | `TimeRecordProvider` ou repository/provider existente |
| Messages | `MessageProvider` |
| AuditLogs | `AuditLogProvider` |
| Consents | `LegalConsentProvider` |
| Biometric | `faceS3ObjectKey`, consentimento, status Rekognition se possível |

### Regras

1. Colaborador comum exporta apenas seus dados.
2. Manager exporta dados de colaborador da mesma empresa.
3. CTO exporta globalmente.
4. Exportação não inclui:
   - senha;
   - token;
   - secrets;
   - conteúdo binário dos documentos;
   - faceImageBase64;
   - storage path completo, salvo se for necessário internamente.
5. Registrar audit log `LGPD_DATA_EXPORTED`.

### Critérios de aceite

- Exportação retorna dados consolidados.
- Cross-tenant bloqueado.
- Não vaza senha.
- Não vaza token.
- Auditoria registrada.

### Testes obrigatórios

```text
employeeCanExportOwnData
employeeCannotExportAnotherEmployeeData
managerCanExportSameCompanyEmployeeData
managerCannotExportOtherCompanyEmployeeData
exportDoesNotContainPassword
exportDoesNotContainJwt
exportCreatesAuditLog
```

---

## LGPD-0604 — Criar endpoint `GET /lgpd/employees/{employeeId}/export`

**Prioridade:** P0  
**Back-end:** sim

### Arquivo provável

```text
src/main/java/com/kts/kronos/adapter/in/web/http/LgpdController.java
```

### Endpoint

```http
GET /lgpd/employees/{employeeId}/export
```

### Critérios de aceite

- Retorna JSON.
- Usa service de exportação.
- Respeita autorização.
- Registra auditoria no service.

### Testes obrigatórios

- 200 para próprio usuário.
- 200 para manager mesma empresa.
- 403/404 para cross-tenant.
- JSON não contém password.

---

## LGPD-0605 — Criar opção de download JSON no front-end

**Prioridade:** P1  
**Front-end:** sim

### Objetivo

Permitir que o usuário baixe seus dados em JSON.

### Arquivos prováveis

```text
src/pages/PrivacyCenter.tsx
src/service/lgpd.service.ts
src/config/api-routes.ts
```

### Service

```ts
export const exportMyLgpdData = async (employeeId: string): Promise<Blob> => {
  const response = await api.get(
    buildRoute(API_ROUTES.LGPD, "employees", employeeId, "export"),
    { responseType: "blob" }
  );
  return response.data;
};
```

### Critérios de aceite

- Botão baixa arquivo `.json`.
- Nome do arquivo não contém CPF.
- Erros são exibidos via toast.
- Usa `api.ts`.

---

# EPIC 7 — Anonimização, bloqueio e exclusão controlada

## LGPD-0701 — Criar tabela `tb_anonymization_job`

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Registrar execuções de anonimização.

### DDL sugerido

```sql
CREATE TABLE tb_anonymization_job (
    job_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES tb_employee(employee_id),
    requested_by_user_id UUID REFERENCES tb_user(user_id),
    reason TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    error_message TEXT
);
```

### Critérios de aceite

- Migration passa.
- Jobs podem ser auditados.

---

## LGPD-0702 — Criar service `EmployeeAnonymizationService`

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Anonimizar dados pessoais sem quebrar obrigações legais/fiscais.

### Arquivo provável

```text
src/main/java/com/kts/kronos/application/service/EmployeeAnonymizationService.java
```

### Regras por campo

| Campo | Ação |
|---|---|
| `fullName` | `ANONYMIZED-{employeeId}` |
| `cpf` | hash irreversível ou marcador controlado |
| `email` | `anon-{employeeId}@deleted.local` |
| `phone` | null |
| `street` | null |
| `number` | null |
| `postalCode` | null se permitido |
| `city` | null se permitido |
| `state` | null se permitido |
| `pis` | null/hash conforme retenção |
| `salary` | avaliar retenção; inicialmente manter se obrigação trabalhista exigir |
| `faceS3ObjectKey` | null após deletar imagem |
| `isActive` | false |

### Critérios de aceite

- Dados pessoais diretos são removidos/mascarados.
- Usuário associado é desativado.
- Biometria é removida.
- Registros trabalhistas não são apagados indevidamente.
- Auditoria é registrada.

### Testes obrigatórios

```text
shouldAnonymizeDirectPersonalData
shouldDeactivateUser
shouldDeleteBiometricArtifacts
shouldKeepTimeRecords
shouldCreateAuditLog
```

---

## LGPD-0703 — Criar endpoint de anonimização

**Prioridade:** P1  
**Back-end:** sim

### Endpoint

```http
POST /lgpd/employees/{employeeId}/anonymize
```

### Body

```json
{
  "reason": "Solicitação do titular aprovada"
}
```

### Autorização

```text
MANAGER ou CTO
```

### Critérios de aceite

- Colaborador comum não anonimiza diretamente.
- Manager só anonimiza colaborador da mesma empresa.
- CTO pode anonimizar qualquer colaborador.
- Retorna 202 ou 204.
- Registra auditoria.

### Testes obrigatórios

- 403 para colaborador.
- 403/404 cross-tenant.
- 204/202 sucesso manager mesma empresa.
- Auditoria registrada.

---

## LGPD-0704 — Criar bloqueio de tratamento

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### Objetivo

Permitir bloquear temporariamente tratamento de dados enquanto solicitação LGPD é analisada.

### Migration sugerida

```sql
ALTER TABLE tb_employee
ADD COLUMN processing_blocked_at TIMESTAMPTZ NULL,
ADD COLUMN processing_blocked_reason TEXT NULL;
```

### Regras

- Bloqueio não deve impedir obrigações legais/fiscais obrigatórias.
- Bloqueio deve impedir operações não essenciais.
- Bloqueio deve ser visível no painel administrativo.

### Critérios de aceite

- Campo criado.
- Service consegue bloquear/desbloquear.
- Audit log registra bloqueio.

---

## LGPD-0705 — Criar endpoint de bloqueio/desbloqueio

**Prioridade:** P1  
**Back-end:** sim

### Endpoints

```http
POST   /lgpd/employees/{employeeId}/block-processing
DELETE /lgpd/employees/{employeeId}/block-processing
```

### Critérios de aceite

- Manager mesma empresa pode bloquear.
- CTO pode bloquear.
- Colaborador comum não pode.
- Auditoria registrada.

---

# EPIC 8 — Retenção de dados executável

## LGPD-0801 — Criar tabela `tb_retention_policy`

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### DDL sugerido

```sql
CREATE TABLE tb_retention_policy (
    policy_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    domain VARCHAR(80) NOT NULL,
    document_type VARCHAR(80),
    table_name VARCHAR(80),
    retention_days INTEGER NOT NULL,
    action_after_retention VARCHAR(40) NOT NULL,
    legal_reason TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Critérios de aceite

- Tabela criada.
- Permite política por domínio/tabela/tipo documental.
- Migration passa.

---

## LGPD-0802 — Criar seed inicial de políticas de retenção

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### Políticas iniciais sugeridas

| Domínio | Retenção | Ação |
|---|---:|---|
| PASSWORD_RESET_TOKEN | 1 dia | DELETE |
| BLACKLISTED_TOKEN | conforme expiração | DELETE |
| MESSAGE | 30/180 dias | DELETE ou ANONYMIZE |
| BIOMETRIC_FACE_IMAGE | enquanto ativo | DELETE_ON_REVOCATION |
| BIOMETRIC_CONSENT_EVIDENCE | 5 anos após revogação | RETAIN_THEN_DELETE |
| AUDIT_LOG | 2 anos ou mais | RETAIN |
| SECURITY_INCIDENT | 5 anos | RETAIN |
| TIME_RECORD | conforme obrigação trabalhista | RETAIN |
| AFD/AEJ | conforme obrigação legal | RETAIN |

### Critérios de aceite

- Seeds idempotentes.
- Nenhuma política apaga dado trabalhista crítico automaticamente sem regra clara.

---

## LGPD-0803 — Criar `RetentionPolicyService`

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Resolver política aplicável por domínio/tipo.

### Métodos sugeridos

```java
RetentionPolicy findActivePolicy(String domain, String documentType);
boolean canDelete(String domain, String documentType);
Instant calculateExpiration(Instant baseDate, String domain, String documentType);
```

### Critérios de aceite

- Retorna política ativa.
- Lida com ausência de política.
- Testes cobrem domínios principais.

---

## LGPD-0804 — Criar scheduler de retenção

**Prioridade:** P1  
**Back-end:** sim

### Arquivo provável

```text
src/main/java/com/kts/kronos/application/scheduler/DataRetentionScheduler.java
```

### Execução

```java
@Scheduled(cron = "0 0 2 * * *", zone = "America/Sao_Paulo")
```

### Primeira versão deve cobrir

```text
- tokens expirados
- mensagens antigas
- consentimentos revogados vencidos
- documentos vencidos com permissão de exclusão
```

### Critérios de aceite

- Scheduler tem feature flag.
- Não apaga dados fiscais/trabalhistas na primeira versão.
- Gera logs e audit logs.
- Idempotente.

### Testes obrigatórios

- Não executa quando flag desligada.
- Executa quando flag ligada.
- Não apaga documento em legal hold.
- Apaga documento vencido permitido.

---

# EPIC 9 — Auditoria LGPD ampliada

## LGPD-0901 — Expandir `tb_audit_logs`

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### Migration sugerida

```sql
ALTER TABLE tb_audit_logs
ADD COLUMN company_id UUID NULL,
ADD COLUMN resource_type VARCHAR(80) NULL,
ADD COLUMN resource_id VARCHAR(80) NULL,
ADD COLUMN correlation_id VARCHAR(80) NULL,
ADD COLUMN risk_level VARCHAR(40) NULL;
```

### Critérios de aceite

- Campos criados.
- Campos opcionais para compatibilidade.
- Migration passa.

---

## LGPD-0902 — Criar `AuditService`

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Evitar criação manual e inconsistente de audit logs.

### Arquivos prováveis

```text
src/main/java/com/kts/kronos/application/service/AuditService.java
src/main/java/com/kts/kronos/application/port/in/usecase/AuditUseCase.java
```

### Métodos sugeridos

```java
void register(AuditAction action, UUID employeeId, UUID companyId, String resourceType, String resourceId);
void registerSecurity(AuditAction action, UUID employeeId, String riskLevel, String details);
void registerLgpd(AuditAction action, UUID employeeId, UUID companyId, String details);
```

### Critérios de aceite

- Services deixam de montar `AuditLog.create` diretamente em novos fluxos.
- Sanitização de details aplicada.
- Não loga dados sensíveis.

---

## LGPD-0903 — Auditar download de documentos

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Registrar quem baixou documento, quando e qual documento.

### Arquivo provável

```text
src/main/java/com/kts/kronos/application/service/DocumentService.java
```

### Critérios de aceite

- Download bem-sucedido registra auditoria.
- Falha por acesso negado registra evento de segurança sem vazar dado.
- Auditoria contém `documentId`, `documentType`, `employeeId`.

### Testes obrigatórios

- Download sucesso cria audit log.
- Download cross-tenant não expõe documento.
- Falha não registra storage path.

---

## LGPD-0904 — Auditar exportação LGPD

**Prioridade:** P0  
**Back-end:** sim

### Objetivo

Toda exportação de dados deve gerar auditoria.

### Critérios de aceite

- Exportação registra `LGPD_DATA_EXPORTED`.
- Inclui quem solicitou.
- Inclui titular exportado.
- Inclui companyId.
- Não inclui dados exportados no log.

---

# EPIC 10 — Incidentes de segurança

## LGPD-1001 — Criar tabela `tb_security_incident`

**Prioridade:** P1  
**Back-end:** sim  
**Banco:** sim

### DDL sugerido

```sql
CREATE TABLE tb_security_incident (
    incident_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    severity VARCHAR(40) NOT NULL,
    personal_data_involved BOOLEAN NOT NULL,
    sensitive_data_involved BOOLEAN NOT NULL,
    affected_subjects_estimate INTEGER,
    status VARCHAR(60) NOT NULL,
    notified_anpd_at TIMESTAMPTZ,
    notified_subjects_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);
```

### Critérios de aceite

- Tabela criada.
- Migration passa.

---

## LGPD-1002 — Criar domínio e enums de incidente

**Prioridade:** P1  
**Back-end:** sim

### Enums

```java
public enum SecurityIncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

public enum SecurityIncidentStatus {
    DETECTED,
    CONFIRMED,
    CONTAINED,
    NOTIFIED,
    CLOSED
}
```

### Critérios de aceite

- Domínio criado.
- Enums criados.
- Testes básicos.

---

## LGPD-1003 — Criar CRUD administrativo de incidentes

**Prioridade:** P1  
**Back-end:** sim

### Endpoints

```http
POST   /security-incidents
GET    /security-incidents
GET    /security-incidents/{id}
PATCH  /security-incidents/{id}
```

### Autorização

```text
CTO inicialmente.
```

### Critérios de aceite

- CTO cria incidente.
- CTO lista incidentes.
- Manager/colaborador não acessa inicialmente.
- Audit log registra criação e alteração.

---

# EPIC 11 — Segurança, anti-enumeração e logs

## LGPD-1101 — Revisar endpoints públicos de checagem CPF/CNPJ/username

**Prioridade:** P1  
**Back-end:** sim  
**Front-end:** possivelmente

### Objetivo

Reduzir risco de enumeração.

### Endpoints envolvidos

```http
GET /employee/check-cpf
GET /companies/check-cnpj
GET /users/check-username
```

### Estratégia

1. Aplicar rate limit.
2. Padronizar resposta.
3. Evitar mensagens que confirmem existência de dado sensível em contextos públicos.
4. Avaliar se devem exigir autenticação.

### Critérios de aceite

- Rate limit aplicado.
- Resposta padronizada.
- Teste de abuso básico.

---

## LGPD-1102 — Garantir que `faceImageBase64` nunca seja logado

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** sim

### Objetivo

Evitar vazamento de imagem facial em logs.

### Escopo

1. Revisar DTOs de login facial e check-in.
2. Revisar exception handlers.
3. Revisar logs do front.
4. Adicionar teste de sanitização, se possível.

### Critérios de aceite

- Nenhum log imprime payload completo com `faceImageBase64`.
- Nenhum erro retorna `faceImageBase64`.
- Testes de handler não incluem payload sensível.

---

## LGPD-1103 — Criar utilitário `SensitiveDataMasker`

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Padronizar mascaramento.

### Métodos mínimos

```java
String maskCpf(String cpf)
String maskEmail(String email)
String maskToken(String token)
String maskStoragePath(String path)
String sanitizeDetails(String details)
```

### Critérios de aceite

- CPF nunca retorna completo.
- E-mail mascarado.
- Tokens mascarados.
- Storage path mascarado.
- Testes cobrindo null/blank.

---

# EPIC 12 — Front-end Privacy Center

## LGPD-1201 — Criar rota `/privacy`

**Prioridade:** P1  
**Front-end:** sim

### Objetivo

Criar área de privacidade do usuário.

### Arquivos prováveis

```text
src/config/app-routes.ts
src/App.tsx
src/pages/PrivacyCenter.tsx
```

### Conteúdo inicial

```text
- Status do termo biométrico
- Botão para revogar biometria
- Botão para exportar meus dados
- Link para política de privacidade
- Link para termos de uso
- Lista de solicitações LGPD
```

### Critérios de aceite

- Rota protegida.
- Acessível pelo menu/perfil.
- Usa `AuthContext`.

---

## LGPD-1202 — Criar `lgpd.service.ts`

**Prioridade:** P1  
**Front-end:** sim

### Métodos

```ts
createLgpdRequest(payload)
listLgpdRequests(params)
getLgpdRequest(id)
exportEmployeeData(employeeId)
anonymizeEmployee(employeeId, reason)
```

### Critérios de aceite

- Usa `api.ts`.
- Usa `api-routes.ts`.
- Não usa `fetch`.
- Trata Blob para exportação.

---

## LGPD-1203 — Criar tela de criação de solicitação LGPD

**Prioridade:** P1  
**Front-end:** sim

### Campos

```text
Tipo de solicitação
Descrição
```

### Tipos exibidos

```text
Acesso aos meus dados
Correção de dados
Revogação de consentimento
Anonimização
Exclusão
Informações sobre compartilhamento
```

### Critérios de aceite

- Usuário cria solicitação.
- Loading durante envio.
- Toast de sucesso.
- Validação de campos.
- Erro amigável.

---

## LGPD-1204 — Criar listagem de solicitações LGPD

**Prioridade:** P1  
**Front-end:** sim

### Objetivo

Mostrar solicitações abertas pelo usuário ou gerenciáveis pelo manager.

### Colunas

```text
Data
Tipo
Status
Última atualização
Ações
```

### Critérios de aceite

- Lista paginada.
- Respeita perfil.
- Mostra status traduzido.
- Erro tratado.

---

# EPIC 13 — Testes e qualidade

## LGPD-1301 — Criar suíte de testes de consentimento biométrico

**Prioridade:** P0  
**Back-end:** sim

### Testes mínimos

```text
acceptCreatesConsent
acceptCreatesEvidenceDocument
acceptStoresVersion
acceptStoresHash
acceptStoresIpAndUserAgent
acceptDoesNotDuplicateActiveConsent
statusUsesActiveConsent
revokeMarksConsentAsRevoked
revokeDeletesFaceImage
revokeDeletesRekognitionTemplates
revokeDoesNotDeleteEvidenceDocument
```

---

## LGPD-1302 — Criar suíte de testes de tenant para LGPD

**Prioridade:** P0  
**Back-end:** sim

### Testes mínimos

```text
employeeCannotExportAnotherEmployeeData
managerCannotExportEmployeeFromAnotherCompany
managerCannotListLgpdRequestFromAnotherCompany
managerCannotAnonymizeEmployeeFromAnotherCompany
ctoCanAccessAllCompanies
```

---

## LGPD-1303 — Criar testes front-end para `TermsAcceptanceGate`

**Prioridade:** P0  
**Front-end:** sim

### Testes mínimos

```text
rendersLoading
rendersPendingTermFromBackend
requiresScrollBeforeAccept
requiresCheckboxBeforeAccept
callsAcceptService
showsErrorOnFailure
allowsLogout
```

---

## LGPD-1304 — Criar testes front-end para revogação biométrica

**Prioridade:** P0  
**Front-end:** sim

### Testes mínimos

```text
rendersActiveConsent
opensRevokeDialog
cancelDoesNotCallService
confirmCallsRevokeService
showsSuccessToast
showsErrorToast
refreshesSessionAfterRevoke
```

---

## LGPD-1305 — Criar testes de exportação LGPD

**Prioridade:** P0  
**Back-end:** sim  
**Front-end:** sim

### Back-end

```text
exportOwnDataSuccess
exportDoesNotExposePassword
exportDoesNotExposeToken
exportDoesNotExposeFaceBase64
exportCreatesAuditLog
```

### Front-end

```text
clickExportDownloadsJson
exportFailureShowsToast
```

---

# EPIC 14 — Documentação operacional

## LGPD-1401 — Atualizar documentação de retenção para refletir implementação real

**Prioridade:** P1

### Arquivo provável

```text
docs/legal/data-retention.md
```

### Objetivo

Atualizar documento para diferenciar:

```text
Implementado
Parcialmente implementado
Planejado
Dependente de decisão jurídica
```

### Critérios de aceite

- Documento não afirma que algo está implementado se ainda é apenas plano.
- Inclui tabela por domínio.
- Inclui responsável operacional.

---

## LGPD-1402 — Criar `docs/legal/lgpd-rights.md`

**Prioridade:** P1

### Conteúdo

```text
- direitos do titular
- endpoints relacionados
- prazo interno de atendimento
- papéis: controlador/operador
- fluxo de solicitação
- fluxo de exportação
- fluxo de anonimização
- fluxo de revogação
```

---

## LGPD-1403 — Criar `docs/security/incident-response.md`

**Prioridade:** P1

### Conteúdo

```text
- definição de incidente
- severidades
- responsáveis
- como registrar
- como conter
- como avaliar impacto
- quando notificar
- evidências mínimas
- checklist pós-incidente
```

---

# EPIC 15 — Hardening final para produção

## LGPD-1501 — Validar Swagger/OpenAPI em produção

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Garantir que Swagger/OpenAPI não fique público em produção, exceto se protegido.

### Critérios de aceite

- Em profile `prod`, Swagger desabilitado ou protegido.
- Teste de profile prod cobre configuração.
- Documentação atualizada.

---

## LGPD-1502 — Validar Actuator em produção

**Prioridade:** P1  
**Back-end:** sim

### Objetivo

Evitar exposição de endpoints sensíveis.

### Critérios de aceite

- `/actuator/health` permitido com detalhes mínimos.
- `/actuator/env`, `/heapdump`, `/beans`, `/configprops` não expostos publicamente.
- Teste de configuração prod.

---

## LGPD-1503 — Validar secrets em produção

**Prioridade:** P1  
**Infra/back-end**

### Objetivo

Garantir que segredos não estejam no repositório nem em logs.

### Critérios de aceite

- `.env.example` não contém segredo real.
- Logs não exibem segredo.
- Documentação orienta uso de Secret Manager/variáveis seguras.
- Pipeline não imprime secrets.

---

# 16. Ordem recomendada de execução

## Sprint 1 — Saneamento imediato

```text
LGPD-0001
LGPD-0002
LGPD-0003
LGPD-0302
LGPD-0303
LGPD-0304
LGPD-0305
```

## Sprint 2 — Consentimento versionado

```text
LGPD-0101
LGPD-0102
LGPD-0103
LGPD-0104
LGPD-0105
LGPD-0106
LGPD-0107
LGPD-0108
LGPD-0109
LGPD-0110
```

## Sprint 3 — Texto legal pelo back-end

```text
LGPD-0201
LGPD-0202
LGPD-0203
LGPD-0204
LGPD-0205
LGPD-0206
LGPD-0207
```

## Sprint 4 — Revogação e evidências

```text
LGPD-0301
LGPD-0401
LGPD-0402
LGPD-0403
LGPD-0404
LGPD-0405
```

## Sprint 5 — Solicitações e exportação LGPD

```text
LGPD-0501
LGPD-0502
LGPD-0503
LGPD-0504
LGPD-0505
LGPD-0506
LGPD-0507
LGPD-0508
LGPD-0601
LGPD-0602
LGPD-0603
LGPD-0604
LGPD-0605
```

## Sprint 6 — Anonimização e retenção

```text
LGPD-0701
LGPD-0702
LGPD-0703
LGPD-0704
LGPD-0705
LGPD-0801
LGPD-0802
LGPD-0803
LGPD-0804
```

## Sprint 7 — Auditoria e incidentes

```text
LGPD-0901
LGPD-0902
LGPD-0903
LGPD-0904
LGPD-1001
LGPD-1002
LGPD-1003
```

## Sprint 8 — Front Privacy Center

```text
LGPD-1201
LGPD-1202
LGPD-1203
LGPD-1204
```

## Sprint 9 — Testes e documentação

```text
LGPD-1301
LGPD-1302
LGPD-1303
LGPD-1304
LGPD-1305
LGPD-1401
LGPD-1402
LGPD-1403
```

## Sprint 10 — Hardening produção

```text
LGPD-1101
LGPD-1102
LGPD-1103
LGPD-1501
LGPD-1502
LGPD-1503
```

---

# 17. Definition of Done global

Uma task só deve ser considerada concluída quando:

```text
[ ] Compila localmente
[ ] Testes unitários novos passam
[ ] Testes existentes passam
[ ] Não introduz endpoint sem autorização
[ ] Não quebra contrato do front sem ajustar o front
[ ] Não loga dados sensíveis
[ ] Não expõe CPF em nome de arquivo, log ou path desnecessário
[ ] Respeita tenant
[ ] Respeita papéis CTO/MANAGER/colaborador
[ ] Tem migration idempotente quando alterar banco
[ ] Tem rollback manual documentado quando alterar dados críticos
[ ] Atualiza documentação quando altera comportamento LGPD
```

---

# 18. Critérios para considerar o Kronos tecnicamente aderente

```text
[ ] Consentimento biométrico salvo em tabela versionada
[ ] Termo legal servido pelo back-end
[ ] Aceite registra versão, hash, IP, user-agent e evidência
[ ] Revogação biométrica funciona no front e back
[ ] Revogação remove imagem facial e template biométrico
[ ] Revogação preserva evidência histórica conforme retenção
[ ] Exportação LGPD implementada
[ ] Solicitações LGPD implementadas
[ ] Anonimização implementada
[ ] Bloqueio de tratamento implementado
[ ] Retenção executável implementada
[ ] Documentos possuem checksum
[ ] Auditoria registra ações críticas
[ ] Incidentes de segurança podem ser registrados
[ ] Logs não vazam dados sensíveis
[ ] Cross-tenant testado
[ ] Swagger/Actuator controlados em produção
[ ] Documentação operacional atualizada
```
