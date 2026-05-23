# Scheduler Activation & Safety Controls

**Date:** 2026-05-23  
**Version:** 1.0  
**Status:** ✅ IMPLEMENTED

---

## Overview

Sprint LGPD-CORR-02 implements controlled activation of the retention scheduler for production environments with safety mechanisms to prevent accidental data deletion.

---

## Changes Implemented

### 1. Retention Scheduler Configuration

**File:** `application.yml`

```yaml
kronos:
  lgpd:
    retention:
      scheduler:
        enabled: ${LGPD_RETENTION_SCHEDULER_ENABLED:false}
        cron: ${LGPD_RETENTION_SCHEDULER_CRON:0 15 4 * * ?}
      allow-apply: ${LGPD_RETENTION_ALLOW_APPLY:false}
```

**Production Deployment:**

For production, set the following environment variables:

```env
LGPD_RETENTION_SCHEDULER_ENABLED=true
LGPD_RETENTION_ALLOW_APPLY=false  # Keep false initially; only enable after testing
```

### 2. APPLY Execution Safety Lock

**Component:** `RetentionPolicyExecutor`

When a retention policy has `executionMode=APPLY`:
- Check environment flag `LGPD_RETENTION_ALLOW_APPLY`
- If flag is `false`, block execution with BLOCKED status
- Log warning event: `event=retention_apply_blocked`
- Save `RetentionExecutionLog` with `status=BLOCKED`
- No data is altered

**Example Flow:**

```
Policy APPLY requested → Check LGPD_RETENTION_ALLOW_APPLY flag
  │
  ├─ If flag=false → BLOCKED
  │  └─ Log: event=retention_apply_blocked
  │  └─ Result: status=BLOCKED, no data modified
  │
  └─ If flag=true → Execute APPLY
     └─ Processor handles deletion/sanitization
```

### 3. Execution Report Endpoints

**New Controller:** `LgpdRetentionController`

Endpoints for viewing retention execution history:

```
GET /api/lgpd/retention/executions
  - Returns paginated execution logs
  - Authorization: CTO, MANAGER
  - Query params: page, size, sort

GET /api/lgpd/retention/executions/{executionId}
  - Returns single execution details
  - Authorization: CTO, MANAGER
  - Returns 404 if not found
```

**Response Example:**

```json
{
  "executionId": "550e8400-e29b-41d4-a716-446655440000",
  "policyCode": "RET_AUDIT_LOG",
  "resourceType": "AUDIT_LOG",
  "executionMode": "DRY_RUN",
  "status": "SUCCESS",
  "scannedCount": 1250,
  "affectedCount": 1200,
  "skippedCount": 50,
  "errorCount": 0,
  "finishedAt": "2026-05-23T04:30:15Z",
  "notes": null
}
```

---

## Status Values

Retention execution can return:

| Status | Meaning | Data Modified? |
|--------|---------|-----------------|
| SUCCESS | Execution completed normally | Yes (in APPLY mode) |
| ERROR | Execution failed with exception | No |
| PARTIAL | Some processors succeeded, some failed | Partial (see notes) |
| BLOCKED | Execution blocked by safety flag | **No** |

---

## Deployment Checklist

- [ ] Review `/docs/legal/retention-resource-coverage.md` for all 8 RetentionResourceType values
- [ ] Verify each type has dedicated processor
- [ ] Run `./gradlew test --tests "*Retention*"` locally
- [ ] Enable scheduler in test: `LGPD_RETENTION_SCHEDULER_ENABLED=true`
- [ ] Keep APPLY disabled: `LGPD_RETENTION_ALLOW_APPLY=false`
- [ ] Deploy to staging
- [ ] Monitor retention executions via `/api/lgpd/retention/executions`
- [ ] Verify DRY_RUN shows expected counts
- [ ] After validation, enable APPLY: `LGPD_RETENTION_ALLOW_APPLY=true`
- [ ] Run APPLY on non-critical data first
- [ ] Monitor `/api/lgpd/retention/executions` for BLOCKED or ERROR statuses

---

## Testing

### Unit Tests

- `RetentionPolicyExecutorApplyBlockingTest` — validates APPLY safety lock
- `LgpdRetentionControllerTest` — validates execution report endpoints
- All processor tests (Blacklisted, Password Reset, Audit Log, etc.)

### Manual Testing

```bash
# 1. Check initial config (APPLY should be disabled)
curl -X GET http://localhost:8080/admin/retention/dashboard \
  -H "Authorization: Bearer <token>"

# 2. Attempt APPLY on policy (should be blocked)
curl -X POST http://localhost:8080/admin/retention/policies/RET_AUDIT_LOG/apply \
  -H "Authorization: Bearer <token>"
# Response: 200 OK with status="BLOCKED"

# 3. View blocked execution
curl -X GET http://localhost:8080/api/lgpd/retention/executions \
  -H "Authorization: Bearer <token>"
# Should see status="BLOCKED" in recent executions
```

---

## Configuration Examples

### Staging (DRY_RUN only)

```env
LGPD_RETENTION_SCHEDULER_ENABLED=true
LGPD_RETENTION_ALLOW_APPLY=false
```

Scheduler runs, policies execute in DRY_RUN mode, no data altered.

### Production (after validation)

```env
LGPD_RETENTION_SCHEDULER_ENABLED=true
LGPD_RETENTION_ALLOW_APPLY=true
```

Scheduler runs, policies execute in their configured mode (DRY_RUN or APPLY).

### Disable During Incident

```env
LGPD_RETENTION_SCHEDULER_ENABLED=false
```

Emergency stop if needed.

---

## Files Modified

| File | Change |
|------|--------|
| `application.yml` | Added `kronos.lgpd.retention.allow-apply` config |
| `RetentionPolicyExecutor.java` | Added APPLY safety check |
| `RetentionExecutionResult.java` | Added `blocked()` factory method |
| `RetentionExecutionLogProvider.java` | Added `findById()` method |
| `RetentionExecutionLogProviderImpl.java` | Implemented `findById()` |
| `ApiPaths.java` | Added LGPD retention execution paths |
| `LgpdRetentionController.java` | New controller for execution report endpoints |

---

## Logs to Monitor

Search application logs for these events:

```
event=retention_execution_start    # Scheduler triggered policy
event=retention_apply_blocked      # APPLY was blocked
event=retention_execution_complete # Policy finished
```

Example:

```
2026-05-23 04:15:00 WARN retention_apply_blocked policyCode=RET_AUDIT_LOG resourceType=AUDIT_LOG reason=APPLY_NOT_ALLOWED
```

---

## Related Documentation

- `/docs/legal/retention-resource-coverage.md` — Full coverage matrix
- `/docs/legal/SPRINT-12-FINAL-REPORT.md` — Previous sprint context

---

**Document ID:** SCHEDULER-ACTIVATION-2026-05-23  
**Version:** 1.0  
**Status:** ✅ COMPLETE
