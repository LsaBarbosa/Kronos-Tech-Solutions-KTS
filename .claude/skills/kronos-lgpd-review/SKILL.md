# kronos-lgpd-review

**Purpose:** Dedicated LGPD (Lei Geral de Proteção de Dados) compliance audit of code changes. Focuses exclusively on data protection, privacy, and legal conformance.

**Trigger:** User runs `/kronos-lgpd-review` when adding/modifying data handling features.

---

## Scope

Audit all code changes for LGPD compliance violations:

1. **Data Collection**
   - Is collection minimal and necessary?
   - Is there a documented legal basis (consent/contract/legal obligation)?
   - Are defaults set to "collect less" (privacy by default)?
   - Is the user informed of what's being collected?

2. **Personal Data Handling**
   - CPF/CNPJ: encrypted, masked in logs, retention defined
   - Biometric (facial): score protected, photos deleted after 6mo, consent explicit
   - Geolocation: explicit consent, not collected for "future use"
   - Documents (RG, CNH, photo): encrypted, retention justified
   - Email/Phone: masked in logs, opt-out honored
   - Health-related (férias, abonos): sensitive treatment, access logs

3. **Consent Mechanism**
   - Is consent requested explicitly (not pre-checked)?
   - Is proof of consent stored with timestamp?
   - Can user revoke consent at any time?
   - Is consent granular (per-field or per-feature)?
   - Are children (< 12 years) handled separately?

4. **User Rights**
   - ✅ Access: Can user export all their data? (`/data/me` endpoint)
   - ✅ Correction: Can user edit their data?
   - ✅ Deletion: Can user trigger right-to-be-forgotten?
   - ✅ Portability: Can user download in standard format (JSON/CSV)?

5. **Data Retention**
   - Is max retention period defined per data type?
   - Is auto-deletion/anonymization scheduled?
   - Is soft-delete used (with `deleted_at`)?
   - Are backups also deleted after retention expires?

6. **Security & Protection**
   - Are sensitive fields encrypted at rest?
   - Are sensitive fields masked in logs?
   - Are sensitive fields protected in error responses?
   - Is data isolated by tenant (multi-tenancy)?
   - Are database backups protected?

7. **Logging & Monitoring**
   - Is access to personal data logged?
   - Is export/deletion logged?
   - Are logs themselves protected (not storing plaintext sensitive data)?
   - Is there audit trail for compliance review?

8. **Vendor/Third-Party**
   - Are third-party services (storage, email, biometria) data processors?
   - Are data processing agreements (DPA) in place?
   - Is data transmission encrypted?
   - Are subprocessors disclosed?

---

## Execution Flow

1. **Analyze Changes**
   ```bash
   git diff origin/main..HEAD
   ```
   - Identify data collection/handling code
   - Identify new fields or integrations

2. **Check Against Checklist**
   - Does code violate any LGPD principle?
   - Are there concrete violations or just potential issues?

3. **Search for Red Flags**
   - `SELECT *` on sensitive tables (unnecessary data)
   - Unencrypted storage of CPF, biometric data
   - Logging of sensitive fields
   - Missing retention policies
   - No consent tracking
   - Missing soft-delete

4. **Generate Report**
   - Status: ✅ COMPLIANT / ⚠️ HAS RISKS / ❌ VIOLATION
   - Issues (if any) with fix recommendations
   - Compliance summary

---

## Report Template

```
## 📋 LGPD Compliance Review: [Branch Name]

### ✅ Status: [COMPLIANT | HAS RISKS | VIOLATION]

### 🕐 Scope
- Branch: [branch name]
- Files reviewed: X
- Data types affected: [list]
- Risk level: [HIGH | MEDIUM | LOW]

### ✅ Compliant Areas
- [Area 1] - [brief explanation]

### ⚠️ Risk Areas / Issues Found
[Only if relevant]

1. **[Category]** - Severity: HIGH/MEDIUM/LOW
   - Issue: [Concrete description]
   - Path: src/main/java/...
   - Line: XXX
   - Risk: [What could go wrong]
   - Fix: [How to fix]

### 🔍 Legal Basis
- Consent: [If required, is it implemented?]
- Contract: [If processing for service, is it clear?]
- Legal obligation: [If required by law, is it documented?]

### 🔐 Data Protection
- Encryption at rest: [Status]
- Masking in logs: [Status]
- Soft-delete: [Status]
- Tenant isolation: [Status]

### 📊 User Rights
- ✅ Access right: [Implemented? Where?]
- ✅ Correction right: [Implemented? Where?]
- ✅ Deletion right: [Implemented? Where?]
- ✅ Portability: [Implemented? Format?]

### 📅 Retention Policy
- Personal data: [X days/months/years]
- Sensitive data: [X days/months/years]
- Deletion method: [soft-delete/anonymization/physical]

### 📝 Recommendation
[If COMPLIANT: "Ready for production"]
[If HAS RISKS: "Requires fixes before merge: 1) ... 2) ..."]
[If VIOLATION: "BLOCKED - must fix before merge"]
```

---

## How to Use

```bash
cd /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS

# Review current branch
/kronos-lgpd-review

# Or specify a branch
/kronos-lgpd-review --branch=feature/new-data-field
```

---

## Important Notes

- ✅ Audit only - does NOT execute deploy
- ✅ Focuses exclusively on LGPD/privacy violations
- ✅ Identifies concrete issues only (not theoretical risks)
- 🔒 Protects company from regulatory violations and fines (LGPD fines up to 2% of revenue, max BRL 50M)
- 🔒 Prevents data breaches and trust issues
- 📌 Complementary to `kronos-backend-review` (which covers general security)
