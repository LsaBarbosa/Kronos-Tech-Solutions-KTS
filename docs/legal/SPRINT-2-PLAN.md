# Sprint 2 — Retenção Real de Dados
## Implementation Plan

**Status:** PLANNING  
**Branch:** `feature/lgpd-compliance`  
**Target Completion:** End of Sprint 2  

---

## Sprint Objective

Replace the current noop retention implementation with real data deletion and anonymization, supporting dry-run validation, comprehensive audit trail, and domain-specific policies.

---

## Features in Scope

| Feature | Priority | Type | Effort |
|---------|----------|------|--------|
| LGPD-201 — Architecture | P0 | Back-end + DB | High |
| LGPD-202 — Token Retention | P0 | Back-end | Medium |
| LGPD-203 — Message Retention | P0 | Back-end + DB | Medium |
| LGPD-204 — Document Retention | P0 | Back-end + DB | Medium |
| LGPD-205 — Audit Log Retention | P0 | Back-end + DB | Low |

---

## Impact Matrix

### Database Changes Required

| Table | Migration | Changes | Impact |
|-------|-----------|---------|--------|
| NEW: `tb_retention_execution_log` | V9 | Create table | High — new audit log |
| `tb_message` | V10 | Add: `deleted_at`, `deleted_by_system`, `retention_policy_code` | Medium — soft delete support |
| `tb_document` | V11 | Add: `deleted_by_retention`, `retention_deleted_at`, `retention_policy_code` | Medium — retention tracking |
| `tb_blacklisted_token` | None | Query methods | Low — no schema change |
| `tb_password_reset_token` | None | Query methods | Low — no schema change |
| `tb_audit_log` | V12 | Add: `anonymized_ip`, `anonymized_user_agent`, `sanitized_details` | Medium — anonymization |

### Back-end Files to Create

```
Core Architecture:
├─ src/main/java/com/kts/kronos/domain/model/enuns/RetentionResourceType.java
├─ src/main/java/com/kts/kronos/domain/model/enuns/RetentionAction.java
├─ src/main/java/com/kts/kronos/domain/model/enuns/RetentionExecutionMode.java
├─ src/main/java/com/kts/kronos/domain/model/RetentionExecutionResult.java
├─ src/main/java/com/kts/kronos/domain/model/RetentionExecutionLog.java
├─ src/main/java/com/kts/kronos/domain/model/RetentionPolicy.java (if not exists)

Service Layer:
├─ src/main/java/com/kts/kronos/application/service/retention/RetentionDomainProcessor.java
├─ src/main/java/com/kts/kronos/application/service/retention/RetentionPolicyExecutor.java
├─ src/main/java/com/kts/kronos/application/service/retention/TokenRetentionProcessor.java
├─ src/main/java/com/kts/kronos/application/service/retention/MessageRetentionProcessor.java
├─ src/main/java/com/kts/kronos/application/service/retention/DocumentRetentionProcessor.java
├─ src/main/java/com/kts/kronos/application/service/retention/AuditLogRetentionProcessor.java

Persistence Layer:
├─ src/main/java/com/kts/kronos/application/port/out/provider/RetentionExecutionLogProvider.java
├─ src/main/java/com/kts/kronos/adapter/out/persistence/RetentionExecutionLogRepository.java

Utilities:
├─ src/main/java/com/kts/kronos/application/service/SensitiveDataMasker.java (enhanced)
```

### Back-end Files to Modify

| File | Changes | Impact |
|------|---------|--------|
| `RetentionPolicyService.java` | Replace `apply_noop()` with `executeEnabledPolicies()` | High — core change |
| `RetentionPolicy.java` | Add validation for required fields | Medium — model enhancement |
| `TokenRepository.java` | Add `countExpiredBefore()`, `deleteExpiredBefore()` | Low — new query methods |
| `MessageRepository.java` | Add `countExpiredAndRemovable()`, `softDeleteExpired()` | Low — new query methods |
| `DocumentRepository.java` | Add `countRemovableByType()`, `markAsDeletedByRetention()` | Low — new query methods |
| `AuditLogRepository.java` | Add `countOldLogs()`, `anonymizeLogs()` | Low — new query methods |

### Database Migration Files

```
src/main/resources/db/migration/
├─ V9__create_retention_execution_log.sql
├─ V10__add_retention_fields_to_message.sql
├─ V11__add_retention_fields_to_document.sql
├─ V12__add_anonymization_fields_to_audit_log.sql
```

### Test Files to Create

```
Unit Tests:
├─ src/test/java/com/kts/kronos/application/service/retention/TokenRetentionProcessorTest.java
├─ src/test/java/com/kts/kronos/application/service/retention/MessageRetentionProcessorTest.java
├─ src/test/java/com/kts/kronos/application/service/retention/DocumentRetentionProcessorTest.java
├─ src/test/java/com/kts/kronos/application/service/retention/AuditLogRetentionProcessorTest.java

Integration Tests:
├─ src/test/java/com/kts/kronos/application/service/RetentionPolicyServiceIntegrationTest.java

Database Tests:
├─ src/test/java/com/kts/kronos/adapter/out/persistence/RetentionExecutionLogRepositoryTest.java
```

---

## Implementation Sequence

### Phase 1: Architecture Foundation (LGPD-201)
**Estimated:** 2-3 days

1. Create enums: `RetentionResourceType`, `RetentionAction`, `RetentionExecutionMode`
2. Create `RetentionExecutionResult` model
3. Create `RetentionExecutionLog` entity and migration (V9)
4. Create `RetentionDomainProcessor` interface
5. Create `RetentionPolicyExecutor` orchestrator
6. Update `RetentionPolicyService.executeEnabledPolicies()`
7. Add validation rules (required fields for APPLY mode)
8. Create comprehensive unit tests

**Key Decisions Needed:**
- How to handle processor registration (registry pattern vs factory)?
- Transaction boundaries (one per processor or global)?
- Retry strategy for failed processors

### Phase 2: Token Retention (LGPD-202)
**Estimated:** 1 day

1. Add query methods to `TokenRepository` and `PasswordResetTokenRepository`
2. Create `TokenRetentionProcessor`
3. Implement `BLACKLISTED_TOKEN` domain logic
4. Implement `PASSWORD_RESET_TOKEN` domain logic
5. Create comprehensive tests (expired, valid, mixed scenarios)
6. Verify no valid tokens deleted

### Phase 3: Message Retention (LGPD-203)
**Estimated:** 1.5 days

1. Create migration V10 (add `deletedAt`, `deletedBySystem`, `retentionPolicyCode`)
2. Add query methods to `MessageRepository`
3. Create `MessageRetentionProcessor`
4. Implement preservation rules (legal processes, incidents)
5. Create soft delete logic
6. Create tests covering all preservation scenarios
7. Verify front-end doesn't display deleted messages

### Phase 4: Document Retention (LGPD-204)
**Estimated:** 2 days

1. Create migration V11 (add `deletedByRetention`, `retentionDeletedAt`, `retentionPolicyCode`)
2. Add query methods to `DocumentRepository`
3. Create `DocumentRetentionProcessor`
4. Implement S3 deletion logic
5. Implement soft delete in DB
6. Handle document type-specific rules (fiscal, labor, biometric)
7. Create tests covering type-specific scenarios
8. Verify metadata preservation after deletion

### Phase 5: Audit Log Retention (LGPD-205)
**Estimated:** 1 day

1. Enhance `SensitiveDataMasker` with anonymization methods
2. Create migration V12 (add anonymization fields)
3. Add query methods to `AuditLogRepository`
4. Create `AuditLogRetentionProcessor`
5. Implement IP anonymization
6. Implement userAgent sanitization
7. Implement details sanitization
8. Create tests covering PII removal

---

## Key Architecture Decisions

### Decision 1: Processor Registration Pattern

**Option A (Registry Pattern):**
```java
@Component
public class RetentionProcessorRegistry {
    private Map<RetentionResourceType, RetentionDomainProcessor> processors;
    
    public RetentionDomainProcessor get(RetentionResourceType type) {
        return processors.get(type);
    }
}
```

**Option B (Factory):**
```java
@Component
public class RetentionProcessorFactory {
    public RetentionDomainProcessor create(RetentionResourceType type) {
        return switch(type) { ... }
    }
}
```

**Decision:** Registry Pattern — allows easy addition of new processors, better for testing.

### Decision 2: Transaction Handling

**Option A (Single Global Transaction):**
- One transaction wraps all processors
- Rollback if any processor fails
- Risk: All-or-nothing semantics might not match policy intent

**Option B (Per-Processor Transactions):**
- Each processor runs in its own transaction
- Failure in one doesn't affect others
- More resilient, better audit trail

**Decision:** Per-Processor Transactions — allows partial success, better matches real-world requirements.

### Decision 3: Dry-Run Implementation

**Option A (Read-Only Mode):**
- Dry-run just counts without touching data
- Faster, safer
- Doesn't validate actual deletion capability

**Option B (Simulated Execution):**
- Dry-run actually attempts deletion but rolls back
- Validates full capability
- Slower, more resource-intensive

**Decision:** Read-Only Mode — speed and safety more important than capability validation.

---

## Data Safety Mechanisms

### Protection 1: Validation Before APPLY
```
APPLY mode requires:
├─ resourceType set
├─ retentionDays set
├─ action set (DELETE, ANONYMIZE, PRESERVE)
├─ preserveLaborData defined
├─ preserveFiscalData defined
└─ policy enabled
```

### Protection 2: Audit Trail
Every execution logged with:
- Execution ID (UUID)
- Policy code
- Resource type
- Mode (DRY_RUN or APPLY)
- Counts: scanned, affected, skipped, error
- Timestamps and status

### Protection 3: Isolation
Each processor:
- Runs independently
- Doesn't affect other domains
- Failure doesn't cascade
- Returns detailed result

### Protection 4: Compensation
For S3 deletions:
- Log S3 key before deletion
- If DB operation fails, S3 still deleted (acceptable)
- If S3 fails, DB not updated (safe state)

---

## Test Strategy

### Unit Tests (25+ tests)
```
TokenRetentionProcessor — 8 tests
├─ expired token deleted in APPLY mode
├─ valid token preserved in APPLY mode
├─ DRY_RUN doesn't modify database
├─ count includes only expired
├─ APPLY mode counted correctly
├─ error handling for DB issues
├─ mixed scenarios (some expire, some valid)
└─ edge cases (exactly at cutoff)

MessageRetentionProcessor — 8 tests
├─ old message soft deleted
├─ recent message preserved
├─ legal process messages preserved
├─ DRY_RUN count accuracy
├─ soft delete maintains integrity
├─ front-end query respects deletedAt
├─ mixed scenarios
└─ edge cases

Similar for Document and AuditLog
```

### Integration Tests (5+ tests)
```
RetentionPolicyService — 5 tests
├─ executeEnabledPolicies with all processors
├─ partial success scenario
├─ error in one processor doesn't stop others
├─ all execution logs created
├─ metrics recorded
└─ audit events registered
```

### Database Tests (5+ tests)
```
Query methods validation
├─ countExpiredBefore accuracy
├─ deleteExpiredBefore correctness
├─ soft delete preserves data integrity
├─ anonymization masks PII
└─ audit trail completeness
```

---

## Acceptance Criteria by Feature

### LGPD-201 Architecture
- [ ] `RetentionPolicyService.executeEnabledPolicies()` replaces `apply_noop()`
- [ ] Dry-run doesn't modify data
- [ ] Apply mode validates required fields before execution
- [ ] All executions logged to `tb_retention_execution_log`
- [ ] Each domain processor independent and tested
- [ ] Metrics recorded for each execution
- [ ] Processor failure doesn't affect other processors

### LGPD-202 Token Retention
- [ ] `TokenRetentionProcessor` created and tested
- [ ] Dry-run returns count without deletion
- [ ] Apply mode deletes expired tokens only
- [ ] Valid tokens never deleted
- [ ] Tests cover both domains (blacklist, reset)
- [ ] No valid token accidentally deleted

### LGPD-203 Message Retention
- [ ] `MessageRetentionProcessor` created
- [ ] Migration V10 adds required fields
- [ ] Soft delete preserves data for audit
- [ ] Legal process messages never deleted
- [ ] Front-end queries respect `deleted_at`
- [ ] Incidents/LGPD requests preserved

### LGPD-204 Document Retention
- [ ] `DocumentRetentionProcessor` created
- [ ] Migration V11 adds retention fields
- [ ] S3 files deleted when document removed
- [ ] Metadata preserved for audit
- [ ] Fiscal/labor documents preserved
- [ ] Biometric term preserved as evidence
- [ ] Type-specific rules applied

### LGPD-205 Audit Log Retention
- [ ] `AuditLogRetentionProcessor` created
- [ ] Migration V12 adds anonymization fields
- [ ] IP addresses anonymized (not deleted)
- [ ] User agents sanitized
- [ ] Details sanitized (no CPF, email, etc.)
- [ ] Critical security logs preserved longer
- [ ] Historical value maintained

---

## Risk Assessment

### Risk 1: Data Loss
**Severity:** Critical  
**Mitigation:**
- Dry-run validation before APPLY
- Comprehensive audit trail
- Backups before major runs
- Tests covering edge cases

### Risk 2: Performance Impact
**Severity:** Medium  
**Mitigation:**
- Batch deletion instead of row-by-row
- Pagination for large datasets
- Scheduled off-peak runs
- Monitoring and alerting

### Risk 3: Concurrent Execution
**Severity:** Medium  
**Mitigation:**
- Mutual exclusion lock on policy execution
- Execution log prevents duplicate runs
- Idempotent operations

### Risk 4: S3/Rekognition Failures
**Severity:** Medium  
**Mitigation:**
- Retry logic with exponential backoff
- Log failures for manual review
- DB state is source of truth (not S3)
- Async cleanup job for orphaned S3 objects

### Risk 5: PII in Anonymized Data
**Severity:** Medium  
**Mitigation:**
- Comprehensive `SensitiveDataMasker`
- Test cases with sample PII
- Manual review before production
- Metrics on redaction success

---

## Known Dependencies

### From Sprint 1
- `LegalConsent` model (may need preservation rules)
- `RetentionPolicy` model (already exists)
- `AuditService` for logging

### External Services
- AWS S3 (document deletion)
- AWS Rekognition (if used, but probably not relevant for retention)
- Database (all operations)

---

## Success Criteria for Sprint 2

1. ✅ All 5 features implemented
2. ✅ All acceptance criteria met
3. ✅ All unit tests passing (25+)
4. ✅ All integration tests passing (5+)
5. ✅ All database migrations applied cleanly
6. ✅ No data loss incidents
7. ✅ Dry-run and Apply modes both functional
8. ✅ Comprehensive audit trail for all operations
9. ✅ No sensitive data in logs
10. ✅ Documentation complete

---

## Next Steps (After Sprint 2)

1. Manual testing of retention flows in staging
2. Load testing for batch operations
3. Scheduling implementation (Quartz or Spring Scheduler)
4. Admin UI for manual retention execution
5. Metrics dashboard for retention operations

---

## Questions for Clarification

Before starting implementation, confirm:

1. **Batch Size:** How many records to delete per transaction? (Suggest: 1000)
2. **Scheduling:** How often should retention run? (Suggest: nightly)
3. **Preservation Period:** How long to preserve before deletion? (Already in RetentionPolicy, but confirm ranges)
4. **Labor Data:** What constitutes labor data requiring preservation? (Suggest: TimeRecord, EmployeeContract)
5. **Fiscal Data:** What constitutes fiscal data? (Suggest: InvoiceRecord, LegalReport)

---

**Prepared By:** Sprint Planning  
**Date:** 2026-05-22  
**Status:** Ready for Implementation  
**Awaiting:** Team approval and clarification answers
