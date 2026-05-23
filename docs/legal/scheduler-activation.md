# LGPD Data Retention Scheduler Activation Guide

## Overview

The Kronos LGPD Data Retention Scheduler is a scheduled background job that executes data retention policies based on configured retention windows. This scheduler automatically identifies records that exceed their retention thresholds and executes retention processors (minimization or deletion) as configured.

**CRITICAL**: This scheduler must be explicitly enabled and authorized in production. Default configuration disables both the scheduler and the APPLY execution mode.

## Scheduler Configuration

### Environment Variables

| Variable | Default | Description | Safe Values |
|----------|---------|-------------|------------|
| `LGPD_RETENTION_SCHEDULER_ENABLED` | `false` | Enable/disable the scheduler | `true`, `false` |
| `LGPD_RETENTION_SCHEDULER_CRON` | `0 15 4 * * ?` | Cron expression (UTC) | Standard 5-field or 6-field quartz cron |
| `LGPD_RETENTION_ALLOW_APPLY` | `false` | Allow APPLY execution mode | `true`, `false` |

### Default Cron: 4:15 AM UTC Daily

The default cron expression `0 15 4 * * ?` schedules the retention processor to run every day at 4:15 AM UTC. This follows the pattern:
- Minute: 15
- Hour: 4 (UTC)
- Day of month: * (any)
- Month: * (any)
- Day of week: ? (any)
- Second: 0 (implicit)

To adjust the schedule, modify the cron expression. Examples:

```
0 30 2 * * ?     # 2:30 AM UTC daily
0 0 3 ? * MON    # 3:00 AM UTC every Monday
0 0 * * * ?      # Every hour on the hour
```

## Scheduler Behavior

### Phase 1: DRY_RUN (Always Enabled)

The scheduler always runs in DRY_RUN mode to:
1. Count records eligible for retention by resource type
2. Estimate the impact of retention policies
3. Log execution metrics without modifying data
4. Identify and report processing errors

**DRY_RUN is safe to enable independently** — it produces no data modifications.

### Phase 2: APPLY (Must Be Explicitly Authorized)

When `LGPD_RETENTION_ALLOW_APPLY=true`, the scheduler executes retention processors with the following guarantees:

- **Multi-Tenant Safety**: Only processes records within the owning company's data boundary
- **Evidence Preservation**: Critical audit logs preserve `userId` for compliance audit trails
- **Graceful Failure**: If any resource type processor fails, execution continues with others (partial success)
- **Comprehensive Logging**: All executed operations are audited and logged

Each resource type processor implements:
- Atomic record selection based on retention policies
- Explicit count-before/count-after metrics
- Transaction-scoped execution with automatic rollback on critical errors

**APPLY execution requires explicit authorization and should only be enabled in production after thorough testing.**

## ⚠️ MANDATORY: Explicit Production Decision

**BEFORE DEPLOYING TO PRODUCTION, YOU MUST EXPLICITLY CHOOSE:**

### Three Options for Production

| Option | Configuration | Use Case | Readiness |
|--------|---------------|----------|-----------|
| **Conservative** | `ENABLED=false`<br/>`ALLOW_APPLY=false` | First deployment<br/>Manual operations preferred<br/>Maximum control | Immediate |
| **Monitoring** | `ENABLED=true`<br/>`ALLOW_APPLY=false` | Assess impact<br/>Baseline metrics<br/>No data modification | Week 1-2 |
| **Automation** | `ENABLED=true`<br/>`ALLOW_APPLY=true` | Full production<br/>Automatic retention<br/>After 2+ weeks monitoring | Week 3+ |

**Deployment MUST FAIL if these variables are not explicitly set in production configuration.**

This is not a default that can be left unconfigured. Your DevOps/Infra team must:
1. Choose one option from the table above
2. Document the choice in DEPLOYMENT-CHECKLIST.md with date and approver
3. Configure the environment variables accordingly
4. Ensure deployment fails if variables are missing

## Production Activation Checklist

Follow this checklist when activating the scheduler in production:

### Pre-Activation Testing (Staging Environment)

- [ ] Test with `LGPD_RETENTION_SCHEDULER_ENABLED=false` and `LGPD_RETENTION_ALLOW_APPLY=false` (DRY_RUN disabled)
- [ ] Test with `LGPD_RETENTION_SCHEDULER_ENABLED=true` and `LGPD_RETENTION_ALLOW_APPLY=false` (DRY_RUN only)
- [ ] Verify DRY_RUN logs count records correctly and report no errors
- [ ] Review DRY_RUN metrics to understand impact scope
- [ ] Test with `LGPD_RETENTION_SCHEDULER_ENABLED=true` and `LGPD_RETENTION_ALLOW_APPLY=true` (Full execution)
- [ ] Verify APPLY execution modifies records as expected
- [ ] Verify critical audit logs preserve required evidence
- [ ] Verify retention policies work correctly for all resource types
- [ ] Monitor for any partial-success scenarios and verify error handling

### Production Activation

1. **Schedule an Appropriate Time**
   - Coordinate with database administration team
   - Choose a time with minimal production traffic
   - Plan for 15-30 minute execution window based on data volume

2. **Enable Logging and Monitoring**
   - Set `LOG_LEVEL_ROOT=INFO` to capture retention processor logs
   - Set up alerts for scheduler execution failures
   - Create monitoring dashboard for data retention metrics
   - Plan on-call coverage during first executions

3. **Activate Incrementally**
   - First: Enable scheduler with DRY_RUN mode (`LGPD_RETENTION_ALLOW_APPLY=false`)
   - Monitor for 3-5 execution cycles
   - Then: Enable APPLY mode (`LGPD_RETENTION_ALLOW_APPLY=true`)
   - Monitor for 3-5 execution cycles
   - Maintain ability to quickly rollback by setting `LGPD_RETENTION_SCHEDULER_ENABLED=false`

4. **Maintain Rollback Path**
   - Keep `LGPD_RETENTION_SCHEDULER_ENABLED=false` as the emergency stop
   - Document rollback procedure
   - Ensure on-call team can execute rollback within 5 minutes

## Monitoring and Observability

### Logs to Monitor

The scheduler produces logs at INFO level with the following patterns:

```
event=retention_execution_start resourceType=AUDIT_LOG executionMode=DRY_RUN
event=retention_processor_complete status=SUCCESS affected=42
event=retention_execution_failed error=...
```

### Metrics to Track

- Execution frequency (should match cron schedule)
- Records scanned per resource type
- Records affected (modified/deleted)
- Records skipped (ineligible)
- Error count and error types
- Execution duration

### Alerts to Configure

- Scheduler execution failed (critical)
- Partial success with > 10% error rate (warning)
- Execution took > 30 minutes (warning)
- Critical audit log userId cleared unexpectedly (critical)

## Disabling the Scheduler

To disable the scheduler immediately:

```bash
export LGPD_RETENTION_SCHEDULER_ENABLED=false
# Restart the application
```

To disable APPLY execution while keeping DRY_RUN monitoring:

```bash
export LGPD_RETENTION_ALLOW_APPLY=false
# Restart the application
```

## Related Documentation

- [LGPD Retention Implementation](./lgpd-implementation-report.md)
- [Data Retention Resource Coverage](./retention-resource-coverage.md)
- [Deployment Checklist](../deploy/DEPLOYMENT-CHECKLIST.md)
- [Environment Configuration](../deploy/ENV-PRODUCTION.md)

## Support and Escalation

For scheduler issues in production:

1. Check logs for execution patterns and error messages
2. Verify retention policy configuration matches data expectations
3. Contact the Kronos compliance team with:
   - Execution logs from failed run
   - Data volume metrics (record counts affected)
   - Error messages and stack traces
   - Current scheduler configuration
