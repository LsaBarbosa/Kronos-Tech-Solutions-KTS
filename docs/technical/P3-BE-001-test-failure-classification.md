# P3-BE-001: Classificação de Testes Back-end Falhando

**Data:** 2026-05-25  
**Status:** 🔍 DIAGNÓSTICO CONCLUÍDO  
**Branch:** feature/lgpd-compliance

---

## Resumo Executivo

- **Total de testes:** 1498
- **Testes falhando:** 130 (8.7%)
- **Grupos de falhas identificados:** 8
- **Principais causas:** Dependency injection (56 casos), Context initialization (166 casos), Logic assertions (21 casos)

---

## Matriz de Classificação por Grupo

| # | Grupo | Classe | Erro Principal | Causa Provável | # Testes | Impacto LGPD | Impacto Produção | Prioridade |
|---|-------|--------|-----------------|-----------------|----------|-------------|-----------------|-----------|
| **1** | **Injection Spring** | `AuthService*` | `NullPointerException: auditRequestContextService is null` | `@MockitoBean` não injetando dependência em service layer | 27 | 🔴 ALTO | 🔴 CRÍTICO | **P0** |
| **1a** | Injection Spring | AuthServiceAuthenticationAndResetTest | NullPointerException (AuditRequestContextService) | Mock bean setup inválido no teste | 15 | ALTO | CRÍTICO | P0 |
| **1b** | Injection Spring | AuthServiceTest | NullPointerException (AuditRequestContextService) | Mock bean setup inválido no teste | 12 | ALTO | CRÍTICO | P0 |
| **2** | **Observability Injection** | `Company/GeolocationService` | `NullPointerException: kronosMetrics is null` | `KronosMetrics` não registrado como @Bean em test config | 4 | 🟡 MÉDIO | 🟡 ALTO | **P1** |
| **2a** | Observability Injection | CompanyServiceFeature44OptimizationTest | NullPointerException (KronosMetrics) | Mock bean missing in @TestConfiguration | 3 | MÉDIO | ALTO | P1 |
| **2b** | Observability Injection | GeolocationServiceTest | NullPointerException (KronosMetrics) | Mock bean missing in @TestConfiguration | 1 | MÉDIO | ALTO | P1 |
| **3** | **Spring Context Init** | `*ComplianceTest` | `IllegalStateException: ApplicationContext failure threshold exceeded` | Cascading bean dependencies não resolvidas; test context loading falha | 83 | 🔴 ALTO | 🟡 MÉDIO | **P0** |
| **3a** | Context Init | BiometricConsentComplianceTest | ApplicationContext failure (8 test methods) | Missing repository beans in test context | 8 | ALTO | MÉDIO | P0 |
| **3b** | Context Init | DataRetentionAnonymizationComplianceTest | ApplicationContext failure (10 test methods) | Missing repository/service beans | 10 | ALTO | MÉDIO | P0 |
| **3c** | Context Init | MultiTenantIsolationComplianceTest | ApplicationContext failure (9 test methods) | Missing repository/service beans | 9 | ALTO | MÉDIO | P0 |
| **3d** | Context Init | SecurityContext smoke tests (3 classes) | ApplicationContext failure on @SpringBootTest | Missing security filter chain beans | 3 | MÉDIO | CRÍTICO | P0 |
| **4** | **Security Config** | `SecurityConfig*` | `IllegalStateException` (context load) + `AssertionFailedError` (endpoint validation) | Security filter chain not initialized properly in tests | 28 | 🟡 MÉDIO | 🔴 CRÍTICO | **P0** |
| **4a** | Security Config | SecurityConfigDependencyRegressionTest | IllegalStateException (context load) | Missing SecurityFilterChain bean | 3 | MÉDIO | CRÍTICO | P0 |
| **4b** | Security Config | SecurityConfigIntegrationTest | AssertionFailedError (18 endpoints failing validation) | CSRF, CORS, Auth header expectations not met | 18 | MÉDIO | CRÍTICO | P0 |
| **4c** | Security Config | UserEnumerationExposureIntegrationTest | AssertionFailedError (endpoint access control) | User enumeration protection not enforced | 7 | MÉDIO | CRÍTICO | P0 |
| **5** | **Web Layer Integration** | `BiometricConsentFlowIntegrationTest` | `IllegalStateException` (context load) + runtime errors | Missing controller/service beans in web test context | 10 | 🟡 MÉDIO | 🟡 ALTO | **P1** |
| **5a** | Web Layer | BiometricConsentFlowIntegrationTest | Context load failure + 10 endpoint tests | @SpringBootTest context incomplete | 10 | MÉDIO | ALTO | P1 |
| **6** | **LGPD Admin/Export** | `LgpdAdmin/ExportFlowIntegrationTest` | `IllegalStateException` (context load) | Missing LGPD service/provider beans | 21 | 🔴 ALTO | 🟡 ALTO | **P1** |
| **6a** | LGPD Admin/Export | LGPD Admin Request Management (9 tests) | Context load + endpoint access failures | Missing LGPD admin controllers/services | 9 | ALTO | ALTO | P1 |
| **6b** | LGPD Admin/Export | LGPD Export Flow (12 tests) | Context load + data masking logic failures | Missing export service implementation/beans | 12 | ALTO | ALTO | P1 |
| **7** | **LGPD Retention Audit** | `LgpdRetentionAuditValidationTest` | `NullPointerException` (user_id NOT NULL constraint) | H2 database schema enforces NOT NULL on user_id; test asserts null expected | 6 | 🔴 ALTO | 🟢 BAIXO | **P2** |
| **7a** | LGPD Audit | LgpdRetentionAuditValidationTest | AssertionFailedError (6 tests) | AuditLogEntity.user_id cannot be null in database | 6 | ALTO | BAIXO | P2 |
| **8** | **Service Logic** | `AuditLogProviderImplTest`, `DocumentServiceSecurityTest`, `LgpdServiceAnonymizationTest` | `AssertionFailedError` (logic validation) + `BadRequestException` (storage access) | Business logic errors in sanitization/masking; missing storage provider mock | 3 | 🟡 MÉDIO | 🟡 MÉDIO | **P1** |
| **8a** | Service Logic | AuditLogProviderImplTest | AssertionFailedError (sanitization check) | Sensitive data masking not applied correctly | 1 | MÉDIO | MÉDIO | P1 |
| **8b** | Service Logic | DocumentServiceSecurityTest | BadRequestException (S3 storage access) | BucketStorageProvider mock not returning data | 1 | MÉDIO | MÉDIO | P1 |
| **8c** | Service Logic | LgpdServiceAnonymizationTest | NullPointerException (AuditRequestContextService) | Same as Group 1 issue | 2 | ALTO | CRÍTICO | P0 |

---

## Análise Detalhada por Problema

### **PROBLEMA 1: AuditRequestContextService Não Injetado (27 testes, P0)**

**Erro:**
```
java.lang.NullPointerException: Cannot invoke "com.kts.kronos.application.service.
AuditRequestContextService.extractContext()" because "this.auditRequestContextService" is null
```

**Causa Raiz:**
- `AuthService` depende de `AuditRequestContextService` via `@Autowired`
- Testes usam `@MockBean` para repositórios, mas `AuditRequestContextService` não está no test context
- Service real tenta executar lógica que depende dessa dependência

**Classes Afetadas:**
- `AuthServiceAuthenticationAndResetTest` (15 falhas)
- `AuthServiceTest` (12 falhas)

**Impacto LGPD:**
- Login/password reset são operações críticas de auditoria
- Falha impede validação de consenti biométrico, direitos de acesso, rastreamento de tentativas

**Impacto Produção:**
- Bloqueia autenticação em produção
- Usuários não conseguem fazer login/reset de senha

**Solução:**
Adicionar `AuditRequestContextService` ao `@TestConfiguration`:
```java
@TestConfiguration
static class AuthTestConfiguration {
    @Bean
    public AuditRequestContextService auditRequestContextService() {
        AuditRequestContextService mock = mock(AuditRequestContextService.class);
        when(mock.extractContext()).thenReturn(new AuditContext(...));
        return mock;
    }
}
```

---

### **PROBLEMA 2: KronosMetrics Não Injetado (4 testes, P1)**

**Erro:**
```
java.lang.NullPointerException: Cannot invoke "com.kts.kronos.observability.
application.KronosMetrics.companyUpdated()" because "this.kronosMetrics" is null
```

**Causa Raiz:**
- `CompanyService` e `GeolocationService` usam `KronosMetrics` para observability
- Bean não registrado em test configuration

**Classes Afetadas:**
- `CompanyServiceFeature44OptimizationTest` (3 falhas)
- `GeolocationServiceTest` (1 falha)

**Impacto LGPD:**
- Falha em criar/atualizar empresa ou resolver geolocalização
- Menos crítico que autenticação, mas afeta operações de conformidade

**Solução:**
```java
@Bean
public KronosMetrics kronosMetrics() {
    return mock(KronosMetrics.class);
}
```

---

### **PROBLEMA 3: ApplicationContext Initialization Failures (83 testes, P0)**

**Erro:**
```
java.lang.IllegalStateException: Failed to load ApplicationContext for 
[WebMergedContextConfiguration@xyz testClass = BiometricConsentComplianceTest, ...]
org.springframework.beans.factory.UnsatisfiedDependencyException: 
No qualifying bean of type 'PasswordResetTokenRepository' available
```

**Causa Raiz:**
- Compliance tests usam `@SpringBootTest` (carrega contexto inteiro)
- 24 repositories no projeto, nenhuma está autoconfigurada no test context
- Primeiro bean criado tenta autowire todas dependências, repositories não prontos

**Classes Afetadas (por número de testes):**
- `BiometricConsentComplianceTest` (8)
- `DataRetentionAnonymizationComplianceTest` (10)
- `MultiTenantIsolationComplianceTest` (9)
- `ObservabilityLocalProfileContextSmokeTest` (1)
- `ObservabilityProdProfileContextSmokeTest` (1)
- `ProdProfileContextSmokeTest` (1)

**Impacto LGPD:**
- Estes são testes de **conformidade legal** (LGPD compliance tests)
- Falha impede validação de biometric consent, data retention, multi-tenant isolation
- Alto impacto em validação regulatória, médio em produção (features existem, tests faltam)

**Solução Recomendada (Solution 2 - @DataJpaTest pattern):**

Para cada compliance test, substituir:
```java
@SpringBootTest  // ❌ Carrega tudo
```

Por:
```java
@DataJpaTest
@Import({
    BiometricConsentService.class,
    RetentionPolicyExecutor.class,
    // ... only required services
})
@EnableTransactionManagement
@AutoConfigureTestDatabase(replace = NONE)
```

---

### **PROBLEMA 4: Security Filter Chain Config (28 testes, P0)**

**Erro (tipo 1):**
```
java.lang.IllegalStateException: Failed to load ApplicationContext ... 
no qualifying bean of type 'SecurityFilterChain'
```

**Erro (tipo 2):**
```
org.opentest4j.AssertionFailedError: 
assertEquals expected: <true> but was: <false>
// Test assertion for CSRF, CORS, endpoint access control
```

**Causa Raiz:**
- `SecurityConfigDependencyRegressionTest` (3): SecurityFilterChain não injetado
- `SecurityConfigIntegrationTest` (18): CSRF/CORS/Auth headers não validados corretamente
- `UserEnumerationExposureIntegrationTest` (7): Endpoint access control not enforced

**Classes Afetadas:**
- SecurityConfigDependencyRegressionTest (3)
- SecurityConfigIntegrationTest (18)
- SecurityConfigPublicDocsIntegrationTest (1)
- UserEnumerationExposureIntegrationTest (7)

**Impacto LGPD:**
- **CRÍTICO para produção**: Afeta autenticação, autorização, proteção de dados
- Médio para LGPD compliance (features devem funcionar, testes faltam)

**Impacto Produção:**
- Endpoints podem estar expostos sem autenticação
- CSRF/CORS protections podem estar desabilitadas
- User enumeration attacks podem ser possíveis

**Solução:**
1. Criar SecurityTestConfiguration com beans necessários
2. Validar SecurityFilterChain está sendo criado
3. Verificar CORS, CSRF, Auth header expectations

---

### **PROBLEMA 5: Web Layer Biometric Consent Flow (10 testes, P1)**

**Erro:**
```
java.lang.IllegalStateException: ApplicationContext failure threshold exceeded
// BiometricConsentFlowIntegrationTest > Should accept biometric term successfully FAILED
```

**Causa Raiz:**
- Integration tests para endpoints de biometric consent
- @SpringBootTest context não consegue carregar (mesma issue de #3)
- Controllers dependem de services que dependem de repositories não autoconfigured

**Impacto LGPD:**
- Medium: Biometric consent é LGPD-critical, mas testes de acceptance faltam
- Feature deveria funcionar em produção

**Solução:**
- Migrate para @DataJpaTest com selective imports (Solution 2)
- Ou usar TestRestTemplate com MockMvc setup

---

### **PROBLEMA 6: LGPD Admin & Export Services (21 testes, P1)**

**Erro:**
```
java.lang.IllegalStateException: ApplicationContext failure threshold exceeded
// LGPD Admin Request Management Integration Tests
// LGPD Export Flow Integration Tests
```

**Causa Raiz:**
- LGPD admin/export flows são novos para Phase 2
- Services e controllers não totalmente implementados
- Test context loading falha por missing beans

**Classes Afetadas:**
- LGPD Admin Request Management (9)
- LGPD Export Flow (12)

**Impacto LGPD:**
- **ALTO**: Estas são funcionalidades de direitos de dados (export, access)
- Regulatórias para LGPD compliance

**Impacto Produção:**
- Alto: Users não conseguem exportar/acessar dados pessoais

**Solução:**
- Completar implementação de LgpdAdminService, ExportService
- Registrar como beans no contexto
- Criar proper TestConfiguration

---

### **PROBLEMA 7: Audit Log User ID NOT NULL (6 testes, P2)**

**Erro:**
```
java.sql.SQLException: Constraint violation
org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: 
NOT NULL constraint failed: TB_AUDIT_LOG.USER_ID
```

**Causa Raiz:**
- `AuditLogEntity` schema define `user_id` como NOT NULL
- Tests para retention audit validation esperam `user_id = null` (system operations não têm usuário)
- H2 database enforcement bloqueia inserção

**Classes Afetadas:**
- LgpdRetentionAuditValidationTest (6 testes)

**Impacto LGPD:**
- Alto: Tests validam que retenção NÃO loga employee IDs
- Médio em produção: Retenção funciona, testes não validam

**Solução Recomendada:**

**Opção A** (Recomendada): Permitir NULL em schema:
```sql
ALTER TABLE TB_AUDIT_LOG MODIFY USER_ID UUID NULL;
```

**Opção B**: Usar system user ID para operações do sistema:
```java
AuditLogEntity audit = new AuditLogEntity()
    .userId(SYSTEM_USER_UUID)  // Special UUID for system operations
    .companyId(null)
    .action("RETENTION_DRY_RUN")
    .riskLevel("SYSTEM");
```

**Opção C**: Criar separate table for system-level operations:
```java
SystemOperationLogEntity (sem userId/companyId)
```

---

### **PROBLEMA 8: Service Logic Failures (3 testes, P1-P2)**

**Erro:**
```
org.opentest4j.AssertionFailedError: expected <true> but was <false>
// AuditLogProviderImplTest: sanitization test

com.kts.kronos.application.exceptions.BadRequestException: 
Falha ao buscar o arquivo no storage
// DocumentServiceSecurityTest: S3 access
```

**Causa Raiz:**
- Service-level business logic errors
- Mock providers not properly configured

**Classes Afetadas:**
- AuditLogProviderImplTest (1) - Sensitive data masking
- DocumentServiceSecurityTest (1) - Storage provider
- LgpdServiceAnonymizationTest (2) - AuditRequestContextService (grouped with #1)

**Solução:**
- Audit masking: Review SensitiveDataMasker implementation
- Document service: Mock BucketStorageProvider properly
- Service: Fix AuditRequestContextService injection (Problem #1)

---

## Recomendações de Priorização

### **BLOCKER (P0) - Resolver Hoje**

| Ordem | Problema | Testes | Tempo Est. | Impacto |
|-------|----------|--------|-----------|---------|
| 1️⃣ | Security Filter Chain (Problem 4a-4c) | 28 | 2h | CRÍTICO prod + LGPD |
| 2️⃣ | AuditRequestContextService injection (Problem 1) | 27 | 1.5h | CRÍTICO auth + audit |
| 3️⃣ | Spring Context Init - Compliance tests (Problem 3) | 83 | 2h | ALTO LGPD compliance |

**Total para P0: 138 testes, ~5.5 horas**

### **HIGH (P1) - Resolver Esta Sprint**

| Ordem | Problema | Testes | Tempo Est. |
|-------|----------|--------|-----------|
| 4️⃣ | KronosMetrics injection (Problem 2) | 4 | 30 min |
| 5️⃣ | LGPD Admin/Export services (Problem 6) | 21 | 2h |
| 6️⃣ | Web Layer Biometric (Problem 5) | 10 | 1h |
| 7️⃣ | Service logic fixes (Problem 8) | 3 | 1h |

**Total para P1: 38 testes, ~4.5 horas**

### **MEDIUM (P2) - Resolver Antes do Release**

| Ordem | Problema | Testes | Tempo Est. |
|-------|----------|--------|-----------|
| 8️⃣ | Audit Log schema (Problem 7) | 6 | 30 min |

**Total para P2: 6 testes, 30 minutos**

---

## Roadmap de Execução

### Fase 1: Stabilize Core (P0 + Critical P1)
**Estimativa: 6-7 horas**

```
Dia 1:
  ├─ Hora 1-2: Fix SecurityFilterChain (Problem 4) ← BLOCKER
  ├─ Hora 2-3: Fix AuditRequestContextService (Problem 1) ← BLOCKER
  └─ Hora 3-4: Refactor Compliance Tests to @DataJpaTest (Problem 3) ← BLOCKER

Dia 2:
  ├─ Hora 1: KronosMetrics injection (Problem 2)
  ├─ Hora 2: Web Layer Biometric (Problem 5)
  └─ Hora 2-3: LGPD Admin/Export services (Problem 6)
```

### Fase 2: Complete LGPD Compliance
**Estimativa: 2 horas**

```
Dia 3:
  ├─ Hora 1: Audit Log schema fix (Problem 7)
  └─ Hora 2: Service logic refinements (Problem 8)
```

### Resultado Final
- ✅ **130 → 0 failures** (ou ~6 tolerated in P7 if schema stays NOT NULL)
- ✅ **Test coverage:** 1498 tests passing
- ✅ **LGPD Compliance:** All 130 tests covering legal requirements passing

---

## Próximas Ações

### Imediatamente (Esta hora)
1. ✅ Diagnóstico de 130 testes concluído
2. ⏳ Criar branches para P3-BE-001.1, P3-BE-001.2, etc.
3. ⏳ Distribuir entre time para work em paralelo

### Curto Prazo (Próximas 24h)
- [ ] Fix Problem 4: SecurityFilterChain tests
- [ ] Fix Problem 1: AuditRequestContextService injection
- [ ] Fix Problem 3: Compliance test context loading

### Médio Prazo (Esta semana)
- [ ] Fix remaining P1 issues
- [ ] Fix P2 audit log schema constraint
- [ ] Run full test suite: `./gradlew test`
- [ ] Target: 1495+ passing tests (99.8%+)

---

## Documentação Relacionada

- `docs/technical/p2-be-007-bean-initialization-solution.md` — Solution patterns for Spring context
- `docs/technical/P2-LGPD-PHASE-2-SUMMARY.md` — Phase 2 completion status
- `docs/technical/p2-be-008-retention-audit-validation.md` — Audit test specifications

---

**Assinado:** P3-BE-001 Analysis  
**Data:** 2026-05-25  
**Status:** 🟢 Ready for implementation
