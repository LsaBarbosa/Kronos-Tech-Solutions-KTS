# Pre-Production Deployment Checklist

**Branch**: Check before merging to MAIN and deploying to production.

**Last Updated**: 2026-05-13

**Version**: 1.0

---

## 1. Build & Compilation

- [ ] `./gradlew clean build` passes without errors
- [ ] No compilation warnings related to code (ignore external library warnings)
- [ ] `./gradlew check` passes
- [ ] No duplicate imports in any file
- [ ] No unused imports (IDE shows no warnings)

---

## 2. Testing

- [ ] `./gradlew unitTest` passes (799+ tests)
- [ ] `./gradlew integrationTest` passes if exists
- [ ] Test coverage meets minimum threshold (>75%)
- [ ] No flaky tests (run twice to verify consistency)
- [ ] No commented-out test code or `@Disabled` tests without explanation

---

## 3. Database & Migrations

- [ ] All migrations apply cleanly to fresh database
- [ ] `./gradlew flywayValidate` passes
- [ ] No hardcoded data in migrations
- [ ] Foreign key constraints are explicit
- [ ] Indexes exist for all foreign keys
- [ ] `ddl-auto: validate` confirmed in production config
- [ ] No direct SQL modifications (everything through migrations)
- [ ] Backup & restore tested on staging

---

## 4. Authentication & Authorization

- [ ] JWT tokens not exposed in response body (only in cookies)
- [ ] JWT token expiration is 15 minutes
- [ ] Cookie contains `HttpOnly` flag
- [ ] Cookie contains `Secure` flag in production
- [ ] Cookie contains `SameSite=Lax` flag
- [ ] Logout endpoint properly expires cookie (max-age=0)
- [ ] Authentication fails gracefully with 401/403
- [ ] Role-based access control (RBAC) enforced consistently
- [ ] Biometric acceptance properly tokenized
- [ ] No hardcoded roles or permissions

---

## 5. Data Security & LGPD

- [ ] No plain-text passwords stored (bcrypt, scrypt, or PBKDF2)
- [ ] Sensitive data encrypted at rest (passwords, biometric hashes)
- [ ] No sensitive data in logs (passwords, tokens, CPF, PIS)
- [ ] Soft delete implemented for legal data retention
- [ ] Deleted data not accessible in APIs
- [ ] LGPD right-to-erasure endpoint implemented
- [ ] Data export functionality available for users
- [ ] No cross-tenant data leaks (verified in integration tests)
- [ ] Audit logs capture all sensitive operations

---

## 6. API Security

- [ ] CORS restricted to real domain (not `*`)
- [ ] CORS credentials mode properly configured
- [ ] CSRF protection enabled via SameSite cookies and token validation
- [ ] Input validation on all endpoints
- [ ] No SQL injection vulnerabilities (all parameterized queries)
- [ ] No command injection vulnerabilities
- [ ] No XXE vulnerabilities (XML parsing disabled if not needed)
- [ ] Rate limiting implemented on authentication endpoints
- [ ] Rate limiting implemented on file upload endpoints
- [ ] Error responses don't leak stack traces or internal paths
- [ ] Swagger/OpenAPI disabled in production profile

---

## 7. File Upload & Storage

- [ ] Allowed file types restricted (PDF, JPEG, PNG only)
- [ ] File size limits enforced (5MB default)
- [ ] Doc/Docx files explicitly rejected
- [ ] Executable files rejected (.exe, .jar, etc.)
- [ ] File content validated (magic bytes check)
- [ ] Files stored outside web root
- [ ] Files served through secured download endpoint
- [ ] Virus scanning integrated (if applicable)
- [ ] S3 buckets private (no public read access)
- [ ] S3 versioning enabled for recovery

---

## 8. Configuration & Secrets

- [ ] No secrets in code (all in environment variables)
- [ ] No `.env` files committed
- [ ] No default passwords in config
- [ ] `application-prod.yml` uses environment variable references
- [ ] Logging level is INFO or WARN (not DEBUG)
- [ ] Debug endpoints disabled in production
- [ ] Feature flags properly configured
- [ ] All external service credentials in environment variables
- [ ] AWS credentials use IAM roles (not hardcoded keys)

---

## 9. Infrastructure & Deployment

- [ ] Dockerfile uses non-root user (`kronos` user)
- [ ] Docker image based on `eclipse-temurin:21-jre-jammy`
- [ ] Docker image includes HEALTHCHECK directive
- [ ] Health endpoints respond correctly:
  - `/actuator/health/liveness` returns 200
  - `/actuator/health/readiness` returns 200
- [ ] Memory limits configured (512MB minimum, 1GB recommended)
- [ ] JVM GC parameters optimized (`-XX:MaxGCPauseMillis=200`)
- [ ] Container runs as non-root user
- [ ] No hardcoded ports (use environment variable for port 8080)
- [ ] Graceful shutdown configured (30s timeout)

---

## 10. Monitoring & Logging

- [ ] Prometheus metrics endpoint available (`/actuator/prometheus`)
- [ ] Key metrics monitored:
  - [ ] Application availability
  - [ ] Database connection pool
  - [ ] API response times
  - [ ] Authentication failures
  - [ ] File upload success rate
- [ ] Structured logging in JSON format (if applicable)
- [ ] Audit logs capture:
  - [ ] Login/logout events
  - [ ] Document access
  - [ ] Data modifications
  - [ ] Configuration changes
  - [ ] Biometric operations
- [ ] Log retention configured (minimum 30 days)
- [ ] Alerts configured for critical errors
- [ ] Dashboard created for operations team

---

## 11. Security Scanning

- [ ] `./gradlew dependencyCheck` runs without high/critical vulns
- [ ] OWASP Top 10 review completed
- [ ] CVE scanning completed (Trivy or similar)
- [ ] No known vulnerable dependencies
- [ ] Security headers configured:
  - [ ] X-Content-Type-Options: nosniff
  - [ ] X-Frame-Options: DENY
  - [ ] X-XSS-Protection: 1; mode=block
  - [ ] Strict-Transport-Security: max-age=31536000
- [ ] WAF rules configured on load balancer
- [ ] DDoS protection enabled

---

## 12. Performance & Load Testing

- [ ] Response times acceptable under normal load
- [ ] Database query performance verified (no N+1 queries)
- [ ] Connection pools properly sized
- [ ] No memory leaks detected (heap analysis)
- [ ] Caching configured for static resources
- [ ] Pagination implemented for large result sets
- [ ] Load tested with realistic data volume

---

## 13. Documentation

- [ ] API documentation complete and accurate
- [ ] Architecture documentation updated
- [ ] Deployment guide created and tested
- [ ] Runbooks created for common operations
- [ ] Security policies documented:
  - [ ] Session policy
  - [ ] CSRF protection strategy
  - [ ] Data retention policy
- [ ] Troubleshooting guide prepared
- [ ] On-call procedures defined

---

## 14. Backup & Disaster Recovery

- [ ] Database backup automated and tested
- [ ] Backup retention policy defined (30 days minimum)
- [ ] Weekly restore test from backup
- [ ] Database replication/failover configured
- [ ] Document storage (S3) backup enabled
- [ ] Recovery Time Objective (RTO) defined
- [ ] Recovery Point Objective (RPO) defined
- [ ] Disaster recovery drill completed

---

## 15. Accessibility & Compliance

- [ ] No hardcoded timezone assumptions (use user timezone)
- [ ] Date/time formats internationalized
- [ ] Error messages user-friendly
- [ ] No personally identifiable information in URLs
- [ ] Unicode/UTF-8 properly handled
- [ ] Mobile-friendly UI (if applicable)
- [ ] Compliance with Brazilian regulations (LGPD, CLT)

---

## 16. Code Quality & Review

- [ ] Code review completed by at least 1 team member
- [ ] All comments and TODOs resolved
- [ ] No debugging code left (`System.out.println`, etc.)
- [ ] No temporary branches or test code merged
- [ ] Commit messages are clear and descriptive
- [ ] No code commented out (delete or create issue)
- [ ] Naming conventions followed consistently
- [ ] Duplicated code consolidated where practical

---

## 17. Smoke Tests

After deployment to production:

- [ ] Application starts without errors
- [ ] Health checks pass
- [ ] Login functionality works
- [ ] Document upload works
- [ ] Document download works
- [ ] Time record creation works
- [ ] User can navigate key flows
- [ ] No unexpected errors in logs
- [ ] Performance metrics within baseline

---

## 18. Final Sign-Off

**Before merging to main:**

- [ ] All items checked
- [ ] No blockers or high-priority issues
- [ ] Team lead approval obtained
- [ ] Security review completed
- [ ] Operations team notified
- [ ] Rollback plan documented
- [ ] Deployment window scheduled
- [ ] Status page updated

**After production deployment:**

- [ ] Monitoring dashboards checked
- [ ] No critical alerts
- [ ] Smoke tests passed
- [ ] Team notified of successful deployment
- [ ] Post-deployment review scheduled (24h after)

---

## Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| Code Reviewer | | | |
| Team Lead | | | |
| Security Review | | | |
| Operations | | | |

---

## Notes

Use this space for additional notes or exceptions:

```




```

---

## Related Documentation

- [Deployment Guide](docs/production/hostinger-deploy.md)
- [Security Policies](docs/security/)
- [Data Retention Policy](docs/legal/data-retention.md)
- [Database Migrations](docs/database/migrations.md)

---

**Last Review**: [Date]
**Next Review Due**: [Date + 3 months]
