# kronos-debug-prod

**Purpose:** Analyze production errors safely without requesting sensitive credentials. Diagnose root cause and recommend safe fixes with rollback plan.

**Trigger:** User runs `/kronos-debug-prod` with error logs or incident description.

---

## Scope

Analyze production incidents safely:

1. **Symptom Extraction**
   - What is the observable user-facing behavior?
   - When did it start (timestamp)?
   - How many users affected?
   - Which feature/endpoint is failing?

2. **Log Analysis** (with data masking)
   - Parse stacktraces
   - Identify error class (NPE, DataIntegrityViolation, TimeoutException, etc.)
   - Extract context WITHOUT requesting raw secrets
   - Timeline reconstruction

3. **Root Cause Hypothesis**
   - Code review of changed components
   - Database state analysis (without raw query results)
   - Dependency/library version issues
   - Configuration mismatches
   - Recent deployments that might have caused it

4. **Confirmation Steps**
   - Commands to run (safe, read-only)
   - Metrics to check (monitoring/observability)
   - How to reproduce in staging

5. **Safe Fix**
   - Fix with code changes
   - Tests to verify fix
   - How to validate in staging before production redeploy

6. **Rollback Plan**
   - If fix causes new issues, how to rollback
   - How to restore data if necessary
   - Commands to execute safely

---

## Execution Flow

1. **User provides:**
   - Error stacktrace (may have masked fields)
   - Timestamp and duration of outage
   - Feature/endpoint affected
   - Any recent deployments or config changes

2. **Never Ask For:**
   - ❌ Raw database password
   - ❌ JWT tokens or API keys
   - ❌ Production config files
   - ❌ User email/CPF data
   - ❌ Private keys or certificates

3. **Analyze Safely**
   - Examine logs with PIIs already masked
   - Check git history for recent changes
   - Review error patterns
   - Cross-reference with monitoring/alerts

4. **Diagnose**
   - Symptom → Evidence → Hypothesis → Confirmation
   - Be explicit about certainty level

5. **Recommend**
   - Safe fix (with code diff)
   - Test plan
   - Staging validation
   - Monitoring before merge
   - Rollback procedure

---

## Report Template

```
## 🚨 Production Incident Analysis

### 📊 Incident Summary
- **Status:** INVESTIGATING | DIAGNOSED | FIXED
- **Severity:** CRITICAL | HIGH | MEDIUM | LOW
- **Start Time:** [UTC timestamp]
- **Duration:** X minutes
- **Users Affected:** X
- **Feature:** [Endpoint/Feature name]

### 🔍 Symptom
[What users are experiencing - concrete, observable behavior]
Example: "POST /api/employees/:id/check-in returns 500; app shows 'System error'"

### 📋 Evidence
[From logs - masked but informative]

1. Error Class: [e.g., `DataIntegrityViolationException`]
2. Last successful request: [timestamp]
3. First error: [timestamp]
4. Pattern: [Intermittent? All users? Specific endpoint?]
5. Recent changes: [git log around timestamp]

### 💡 Root Cause (Hypothesis)
[Most likely cause based on evidence]
- Code: [What in the code could cause this?]
- Data: [What database state could cause this?]
- Configuration: [What config mismatch?]
- Dependency: [What library version issue?]

**Certainty Level:** HIGH | MEDIUM | LOW

### 🧪 Confirmation Steps
[Commands user can run to validate hypothesis - safe, read-only]

```bash
# Example: Check if recent migration caused it
git log --oneline -20 -- src/main/resources/db/migration/

# Example: Check database state (safe query)
SELECT COUNT(*) FROM employees WHERE created_at > now() - interval '1 hour';

# Example: Check logs for specific error
grep -i 'DataIntegrityViolation' /var/log/kronos/app.log | tail -20
```

### ✅ Safe Fix
[Code changes to resolve]

```java
// Before
public void checkIn(Employee emp) {
  // vulnerable code
}

// After
public void checkIn(Employee emp) {
  // fixed code
}
```

### 🧪 Test Plan
[Unit/integration tests to verify fix]

```bash
./gradlew test --tests 'CheckInTest'
```

### 🚀 Staging Validation
[Steps to validate in staging before production redeploy]

1. Deploy to staging
2. Run smoke test: POST /api/staging/health
3. Reproduce error scenario
4. Verify fix works
5. Check logs for errors

### 🔄 Rollback Plan
[If new fix causes issues, how to rollback]

1. Revert to previous version:
   ```bash
   git revert <commit-hash>
   ./gradlew clean bootJar -x test
   docker pull kronos:previous-tag
   ```

2. Restore data if necessary (if applicable)

3. Monitor for stability

### 📈 Monitoring Post-Fix
[What to watch after redeploy to production]
- Error rate for endpoint
- Response time
- Database performance
- User reports

### 🎯 Recommendation
[PROCEED WITH FIX | INVESTIGATE FURTHER | ROLLBACK CURRENT VERSION]
```

---

## How to Use

```bash
cd /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS

# Analyze production error
/kronos-debug-prod

# Provide error details (copy-paste logs, describe symptom)
# Claude will ask clarifying questions if needed
```

### Example Input

```
Feature: Electronic Point System
Endpoint: POST /api/points/check-in
Error: 
  com.kts.kronos.domain.exception.BiometricVerificationException: Invalid biometric score
  at com.kts.kronos.domain.service.PointService.validate()
  at com.kts.kronos.adapter.in.web.http.PointController.checkIn()

Started: 2026-06-02 14:32:00 UTC
Duration: 15 minutes
Users affected: ~50

Last deployment: 2026-06-02 14:00:00 (biometric score threshold changed from 0.85 to 0.90)
```

---

## Important Notes

- ✅ Analyze safely without requesting raw secrets
- ✅ Provide concrete diagnosis based on evidence
- ✅ Never assume - request confirmation
- ✅ Safe fix + test plan + rollback plan
- 🔒 Protects company from data exposure during debugging
- 🔒 Reduces MTTR (Mean Time To Recovery) with structured diagnosis
- ⚠️ Do NOT execute deployment - user must do it manually
