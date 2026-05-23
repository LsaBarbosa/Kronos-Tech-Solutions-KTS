# LGPD Compliance Deployment Checklist

This checklist ensures all LGPD compliance features are properly configured and tested before production deployment.

## Pre-Deployment Verification (Development/Staging)

### Code Quality and Testing

- [ ] All LGPD compliance unit tests pass (`./gradlew test`)
- [ ] LGPD integration tests pass in staging environment
- [ ] No compilation warnings in LGPD-related modules
- [ ] Code review completed for all retention and anonymization processors
- [ ] Security review completed for data access patterns

### Functional Testing

- [ ] Test data retention DRY_RUN on all resource types (AUDIT_LOG, BIOMETRIC_ARTIFACT, LGPD_REQUEST, etc.)
- [ ] Verify retention DRY_RUN logs accurate record counts
- [ ] Test anonymization DRY_RUN for multiple employee records
- [ ] Verify anonymization result persistence to database
- [ ] Test LGPD request status transitions with anonymization validation
- [ ] Verify LGPD request cannot be concluded without anonymization result
- [ ] Verify LGPD request cannot be concluded with FAILED anonymization status
- [ ] Verify LGPD request can only transition to PARTIALLY_COMPLETED with PARTIAL_SUCCESS
- [ ] Test data export with PII masking (email, CPF, tokens, paths)
- [ ] Verify exported data does not contain unmasked sensitive information

### Database Migrations

- [ ] All Flyway migrations have been validated (V22 and any other recent migrations)
- [ ] Database schema matches expected table structure
- [ ] All foreign keys and indexes are properly created
- [ ] `tb_anonymization_consolidated_result` table exists with correct columns
- [ ] Test migration rollback and reapply on staging database
- [ ] Verify data integrity after migration application

### Environment Configuration

- [ ] `.env.example` includes all required LGPD variables with safe defaults
- [ ] Production environment variables documented in deployment guide
- [ ] All configuration variables have secure defaults (scheduler disabled, APPLY disabled)
- [ ] Configuration validation logic prevents invalid combinations

### Documentation

- [ ] [scheduler-activation.md](./scheduler-activation.md) is complete and accurate
- [ ] Deployment guide includes LGPD activation procedure
- [ ] Runbooks include LGPD scheduler troubleshooting
- [ ] On-call team has access to LGPD documentation

## Production Deployment Steps

### 1. Pre-Deployment (Before Code Deployment)

- [ ] Backup production database
- [ ] Notify compliance team of deployment window
- [ ] Schedule deployment for low-traffic time window
- [ ] Prepare rollback plan and test it
- [ ] Ensure on-call team is aware and available

### 2. Code Deployment

- [ ] Deploy code to production (includes all retention and anonymization features)
- [ ] Verify application starts successfully
- [ ] Check startup logs for any LGPD-related errors
- [ ] Verify database migrations applied successfully

### 3. ⚠️ MANDATORY: Decide Scheduler Policy

**This step MUST be completed before production deployment. Deployment should fail without explicit decision.**

Choose ONE option below and configure accordingly:

#### Option A: Conservative (Recommended for first deployment)
```env
LGPD_RETENTION_SCHEDULER_ENABLED=false
LGPD_RETENTION_ALLOW_APPLY=false
```
- [ ] Scheduler completely disabled
- [ ] Manual retention execution only
- [ ] No automatic data processing
- [ ] Decision recorded: Conservative approach chosen on [DATE]

#### Option B: Monitoring Only (DRY_RUN)
```env
LGPD_RETENTION_SCHEDULER_ENABLED=true
LGPD_RETENTION_ALLOW_APPLY=false
```
- [ ] Scheduler enabled and running on schedule
- [ ] Runs only in DRY_RUN mode (no data modification)
- [ ] Provides metrics and impact assessment
- [ ] Decision recorded: Monitoring-only approach chosen on [DATE]

#### Option C: Full Automation (After testing)
```env
LGPD_RETENTION_SCHEDULER_ENABLED=true
LGPD_RETENTION_ALLOW_APPLY=true
```
- [ ] Scheduler enabled with full APPLY execution
- [ ] Automatic data retention processing
- [ ] Only after successful DRY_RUN phase (minimum 2 weeks)
- [ ] Decision recorded: Full automation chosen on [DATE]

**Deployment MUST FAIL if these variables are not explicitly set in production configuration.**

### 4. Initial Validation (After Scheduler Decision)

- [ ] Confirm chosen scheduler configuration is deployed (`LGPD_RETENTION_SCHEDULER_ENABLED` and `LGPD_RETENTION_ALLOW_APPLY` match decision)
- [ ] Verify `LGPD_RETENTION_SCHEDULER_CRON=0 15 4 * * ?` or custom cron is appropriate
- [ ] Test LGPD request creation and basic workflow
- [ ] Test data export functionality works correctly
- [ ] Test anonymization DRY_RUN via API (if exposed)
- [ ] Verify no errors in application logs related to scheduler configuration

### 5. Follow-Up (Based on Chosen Option)

#### If Option A (Conservative - Scheduler Disabled)

- [ ] Monitor application logs for 24 hours (no scheduler errors expected)
- [ ] Verify manual LGPD operations work correctly via API
- [ ] Document when/if scheduler will be enabled in future
- [ ] Set reminder to re-evaluate scheduler policy quarterly

#### If Option B (Monitoring Only - DRY_RUN)

- [ ] Monitor first scheduled execution (should occur at configured CRON time)
- [ ] Verify DRY_RUN produces logs with record counts
- [ ] Verify DRY_RUN does NOT modify any data
- [ ] Monitor for 3-5 consecutive executions (verify cron schedule works)
- [ ] Collect baseline metrics (record counts, resource types affected)
- [ ] Plan transition to Option C after minimum 2 weeks of successful DRY_RUN
- [ ] Document DRY_RUN baseline for comparison with APPLY phase

#### If Option C (Full Automation - APPLY)

**ONLY if already completed at least 2 weeks of successful DRY_RUN phase:**

- [ ] Verify DRY_RUN phase executed successfully without errors
- [ ] Monitor first APPLY execution closely (will modify data)
- [ ] Verify APPLY execution modifies records as expected
- [ ] Verify audit logs are created for all retention operations
- [ ] Verify critical audit logs still contain userId (evidence preservation)
- [ ] Monitor for partial-success scenarios and verify error handling
- [ ] Monitor for 3-5 consecutive executions
- [ ] Compare before/after record counts against DRY_RUN estimates

## Post-Deployment Verification

- [ ] Verify anonymization results are persisted with correct status
- [ ] Verify LGPD request status transitions respect validation rules
- [ ] Test that blocked LGPD conclusions produce appropriate error messages
- [ ] Monitor data retention metrics in production (Prometheus dashboard)
- [ ] Verify no unexpected data loss or corruption
- [ ] Confirm compliance audit trails are complete and accurate

## Ongoing Operations

### Weekly

- [ ] Review scheduler execution logs
- [ ] Monitor retention metrics dashboard
- [ ] Check for any ERROR or FAILED status in retention execution logs
- [ ] Verify evidence preservation (audit log userId not cleared)

### Monthly

- [ ] Audit anonymization result accuracy
- [ ] Review LGPD request metrics and status distributions
- [ ] Verify data retention policies align with legal requirements
- [ ] Test anonymization and retention in staging environment

### Quarterly

- [ ] Full compliance audit of retention and anonymization operations
- [ ] Review legal requirements changes
- [ ] Update retention policies if needed
- [ ] Test disaster recovery and rollback procedures

## Rollback Procedures

### Emergency Rollback (Scheduler Disable)

If the scheduler causes issues in production:

```bash
# Set environment variable
export LGPD_RETENTION_SCHEDULER_ENABLED=false

# Restart application
docker restart kronos-backend
# or
systemctl restart kronos-backend
```

### Full Rollback (Code Revert)

If critical issues require full code revert:

1. Disable scheduler: `LGPD_RETENTION_SCHEDULER_ENABLED=false`
2. Revert code to previous version
3. Revert database migrations (if applied)
4. Test thoroughly in staging before re-enabling

## Escalation Contacts

- **Compliance Team**: [contact info]
- **Database Administration**: [contact info]
- **On-Call Engineering**: [contact info]
- **Legal Team**: [contact info]

## Related Documentation

- [Scheduler Activation Guide](./scheduler-activation.md)
- [LGPD Implementation Report](./lgpd-implementation-report.md)
- [Data Retention Resource Coverage](./retention-resource-coverage.md)
- [Environment Configuration](../deploy/ENV-PRODUCTION.md)

## Appendix: Key Metrics

### Retention Execution Metrics

- `retention.records.scanned`: Total records examined by retention processor
- `retention.records.affected`: Records modified or deleted
- `retention.records.skipped`: Records ineligible for retention
- `retention.records.errors`: Processing errors encountered
- `retention.execution.duration_ms`: Execution time

### Anonymization Metrics

- `anonymization.execution.total`: Total anonymization executions
- `anonymization.execution.success`: Successful completions
- `anonymization.execution.partial_success`: Partial completions
- `anonymization.execution.failed`: Failed executions
- `anonymization.records.anonymized`: Total records anonymized

## Sign-Off

- **Deployment Date**: _______________
- **Deployed By**: _______________
- **Reviewed By**: _______________
- **Approved By**: _______________
- **Compliance Officer**: _______________
