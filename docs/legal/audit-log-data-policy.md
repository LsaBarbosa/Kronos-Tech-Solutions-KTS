# Política de Dados em AuditLog — LGPD Compliance

**Data:** 31/05/2026  
**Status:** Vigente  
**Escopo:** Regras de pseudonimização e retenção de dados sensíveis em logs estruturados

---

## 1. Princípio Geral

Existem duas categorias distintas de logs:

1. **Application Logs** (logs de aplicação genéricos):
   - Devem ser pseudonimizados via `SensitiveDataMasker`.
   - Não devem registrar CPF, PIS, e-mail cru, telefone cru, coordenadas geográficas, storage paths, objectKeys, faceIds, tokens ou senhas.
   - Usam `PrivacyLogReferenceService` para converter dados sensíveis em referências anônimas.

2. **AuditLog Estruturado** (logs de auditoria para conformidade):
   - Pode armazenar IDs estruturais internos como evidência interna.
   - Deve ser protegido por controle de acesso restrito.
   - `AuditLog.details` obrigatoriamente passa por `SensitiveDataMasker` antes de armazenamento.
   - IDs como `actorUserId`, `targetEmployeeId`, `companyId`, `resourceId` são permitidos como evidência interna.

---

## 2. Padrão de Mascaramento

### Application Logs

Nunca registrar diretamente:
```text
CPF: 123.456.789-00
PIS: 17049518850
Email: user@company.com
Telefone: (11) 98765-4321
Base64 (imagens biométricas, tokens)
Storage paths: s3://bucket/user-123/face-id-abc.jpg
Object keys: face-id-abc, image-s3-key-123
Face IDs: face_recognition_uuid
External Image IDs: external-image-123
Tokens JWT: eyJhbGc...
Senhas: plaintext ou hashed
```

Usar referências anônimas via `PrivacyLogReferenceService`:
```java
String cpfRef = privacyLogReferenceService.referCPF(cpf);
String emailRef = privacyLogReferenceService.referEmail(email);
String phoneRef = privacyLogReferenceService.referPhone(phone);

log.info("event=user_consent_granted cpf_ref={} email_ref={}", cpfRef, emailRef);
```

### AuditLog.details

O campo `details` no `AuditLog` é um JSON que pode conter dados estruturais. Deve sempre passar por mascaramento:

```java
Map<String, Object> auditDetails = buildAuditDetails(...);
String maskedDetails = sensitiveDataMasker.mask(JsonUtils.toJson(auditDetails));
auditLog.setDetails(maskedDetails);
```

---

## 3. IDs Estruturais como Evidência Interna

Os seguintes IDs são permitidos em `AuditLog` como evidência interna:

| Campo | Tipo | Justificativa |
|-------|------|--------------|
| `actorUserId` | UUID | Identifica quem executou a ação; essencial para auditoria interna. |
| `targetEmployeeId` | UUID | Identifica o alvo da ação; essencial para rastreabilidade. |
| `companyId` | UUID | Contexto organizacional; necessário para conformidade multi-tenant. |
| `resourceId` | UUID | Identifica o recurso afetado; essencial para auditoria de mudanças. |
| `documentId` | UUID | Referência a documento; necessária para rastreabilidade de evidências. |
| `timeRecordId` | UUID | Referência a registro de ponto; essencial para conformidade trabalhista. |

**Restrição de Acesso:**

Esses IDs são preservados internamente, mas o acesso ao `AuditLog` deve ser restrito a:
- Administradores de TI/segurança
- Auditores internos designados
- Órgãos reguladores sob mandado legal

Não deve ser exposto em:
- APIs públicas
- Relatórios de usuário final
- Exportações LGPD padrão (exceto para o titular sob requisição específica)

---

## 4. Diferença entre Application Log e AuditLog

| Aspecto | Application Log | AuditLog |
|--------|-----------------|----------|
| **Pseudonimização** | Obrigatória via `PrivacyLogReferenceService` | `details` passa por `SensitiveDataMasker` |
| **IDs estruturais** | Não usar em log direto; usar referências | Permitido para evidência interna |
| **Retenção** | Conforme política de logs (ex: 30 dias) | Conforme `RetentionPolicyCatalog` (ex: 365 dias) |
| **Acesso** | Disponível para troubleshooting / observabilidade | Restrito a auditores e administradores |
| **Exemplos** | `"User login attempt: cpf_ref=HASH_abc123"` | `"Action: SET_TIME_RECORD by actorUserId=user-uuid with targetEmployeeId=emp-uuid"` |

---

## 5. Exportação LGPD e Minimização

Quando um titular solicita exportação via LGPD ou direito de acesso:

1. **Minimizar application logs**:
   - Incluir apenas logs que o titular pode ter gerado (suas ações).
   - Excluir logs de eventos de backend/administrativos.
   - Pseudonimizar referências de terceiros.

2. **Minimizar AuditLog**:
   - Incluir apenas ações que afetaram o titular diretamente.
   - Mascarar IDs de outros recursos não relacionados.
   - Excluir logs de auditoria interna se não forem diretamente relevantes.

---

## 6. Implementação

### Validar Application Logs

Usar `SensitiveDataMasker` em logs:

```java
@Slf4j
public class UserService {
    private final SensitiveDataMasker masker;
    private final PrivacyLogReferenceService privacyLogReferenceService;

    public void logUserAction(String cpf, String email, String action) {
        String maskedCpf = masker.mask(cpf);
        String emailRef = privacyLogReferenceService.referEmail(email);
        log.info("event=user_action action={} cpf={} email_ref={}", action, maskedCpf, emailRef);
    }
}
```

### Validar AuditLog.details

```java
@Slf4j
public class AuditLogService {
    private final SensitiveDataMasker masker;

    public void recordAction(String actorId, String targetId, String action, Map<String, Object> details) {
        String maskedDetails = masker.mask(JsonUtils.toJson(details));
        AuditLog log = AuditLog.builder()
                .actorUserId(actorId)
                .targetEmployeeId(targetId)
                .action(action)
                .details(maskedDetails)
                .build();
        auditLogRepository.save(log);
    }
}
```

---

## 7. Conformidade e Auditoria

- **Teste automatizado**: Validar que `AuditLog.details` nunca contém dados sensíveis não-mascarados.
- **Teste de cobertura**: Verificar que todos os IDs estruturais obrigatórios estão presentes em ações críticas.
- **Revisão periódica**: Auditar amostras de logs trimestralmente para garantir adesão à política.

---

## 8. Referências

- **LGPD Lei 13.709/2018**, Art. 5º (definição de dados pessoais)
- **Política de Pseudonimização**: `docs/legal/biometric-data-policy.md`
- **Retenção de Dados**: `docs/legal/data-retention.md`
- **Conformidade LGPD**: `docs/legal/lgpd-production-checklist.md`

---

**Aprovado por:** Equipe de Segurança e Conformidade  
**Próxima revisão:** 31/08/2026
