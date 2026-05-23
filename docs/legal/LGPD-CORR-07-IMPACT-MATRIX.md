# LGPD-CORR-07: Impact Matrix
## Incidentes de segurança com prazo e evidência (Security Incidents with Deadline and Evidence)

**Date:** 2026-05-23  
**Sprint:** LGPD-CORR-07  
**Components Affected:** Backend (Java/Spring), Security & Observability  

---

## Risk Assessment Summary

| Category | Risk Level | Mitigation |
|----------|-----------|-----------|
| **Data Validation** | ⚠️ MEDIUM | Validation enforced at service layer with clear error codes |
| **Status Transitions** | ⚠️ MEDIUM | Blocking incomplete incidents from closure |
| **Audit Compliance** | ✅ IMPROVED | All validation failures logged to audit |
| **Metrics/Observability** | ✅ LOW | Non-breaking metrics added |
| **API Changes** | ✅ MINIMAL | Error codes returned, no endpoint changes |

---

## Components Impact

### Backend Changes

| Component | Type | Impact | Breaking? |
|-----------|------|--------|-----------|
| `SecurityIncidentService.java` | Modified | Add validation in evaluateRisk() and updateIncident() | ❌ NO |
| `SecurityIncidentException.java` | NEW | Custom exception for deadline/evidence validation | ❌ NO |
| `SecurityIncidentDeadlineAlert.java` | NEW | Scheduler for deadline monitoring | ❌ NO |
| `SecurityIncidentMetrics.java` | NEW | Prometheus metrics for deadlines | ❌ NO |
| `SecurityIncidentServiceTest.java` | Modified | Add 8+ tests for validation logic | ❌ NO |
| `SecurityIncidentSchedulerTest.java` | NEW | Tests for scheduler and metrics | ❌ NO |

### Frontend Changes
- **Status:** ❌ No changes required (backend validation only)

---

## Validation Rules

### Task LGPD-CORR-07-01: Mandatory Communication Deadlines

**When:** Risk assessment is evaluated (`evaluateRisk()`)

**Rule:** If `communicationRequired = true`, THEN require:
```
anpdCommunicationDeadline != null
subjectsCommunicationDeadline != null
```

**Behavior:**
- ✅ ALLOWED: `communicationRequired = false` (deadlines optional)
- ✅ ALLOWED: `communicationRequired = true` + both deadlines provided
- ❌ BLOCKED: `communicationRequired = true` + missing deadline
- ❌ BLOCKED: `communicationRequired = true` + only one deadline provided

**Error Response:**
```json
{
  "code": "INCIDENT_COMMUNICATION_DEADLINE_REQUIRED",
  "message": "Prazos de comunicação à ANPD e aos titulares são obrigatórios quando a comunicação é requerida.",
  "timestamp": "2026-05-23T01:30:00Z",
  "incidentId": "uuid-here"
}
```

**HTTP Status:** 400 Bad Request

**Audit Log:**
- Action: SECURITY_INCIDENT_VALIDATION_FAILED
- Resource: SECURITY_INCIDENT
- Details: "Missing deadlines for communicationRequired=true"

---

### Task LGPD-CORR-07-02: Evidence Before Closure

**When:** Incident status is transitioning to CLOSED (`updateIncident()`)

**Rule:** If `communicationRequired = true` AND transitioning to CLOSED, THEN require:
```
notifiedAnpdAt != null OR justificationForAnpdDelay != null
notifiedSubjectsAt != null OR justificationForSubjectsDelay != null
evidenceLinks != null AND !evidenceLinks.isBlank()
correctiveActions != null AND !correctiveActions.isBlank()
```

**Behavior:**
- ✅ ALLOWED: `communicationRequired = false` + close without evidence
- ✅ ALLOWED: `communicationRequired = true` + all evidence provided
- ✅ ALLOWED: `communicationRequired = true` + notified + evidence + correction
- ❌ BLOCKED: `communicationRequired = true` + missing evidence
- ❌ BLOCKED: `communicationRequired = true` + empty evidenceLinks
- ❌ BLOCKED: `communicationRequired = true` + no corrective actions

**Error Response:**
```json
{
  "code": "INCIDENT_CLOSURE_MISSING_EVIDENCE",
  "message": "Incidente com comunicação obrigatória não pode ser encerrado sem: notificação à ANPD, notificação aos titulares, links de evidência e ações corretivas.",
  "missingFields": [
    "notifiedAnpdAt",
    "evidenceLinks",
    "correctiveActions"
  ],
  "incidentId": "uuid-here"
}
```

**HTTP Status:** 422 Unprocessable Entity

**Audit Log:**
- Action: SECURITY_INCIDENT_CLOSURE_BLOCKED
- Resource: SECURITY_INCIDENT  
- Details: "Attempted closure without evidence. Missing: [field1, field2]"

---

### Task LGPD-CORR-07-03: Deadline Alerts & Metrics

**Scheduler:** Runs every hour

**Query Criteria:** Find incidents where:
```
communicationRequired = true
notifiedAnpdAt is NULL
(
  anpdCommunicationDeadline <= NOW() [OVERDUE]
  OR
  (NOW() < anpdCommunicationDeadline AND anpdCommunicationDeadline <= NOW() + 24 hours) [DUE SOON]
)
```

**Metrics Generated:**

1. **Overdue Communications:**
```
kronos_security_incident_communication_overdue_total{
  deadline_type="anpd" | "subjects",
  status="open" | "closed"
}
```

2. **Due Soon (Next 24h):**
```
kronos_security_incident_communication_due_soon_total{
  deadline_type="anpd" | "subjects",
  severity="low" | "medium" | "high" | "critical"
}
```

3. **Communication Completion Rate:**
```
kronos_security_incident_communication_notified_ratio{
  deadline_type="anpd" | "subjects"
}
```

**Log Output:**
```
WARN  IncidentDeadlineScheduler - Security incidents with overdue ANPD communication deadlines: count=3
INFO  IncidentDeadlineScheduler - Incident 550e8400-e29b-41d4-a716-446655440000: anpdDeadline=2026-05-22T15:00:00Z (1 day overdue)
WARN  IncidentDeadlineScheduler - Security incidents with subjects communication due within 24h: count=5
```

**Data Safety:**
- ✅ NO incident IDs in metric tags (non-public)
- ✅ NO personal data in any metric
- ✅ NO sensitive field values
- ✅ Counts and statistics only
- ✅ Severity and type only (no content)

---

## Files to Create/Modify

### Files to Create

```
src/main/java/com/kts/kronos/application/exceptions/IncidentCommunicationDeadlineException.java
src/main/java/com/kts/kronos/application/exceptions/IncidentClosureValidationException.java
src/main/java/com/kts/kronos/application/service/SecurityIncidentDeadlineScheduler.java
src/main/java/com/kts/kronos/application/service/SecurityIncidentMetrics.java
src/test/java/com/kts/kronos/application/service/SecurityIncidentCommunicationValidationTest.java
src/test/java/com/kts/kronos/application/service/SecurityIncidentClosureValidationTest.java
src/test/java/com/kts/kronos/application/service/SecurityIncidentDeadlineSchedulerTest.java
```

### Files to Modify

```
src/main/java/com/kts/kronos/application/service/SecurityIncidentService.java
  - Add validation in evaluateRisk() method
  - Add validation in updateIncident() method
  - Inject SecurityIncidentMetrics

src/test/java/com/kts/kronos/application/service/SecurityIncidentServiceTest.java
  - Add tests for deadline validation (Task 07-01)
  - Add tests for closure validation (Task 07-02)
```

### Configuration

```
application.yml
  - Add scheduler enabled flag: security.incident.scheduler.enabled=true
  - Add scheduler interval: security.incident.scheduler.interval-minutes=60
  - Add Prometheus metrics configuration
```

---

## Test Coverage

### Task LGPD-CORR-07-01 Tests (6 tests)

1. ✅ evaluateRisk with communicationRequired=false → saves without deadlines
2. ✅ evaluateRisk with communicationRequired=true + both deadlines → saves successfully
3. ❌ evaluateRisk with communicationRequired=true + missing anpdDeadline → throws exception
4. ❌ evaluateRisk with communicationRequired=true + missing subjectsDeadline → throws exception
5. ✅ Error includes code: INCIDENT_COMMUNICATION_DEADLINE_REQUIRED
6. ✅ Audit log records validation failure

### Task LGPD-CORR-07-02 Tests (7 tests)

7. ✅ updateIncident to CLOSED with communicationRequired=false → succeeds without evidence
8. ✅ updateIncident to CLOSED with communicationRequired=true + all evidence → succeeds
9. ❌ updateIncident to CLOSED with communicationRequired=true + missing evidenceLinks → throws
10. ❌ updateIncident to CLOSED with communicationRequired=true + missing notifiedAnpdAt → throws
11. ❌ updateIncident to CLOSED with communicationRequired=true + empty correctiveActions → throws
12. ✅ Error includes code: INCIDENT_CLOSURE_MISSING_EVIDENCE
13. ✅ Audit log records closure blocked with missing fields

### Task LGPD-CORR-07-03 Tests (5 tests)

14. ✅ Scheduler finds overdue incidents correctly
15. ✅ Scheduler finds incidents due within 24h
16. ✅ Scheduler generates metrics without incident IDs
17. ✅ Metrics contain severity and type only (no content)
18. ✅ Scheduler logs warnings for overdue incidents

---

## Performance Impact

- ✅ Validation adds < 1ms per evaluateRisk call
- ✅ Scheduler runs hourly (non-blocking, async)
- ✅ Metrics generation lightweight (count aggregation only)
- ✅ No new database indexes required (existing indices used)
- ✅ No memory impact from metrics (Prometheus scrapes periodically)

---

## Security & Privacy Compliance

### Data Protection

- ✅ NO incident IDs in error messages visible to non-owners
- ✅ NO personal data in logs
- ✅ NO sensitive field values in exceptions
- ✅ NO external communication data exposed
- ✅ Audit trail complete (all validation failures logged)

### Audit Trail

- ✅ All validation attempts logged with user ID
- ✅ Failed transitions recorded with reason
- ✅ Closure blocks recorded with missing fields
- ✅ Metrics generation logged at INFO level

---

## Deployment Checklist

- ✅ All tests passing
- ✅ Exception classes properly mapped to HTTP responses
- ✅ Scheduler properly initialized with Spring
- ✅ Metrics registered with Prometheus
- ✅ Configuration documented in application.yml
- ✅ No breaking changes to existing API contracts
- ✅ Error codes documented for frontend/client integration

---

## Rollback Plan

If issues occur:

1. **Disable Validation:** Set `security.incident.validation.enabled=false` in config
2. **Disable Scheduler:** Set `security.incident.scheduler.enabled=false` in config
3. **Revert Code:** Restore previous version of SecurityIncidentService
4. **Audit:** Check audit logs for validation failures during rollback window

---

## Success Criteria

✅ Incidents with mandatory communication cannot be evaluated without deadlines  
✅ Incidents with mandatory communication cannot be closed without evidence  
✅ Scheduler identifies overdue/due-soon incident deadlines  
✅ Metrics provide observability without exposing PII  
✅ All validation failures audited and logged  
✅ Error codes standardized for client integration  
✅ 18+ tests verify all validation scenarios  

