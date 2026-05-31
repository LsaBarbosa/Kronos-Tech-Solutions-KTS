# Checklist de Conformidade LGPD — Pré-Produção

**Data:** 31/05/2026  
**Status:** Vigente  
**Escopo:** Validações obrigatórias antes de deployment em produção

---

## 1. Segurança de Segredo — LGPD_LOG_HASH_SECRET

- [ ] `LGPD_LOG_HASH_SECRET` está configurado no ambiente de produção
- [ ] Valor não é igual a `local-dev-lgpd-log-secret`
- [ ] Valor não está versionado no repositório (use variáveis de ambiente)
- [ ] Rotação de chave foi planejada (anual mínimo)
- [ ] Acesso ao secret é restrito a DevOps/SRE
- [ ] Startup falha em produção se secret estiver ausente/default
- [ ] `LgpdProductionReadinessValidator` valida secret antes de aplicação iniciar

**Teste local:**
```bash
./gradlew clean test -Dspring.profiles.active=prod
# Deve falhar se LGPD_LOG_HASH_SECRET não estiver configurado
```

---

## 2. Biometria — BIOMETRIC_LIVENESS_REQUIRED

- [ ] `BIOMETRIC_LIVENESS_REQUIRED=false` está preservado (decisão oficial de produto)
- [ ] `BasicImageLivenessVerificationProvider` não é permitido em produção
- [ ] Startup valida provider de liveness se `BIOMETRIC_LIVENESS_REQUIRED=true`
- [ ] Front-end respeita `VITE_BIOMETRIC_LIVENESS_REQUIRED=false`
- [ ] Enrollment e login funcionam sem liveness obrigatória

---

## 3. Retenção de Dados — Política Jurídica vs Executável

- [ ] `RetentionPolicyCatalogEntry` diferencia `active` e `schedulerExecutable`
- [ ] `TIME_RECORD`: `active=true`, `schedulerExecutable=false`
- [ ] `EMPLOYEE_CONTRACT`: `active=true`, `schedulerExecutable=false`
- [ ] `PASSWORD_RESET_TOKEN`: `active=true`, `schedulerExecutable=true`
- [ ] `MESSAGE`: `active=true`, `schedulerExecutable=true`
- [ ] Validator APPLY valida somente policies executáveis
- [ ] Executor bloqueia APPLY para processors sem `supportsApply()`
- [ ] Mensagens de erro indicam "legal-preservation" para dados trabalhistas/fiscais

**Teste:**
```bash
# Verificar que TIME_RECORD não é deletado
grep -r "DELETE.*TIME_RECORD" src/main/java || echo "OK: TIME_RECORD not deleted"
```

---

## 4. Logs de Aplicação — Pseudonimização

- [ ] `SensitiveDataMasker` mascara CPF, PIS, e-mail, telefone em logs
- [ ] `PrivacyLogReferenceService` converte dados sensíveis em referências
- [ ] Nenhum log contem:
  - `log.* cpf` (sem referência)
  - `log.* pis` (sem referência)
  - `log.* email` (sem referência)
  - `log.* phone` (sem referência)
  - `log.* password`
  - `log.* token`
  - `log.* base64` (imagens biométricas)
  - Paths de storage (s3://, objectKey, fileKey, faceId, externalImageId, imageS3Key)

**Grep de validação:**
```bash
cd /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS
grep -R "log\..*\(cpf\|pis\|email\|phone\|password\|token\|base64\)" src/main/java || true
grep -R "objectKey={}" src/main/java || true
grep -R "faceId={}" src/main/java || true
```

---

## 5. AuditLog Estruturado — Política de IDs Internos

- [ ] `AuditLog` pode conter IDs estruturais (`actorUserId`, `targetEmployeeId`, `companyId`, `resourceId`)
- [ ] `AuditLog.details` passa por `SensitiveDataMasker` antes de gravação
- [ ] Acesso ao `AuditLog` é restrito a:
  - Administradores de TI
  - Auditores internos designados
  - Órgãos reguladores sob mandado legal
- [ ] Documentação clara que IDs estruturais são evidência interna
- [ ] Exportação LGPD minimiza `AuditLog` conforme perfil de usuário

**Referência:**
- `docs/legal/audit-log-data-policy.md`

---

## 6. Endpoints Públicos — Acesso Controlado

- [ ] `/lgpd/**` requer autenticação
- [ ] `/admin/**` requer autenticação + role ADMIN
- [ ] `/documents/**` requer autenticação + autorização
- [ ] `/employee/**` requer autenticação + autorização
- [ ] `/users/**` requer autenticação + autorização
- [ ] Apenas `GET /public/privacy/**` é público (sem autenticação)
- [ ] `SecurityConfig` valida essas regras

**Teste:**
```bash
curl -i http://localhost:8080/lgpd/audit-logs
# Deve retornar 403 ou 401, não 200
```

---

## 7. Front-end — Segurança de Token

- [ ] Nenhuma ocorrência de `decodeToken()` no src
- [ ] `localStorage.getItem("token")` não é usado para role/employeeId
- [ ] JWT é decodificado apenas no back-end (via validação de signature)
- [ ] Token é transmitido seguro (HTTPS em produção)
- [ ] `.env` real não está versionado
- [ ] `.env.example` não contém secrets reais
- [ ] `.gitignore` bloqueia `.env*` (exceto `.env.example`)

**Teste:**
```bash
cd /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solution-User-Plataform
grep -R "decodeToken" src || echo "OK: No decodeToken"
grep -R "localStorage.getItem.*token" src || echo "OK: No token from localStorage"
```

---

## 8. Front-end — Consentimento e Biometria

- [ ] `CONSENT_REVOCATION` inclui `targetConsentType`
- [ ] `VITE_BIOMETRIC_LIVENESS_REQUIRED` existe e default é `false`
- [ ] Enrollment funciona com `VITE_BIOMETRIC_LIVENESS_REQUIRED=false`
- [ ] Revogação de consentimento atualiza `biometric_consents` table corretamente

---

## 9. Builds e Testes

- [ ] `./gradlew clean test` passa 100%
- [ ] `./gradlew clean bootJar -x test` passa sem erros
- [ ] `npm run lint` passa 100% (front-end)
- [ ] `npm run test -- --run` passa 100% (front-end)
- [ ] `npm run build` constrói sem warnings críticos (front-end)
- [ ] `git diff --check` não reporta trailing whitespace ou issues

---

## 10. Greps Finais de Segurança

### Back-end

```bash
# Executar no diretório do back-end
grep -R "employeeId={}" src/main/java || true
grep -R "userId={}" src/main/java || true
grep -R "companyId={}" src/main/java || true
grep -R "storagePath={}" src/main/java || true
grep -R "storage_path=" src/main/java || true
grep -R "objectKey={}" src/main/java || true
grep -R "fileKey={}" src/main/java || true
grep -R "faceId={}" src/main/java || true
grep -R "externalImageId={}" src/main/java || true
grep -R "imageS3Key={}" src/main/java || true
grep -R "log\..*email" src/main/java || true
grep -R "log\..*cpf" src/main/java || true
grep -R "log\..*pis" src/main/java || true
grep -R "log\..*phone" src/main/java || true
grep -R "log\..*base64" src/main/java || true
grep -R "log\..*password" src/main/java || true
grep -R "log\..*token" src/main/java || true
grep -R "local-dev-lgpd-log-secret" src/main/java src/main/resources || true
```

### Front-end

```bash
# Executar no diretório do front-end
grep -R "decodeToken" src || true
grep -R "localStorage.getItem(\"token\")" src || true
grep -R "localStorage.getItem('token')" src || true
grep -R "targetConsentType" src || true
grep -R "CONSENT_REVOCATION" src || true
grep -R "livenessPassed" src || true
grep -R "VITE_BIOMETRIC_LIVENESS_REQUIRED" src .env* || true
grep -R "public/privacy/processing-catalog" src || true
grep -R "openedAt\|closedAt" src || true
```

---

## 11. Variáveis de Ambiente Obrigatórias

| Variável | Produção | Dev/Test | Descrito em |
|----------|----------|----------|------------|
| `LGPD_LOG_HASH_SECRET` | Obrigatória | Fallback: `local-dev-lgpd-log-secret` | BLOCO A |
| `BIOMETRIC_LIVENESS_REQUIRED` | `false` | `false` | BLOCO B |
| `VITE_BIOMETRIC_LIVENESS_REQUIRED` | `false` | `false` | BLOCO C |
| `LGPD_RETENTION_SCHEDULER_ENABLED` | Configurável | Configurável | BLOCO B |
| `LGPD_RETENTION_SCHEDULER_MODE` | `DRY_RUN` ou `APPLY` | `DRY_RUN` | BLOCO B |

---

## 12. Rotação de Chaves e Maintenance

- [ ] `LGPD_LOG_HASH_SECRET` foi rotacionado (anual)
- [ ] Histórico de rotação está documentado
- [ ] Procedimento de rotação foi testado em staging
- [ ] Alerta está configurado para expiração de chave

---

## 13. Conformidade Jurídica

- [ ] Parecer jurídico foi obtido (compliance check)
- [ ] Documentação foi revisada por legal
- [ ] Processamento de dados foi documentado (ROPA)
- [ ] Política de privacidade foi atualizada
- [ ] Consentimento foi obtido para biometria (se aplicável)
- [ ] DPO (ou responsável de compliance) aprovou deploy

---

## 14. Cronograma

| Marco | Data | Status |
|-------|------|--------|
| Implementação de BLOCO A | 31/05/2026 | ✓ Completo |
| Implementação de BLOCO B | 31/05/2026 | ✓ Completo |
| Implementação de BLOCO C | 31/05/2026 | ✓ Completo |
| Testes e validação | 31/05/2026 | Em andamento |
| Deploy em staging | TBD | Planejado |
| Aprovação jurídica | TBD | Pendente |
| Deploy em produção | TBD | Planejado |

---

**Assinado por:** Engenharia de Segurança  
**Data de aprovação:** 31/05/2026  
**Próxima revisão:** 31/08/2026

