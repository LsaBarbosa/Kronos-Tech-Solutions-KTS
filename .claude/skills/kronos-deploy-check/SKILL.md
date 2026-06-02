# kronos-deploy-check

**Purpose:** Pre-deployment safety checklist. Validates branch, tests, build, migrations, and rollback plan WITHOUT executing the deployment.

**Trigger:** User runs `/kronos-deploy-check` before pushing to production.

---

## Scope

Pre-deployment validation checklist:

1. **Branch & Git State**
   - Is branch clean (no uncommitted changes)?
   - Are commits on top of main/staging?
   - Is branch up-to-date with remote?
   - Are there any merge conflicts?

2. **Code Quality**
   - Checkstyle passes?
   - No hardcoded secrets/passwords?
   - No `println`, `System.out.println`, debug logs?
   - No `TODO`, `FIXME`, `HACK` comments?

3. **Build**
   - Does build succeed? `./gradlew clean bootJar -x test`
   - Is artifact size reasonable (no bloat)?
   - Are all dependencies resolvable?

4. **Tests**
   - Do all unit tests pass? `./gradlew test`
   - Do integration tests pass?
   - Is code coverage acceptable (>80%)?
   - Are new tests added for new features?

5. **Database & Migrations**
   - Are migrations versionable (Flyway format)?
   - Are migrations idempotent (safe to run multiple times)?
   - Are migrations tested in development?
   - No destructive operations (DROP, DELETE, TRUNCATE)?
   - Data retention policies honored?

6. **Environment & Configuration**
   - Are all required environment variables documented?
   - Are defaults safe?
   - Are secrets NOT in code?
   - Is configuration externalized (from Spring config, env, vault)?

7. **Security**
   - LGPD compliance verified (via `kronos-lgpd-review`)?
   - Architecture sound (via `kronos-backend-review`)?
   - No secrets hardcoded?
   - JWT/Auth properly configured?
   - Tenant isolation enforced?

8. **Health & Monitoring**
   - Health endpoint works locally? `curl localhost:8080/actuator/health`
   - Metrics endpoint accessible? `curl localhost:8080/actuator/prometheus`
   - Nginx configuration correct?
   - Rollback procedure documented?

9. **Documentation**
   - CHANGELOG updated?
   - Database schema changes documented?
   - New features documented in README?
   - Breaking changes documented?

---

## Execution Flow

1. **User initiates:**
   ```bash
   /kronos-deploy-check --env=staging
   ```

2. **Claude performs checks:**
   - Run all validation commands
   - Generate report
   - Identify blockers (MUST FIX before deploy)
   - Identify warnings (SHOULD FIX before deploy)

3. **Output:**
   - Checklist report (✅ pass / ⚠️ warning / ❌ fail)
   - Blockers list
   - Approval decision

4. **User decision:**
   - If no blockers: User can proceed with deployment
   - If blockers exist: User must fix and re-run check

---

## Report Template

```
## 🚀 Pre-Deployment Checklist: [Branch]

### 📊 Summary
- **Environment:** [staging | production]
- **Branch:** [branch name]
- **Commits:** [X new commits on top of main]
- **Status:** ✅ READY | ⚠️ WARNINGS | ❌ BLOCKED

---

### ✅ Git & Code Quality

| Check | Status | Notes |
|-------|--------|-------|
| Branch clean | ✅ | No uncommitted changes |
| Up-to-date with main | ✅ | Last sync: 10 minutes ago |
| No merge conflicts | ✅ | |
| Checkstyle passes | ✅ | 0 violations |
| No hardcoded secrets | ✅ | Checked with grep |
| No debug code | ✅ | No println, System.out |
| No TODO/FIXME | ⚠️ | 2 TODOs in comments (okay if tracked in issue) |

### ✅ Build & Tests

| Check | Status | Notes |
|-------|--------|-------|
| Build succeeds | ✅ | `./gradlew clean bootJar -x test` |
| Artifact size | ✅ | 125 MB (reasonable) |
| Unit tests pass | ✅ | 1654 tests passed, 0 failed |
| Integration tests pass | ✅ | 45 IT tests passed |
| Coverage | ✅ | 85% (meets threshold) |
| New tests for changes | ✅ | 5 new tests added |

### ✅ Database

| Check | Status | Notes |
|-------|--------|-------|
| Migrations are versioned | ✅ | Flyway V001...V015 |
| Migrations are idempotent | ✅ | Use `IF EXISTS`, `IF NOT EXISTS` |
| No destructive operations | ✅ | All migrations use soft-delete |
| Retention policies honored | ✅ | Reviewed in migration scripts |

### ✅ Configuration & Environment

| Check | Status | Notes |
|-------|--------|-------|
| All env vars documented | ✅ | See .env.example |
| Defaults are safe | ✅ | Reviewed in application.yml |
| No secrets in code | ✅ | Checked with grep patterns |
| External config working | ✅ | Tested locally |

### ✅ Security & Compliance

| Check | Status | Notes |
|-------|--------|-------|
| LGPD review passed | ✅ | Run `/kronos-lgpd-review` |
| Backend review passed | ✅ | Run `/kronos-backend-review` |
| No SQL injection | ✅ | Uses JPA Queries only |
| Tenant isolation | ✅ | All queries filtered by tenant_id |
| Auth/JWT configured | ✅ | Spring Security enabled |

### ✅ Health & Monitoring

| Check | Status | Notes |
|-------|--------|-------|
| Health endpoint works | ✅ | `curl localhost:8080/actuator/health` → UP |
| Metrics available | ✅ | Prometheus metrics enabled |
| Nginx config valid | ✅ | Syntax check passed |
| Rollback plan ready | ✅ | Documented below |

### 📝 Documentation

| Check | Status | Notes |
|-------|--------|-------|
| CHANGELOG updated | ✅ | Entry for version X.Y.Z |
| Schema changes documented | ✅ | Migration V015 documented |
| README updated | ✅ | New feature documented |
| Breaking changes noted | ⚠️ | None, but note config changes |

---

### 🔴 Blockers (MUST FIX)
[If any - deployment cannot proceed]

1. **Security Issue:** SQL injection in user search
   - File: src/main/java/UserRepository.java:45
   - Fix: Use JPA Named Query instead of string concat
   - Status: MUST FIX before deploy

2. **LGPD Violation:** CPF stored unencrypted
   - File: src/main/java/domain/Employee.java:67
   - Fix: Add @Encrypt annotation
   - Status: MUST FIX before deploy

### ⚠️ Warnings (SHOULD FIX)

1. **Code Quality:** 2 TODO comments
   - Status: Acceptable if tracked in issue

2. **Performance:** Migration adds new index (non-blocking, may slow startup)
   - Recommendation: Run with `-Dspring.jpa.hibernate.ddl-auto=none`

---

### 🔄 Rollback Plan

If deployment causes issues, rollback to previous version:

```bash
# 1. Revert code
git revert <commit-hash>
./gradlew clean bootJar -x test

# 2. Downgrade container image
docker pull kronos:v1.2.3  # Previous stable version
docker stop kronos
docker rm kronos
docker run -d --name kronos ... kronos:v1.2.3

# 3. Rollback database (if migrations exist)
# Flyway automatically handles rollback for V001...V014
# For V015, manually reverse if needed
psql -c "DELETE FROM flyway_schema_history WHERE version = 15;"

# 4. Monitor health
curl https://kronos.prod/actuator/health

# 5. Alert team
# Rollback complete, system operational
```

---

### ✅ Approval Decision

**Status: ✅ READY FOR DEPLOYMENT**

All critical checks passed. No blockers detected. 

**Next steps:**
1. User executes: `git push origin new-ui`
2. User merges into main (via GitHub PR)
3. CI/CD pipeline runs (automated)
4. User monitors production health

---

### 📊 Deployment Metadata
- **Prepared by:** Claude Code
- **Timestamp:** 2026-06-02 15:30:00 UTC
- **Environment:** staging → production
- **Estimated downtime:** 0 minutes (rolling update)
- **Rollback time:** ~5 minutes if needed
```

---

## How to Use

```bash
cd /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS

# Check readiness for staging
/kronos-deploy-check --env=staging

# Check readiness for production
/kronos-deploy-check --env=production

# Full report with recommendations
/kronos-deploy-check --verbose
```

---

## Important Notes

- ✅ Validation only - does NOT execute deployment
- ✅ Comprehensive checklist covers all critical areas
- ✅ Identifies blockers vs. warnings
- ✅ Provides rollback plan if needed
- ✅ Ensures deploy safety and compliance
- 🔒 Protects against production incidents
- ⚠️ User must manually execute `git push` and CI/CD
- 📌 Run BEFORE every merge to main/production
