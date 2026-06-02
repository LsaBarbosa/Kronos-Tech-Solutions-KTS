# kronos-backend-review

**Purpose:** Comprehensive review of backend changes with focus on architecture, tests, security, LGPD, and point system compliance.

**Trigger:** User runs `/kronos-backend-review` after making backend changes.

---

## Scope

Review backend changes (Java files in `src/main/java`) for:

1. **Architecture Compliance (Hexagonal)**
   - Controllers are thin (mapping only)
   - Business logic is in Domain/Application layer
   - Adapters are properly isolated
   - No framework leakage to domain

2. **Code Quality**
   - Single Responsibility Principle
   - No code duplication
   - Meaningful naming
   - No dead code

3. **Test Coverage**
   - Unit tests for domain logic
   - Integration tests for adapters
   - Test isolation (no shared state)
   - Assert messages are clear

4. **Security**
   - JWT/Bearer token validation
   - Role-based access control (RBAC)
   - Tenant isolation on every operation
   - No SQL injection
   - No hardcoded secrets

5. **LGPD Compliance**
   - No excessive data collection
   - Personal data masking in logs
   - Consent tracking for sensitive fields
   - Data retention policies enforced
   - Right-to-be-forgotten implemented

6. **Point System (REP)**
   - Timestamp validation (NTP)
   - Geofence validation
   - Biometric score protection
   - Masking in logs
   - Data retention enforced (12mo min, 6mo photos max)

7. **Database**
   - Uses JPA/Flyway for schema
   - Soft-delete for sensitive data
   - Proper indexes on FK and filtered columns
   - Migration scripts are idempotent

---

## Execution Flow

1. **Analyze Changes**
   ```bash
   git diff origin/main..HEAD -- src/main/java
   ```

2. **Check Architecture**
   - Identify changed classes
   - Verify they follow hexagonal pattern
   - Check if business logic leaked to controller

3. **Verify Tests**
   ```bash
   ./gradlew test --tests '*<ChangedClassName>Test'
   ```

4. **Security Audit**
   - Search for hardcoded secrets, API keys
   - Check authentication/authorization decorators
   - Verify tenant isolation

5. **LGPD Audit**
   - Identify data collection points
   - Check consent mechanisms
   - Verify masking in logs
   - Confirm retention policies

6. **Generate Report**
   - Status: ✅ APPROVED / ⚠️ REQUIRES FIXES / ❌ BLOCKED
   - Issues found (if any)
   - Required fixes before merge
   - Test coverage summary

---

## Report Template

```
## 🕐 Backend Review: [Branch Name]

### ✅ Status: [APPROVED | REQUIRES FIXES | BLOCKED]

### 📊 Metrics
- Files changed: X
- Tests added/modified: X
- Coverage: X%
- Architecture violations: X
- Security issues: X
- LGPD issues: X

### 🏛️ Architecture
[Summary of hexagonal compliance]

### 🔐 Security
[Summary of security checks]

### 📋 LGPD
[Summary of LGPD compliance]

### ⏱️ Point System
[Summary of REP compliance, if applicable]

### 🧪 Tests
[Summary of test coverage]

### 🔴 Issues Found (if any)
1. [Issue] - Severity: HIGH/MEDIUM/LOW
   - Path: src/main/java/...
   - Line: XXX
   - Fix: ...

### ✅ Approved for Merge
Only if status is APPROVED.
```

---

## How to Use

```bash
cd /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS

# Review current branch
/kronos-backend-review

# Or specify a branch
/kronos-backend-review --branch=fix/my-feature
```

---

## Important Notes

- ✅ Automatic checks only - does NOT execute deploy
- ✅ Detailed report on compliance areas
- ⚠️ May require manual fixes before merge
- 🔒 Protects against architectural debt, security issues, LGPD violations
