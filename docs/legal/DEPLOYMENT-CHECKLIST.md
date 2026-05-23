# Sprint 7 LGPD Deployment Checklist

**Project:** Kronos - LGPD Request Workflow System  
**Date:** 2026-05-22  
**Version:** 1.0

---

## Pre-Deployment Validation

### Code Quality

- [ ] All code reviewed by at least one senior developer
- [ ] Code follows project style guidelines
- [ ] No TODO or FIXME comments left in code
- [ ] Lint checks pass (ESLint, Checkstyle)
- [ ] Code formatted correctly (Prettier, google-java-format)
- [ ] No console.log or debug statements in production code
- [ ] No hardcoded credentials or secrets in code
- [ ] No unused imports or variables

### Compilation & Build

- [ ] Backend compiles without errors: `./gradlew build`
- [ ] Frontend compiles without errors: `npm run build`
- [ ] All warnings are non-critical and documented
- [ ] Production builds are optimized (minified, tree-shaken)
- [ ] JAR file size is reasonable (~100-200 MB)
- [ ] Frontend bundle size is reasonable (~2-5 MB gzipped)
- [ ] Build artifacts are properly versioned

### Testing

- [ ] Unit tests run successfully: `./gradlew test`
- [ ] Integration tests run successfully
- [ ] No test failures or flaky tests
- [ ] Code coverage reports generated
- [ ] Coverage targets met: backend >85%, frontend >75%
- [ ] Performance tests executed and passed
- [ ] Security tests executed and passed
- [ ] Cross-browser testing completed

### Security

- [ ] Security scan completed (OWASP dependency check)
- [ ] No critical vulnerabilities found
- [ ] All dependencies up to date
- [ ] JWT token configuration reviewed
- [ ] CORS configuration verified
- [ ] SQL injection prevention validated
- [ ] XSS prevention validated
- [ ] CSRF protection enabled
- [ ] Rate limiting configured
- [ ] SSL/TLS certificates valid

### Documentation

- [ ] API documentation complete and accurate
- [ ] Frontend integration guide reviewed
- [ ] Deployment guide written and tested
- [ ] Rollback plan documented
- [ ] Change log updated
- [ ] README updated
- [ ] Configuration documented
- [ ] Architecture diagrams current

---

## Pre-Production Setup

### Database

- [ ] Database backup created and verified
  ```bash
  # Backup command
  pg_dump kronos_prod > kronos_backup_2026-05-22.sql
  ```
- [ ] Backup stored in secure location
- [ ] Restore process tested
- [ ] Database connection string verified
- [ ] Database credentials in secure vault
- [ ] Database schema migration reviewed
  - [ ] V18 migration tested on staging
  - [ ] Migration rollback script prepared
  - [ ] Data validation queries prepared

### Email Service

- [ ] SMTP server configured
  ```
  mail.host = smtp.gmail.com
  mail.port = 587
  mail.username = {secure}
  mail.password = {secure}
  mail.from = noreply@kronos-tech.com
  ```
- [ ] Email credentials in secure vault
- [ ] Test email sending works
- [ ] Email templates reviewed
- [ ] Bounce handling configured
- [ ] Rate limiting on email set (if needed)

### Environment Configuration

- [ ] Application properties configured
  ```properties
  spring.profiles.active=production
  app.mail.from=noreply@kronos-tech.com
  app.lgpd.notifications.enabled=true
  logging.level.root=INFO
  ```
- [ ] Logging configured appropriately
  - [ ] No sensitive data in logs
  - [ ] Log rotation configured
  - [ ] Log retention policy set (30 days)
- [ ] Monitoring configured
  - [ ] Metrics endpoint available
  - [ ] Health check endpoint verified
  - [ ] APM agent configured (if using)

### Load Balancer & Infrastructure

- [ ] Load balancer configured
- [ ] SSL certificates installed and valid
- [ ] Health check endpoint configured
- [ ] Auto-scaling policies set (if applicable)
- [ ] Firewall rules allowing traffic
- [ ] CDN configured (if applicable)
- [ ] DNS records prepared
- [ ] IP whitelisting configured (if needed)

---

## Database Migration

### Pre-Migration

- [ ] Migration strategy documented and reviewed
- [ ] Estimated migration time: ~5-10 seconds
- [ ] Maintenance window scheduled
- [ ] Notification sent to users about downtime
- [ ] Support team briefed
- [ ] Rollback team on standby

### Migration Execution

- [ ] Backup completed successfully
- [ ] Database backup verified
  ```bash
  # Verify backup size and integrity
  ls -lh kronos_backup_2026-05-22.sql
  ```
- [ ] Migration script reviewed one more time
  ```sql
  -- V18__add_lgpd_request_notifications.sql
  -- Creates tb_lgpd_request_notification table with indexes
  ```
- [ ] Migration executed
  ```bash
  ./gradlew flywayMigrate
  ```
- [ ] Migration status verified
  ```sql
  SELECT version, description, success FROM flyway_schema_history 
  ORDER BY version DESC LIMIT 1;
  ```
- [ ] Tables and indexes created correctly
  ```sql
  \dt tb_lgpd_request_notification
  \di idx_lgpd_notification_*
  ```
- [ ] Data integrity checked
  ```sql
  SELECT COUNT(*) FROM tb_lgpd_request_notification;
  -- Should be 0 initially
  ```

### Post-Migration

- [ ] Migration logged in change management system
- [ ] All indexes present and functional
- [ ] Query performance validated
- [ ] No data loss occurred
- [ ] Application can access new table
- [ ] Rollback plan archived

---

## Backend Deployment

### Pre-Deployment Checks

- [ ] Current version backed up
- [ ] Rollback procedure tested
- [ ] New JAR file integrity verified
  ```bash
  # Calculate and verify checksum
  sha256sum kronos-app-1.0-SNAPSHOT.jar
  ```
- [ ] Version number incremented in POM
- [ ] Build number/timestamp recorded
- [ ] Deployment notes prepared

### Deployment Steps

1. **Stop Current Service**
   ```bash
   # Stop backend service
   systemctl stop kronos-backend
   
   # Verify stopped
   systemctl status kronos-backend
   ```
   - [ ] Service stopped cleanly
   - [ ] No active connections remaining
   - [ ] Process memory released

2. **Backup Current JAR**
   ```bash
   # Backup current production JAR
   cp /opt/kronos/kronos.jar /opt/kronos/backup/kronos-backup-$(date +%Y%m%d-%H%M%S).jar
   ```
   - [ ] Backup created successfully
   - [ ] Backup location verified
   - [ ] Backup is accessible

3. **Deploy New JAR**
   ```bash
   # Copy new JAR to production directory
   cp kronos-app-1.0-SNAPSHOT.jar /opt/kronos/kronos.jar
   
   # Verify file copied
   ls -lh /opt/kronos/kronos.jar
   ```
   - [ ] New JAR in place
   - [ ] Permissions correct (644)
   - [ ] Owner/group correct

4. **Start Service**
   ```bash
   # Start backend service
   systemctl start kronos-backend
   
   # Wait for startup (30 seconds)
   sleep 30
   
   # Verify running
   systemctl status kronos-backend
   ```
   - [ ] Service started successfully
   - [ ] No startup errors
   - [ ] Service responding to requests

5. **Health Checks**
   ```bash
   # Check health endpoint
   curl -s https://api.kronos.com/actuator/health | jq .
   
   # Check application is responsive
   curl -s https://api.kronos.com/api/lgpd/admin/requests | jq .
   ```
   - [ ] Health endpoint returns UP
   - [ ] API endpoints responding
   - [ ] Database connection healthy
   - [ ] Email service accessible

### Post-Deployment Validation

- [ ] All endpoints responding (200 OK)
- [ ] New features accessible
  - [ ] Transition endpoint working
  - [ ] Complement endpoint working
  - [ ] Cancel endpoint working
- [ ] Existing endpoints still working
- [ ] Authorization checks active
- [ ] Multi-tenant isolation working
- [ ] Error handling working
- [ ] Notifications being sent
- [ ] Logs clean (no errors)
- [ ] Performance acceptable
- [ ] Database queries performant

---

## Frontend Deployment

### Pre-Deployment Checks

- [ ] Production build created
  ```bash
  npm run build
  ```
- [ ] Build artifact verified
- [ ] Asset sizes checked
- [ ] Source maps excluded from production
- [ ] Environment variables configured
  ```javascript
  // .env.production
  VITE_API_URL=https://api.kronos.com
  VITE_APP_NAME=Kronos
  ```
- [ ] CDN cache key strategy planned

### Deployment Steps

1. **Backup Current Build**
   ```bash
   # Backup current production build
   cp -r /var/www/kronos /var/www/kronos-backup-$(date +%Y%m%d-%H%M%S)
   ```
   - [ ] Backup created
   - [ ] Backup verified

2. **Deploy New Build**
   ```bash
   # Copy new build to web root
   cp -r dist/* /var/www/kronos/
   
   # Verify files copied
   ls -la /var/www/kronos/
   ```
   - [ ] Files deployed
   - [ ] Permissions correct
   - [ ] index.html present

3. **Clear Cache**
   ```bash
   # Clear CDN cache (if using CDN)
   # Clear browser cache headers (verify in response headers)
   ```
   - [ ] Cache cleared
   - [ ] New content cached

4. **Verify Deployment**
   ```bash
   # Check main page loads
   curl -s https://kronos.com | head -20
   
   # Verify API connectivity
   # Load application in browser
   ```
   - [ ] Main page loads
   - [ ] No 404 errors
   - [ ] CSS/JS files loading
   - [ ] API calls working

### Post-Deployment Validation

- [ ] Application loads without errors
- [ ] All pages accessible
- [ ] Admin workflow pages working
  - [ ] Admin LGPD Requests list loads
  - [ ] Request detail page loads
  - [ ] Transition dialog functional
  - [ ] Complement dialog functional
  - [ ] Cancel dialog functional
- [ ] Form submissions working
- [ ] API integration verified
- [ ] Error handling working
- [ ] Notifications showing
- [ ] Performance acceptable
- [ ] Cross-browser compatibility verified
  - [ ] Chrome
  - [ ] Firefox
  - [ ] Safari
  - [ ] Edge

---

## Smoke Testing

### API Endpoints

```bash
# Test transition endpoint
curl -X POST https://api.kronos.com/api/lgpd/admin/requests/{id}/transition-status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"IN_ANALYSIS","publicNotes":"Testing"}'

# Test complement endpoint
curl -X POST https://api.kronos.com/api/lgpd/admin/requests/{id}/request-complement \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"message":"Test complement"}'

# Test cancel endpoint (CTO only)
curl -X POST https://api.kronos.com/api/lgpd/admin/requests/{id}/cancel \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Test cancellation"}'
```

- [ ] All endpoints return 200 OK
- [ ] Response payloads correct
- [ ] Error responses appropriate

### Admin Workflow

1. **View Request**
   - [ ] Navigate to admin LGPD requests
   - [ ] See list of requests
   - [ ] Click on request to view details

2. **Transition Request**
   - [ ] Click "Transicionar" button
   - [ ] Select new status from dropdown
   - [ ] See appropriate fields based on status
   - [ ] Submit transition
   - [ ] See success message
   - [ ] Request status updated

3. **Request Complement**
   - [ ] Transition request to WAITING_DATA_SUBJECT
   - [ ] Click "Solicitar Complemento"
   - [ ] Enter complement message
   - [ ] Submit
   - [ ] See success message
   - [ ] Verify email sent to employee

4. **Cancel Request**
   - [ ] Click "Cancelar"
   - [ ] Enter cancellation reason
   - [ ] Submit
   - [ ] See success message
   - [ ] Request status changed to CANCELLED

### Email Notifications

- [ ] Employee receives status change email
- [ ] Email includes old and new status
- [ ] Email includes timestamp
- [ ] Rejection emails include reason
- [ ] Completion emails include notes
- [ ] Complement emails include message
- [ ] Emails formatted correctly
- [ ] Email links work

---

## Monitoring & Alerts

### Enable Monitoring

- [ ] Application metrics enabled
- [ ] Uptime monitoring configured
- [ ] Error rate monitoring configured
- [ ] Performance monitoring configured
- [ ] Database monitoring configured
- [ ] Email service monitoring configured
- [ ] Alerts configured
  - [ ] High error rate alert (>5% failure)
  - [ ] Service down alert
  - [ ] Database connection alert
  - [ ] Email service alert
  - [ ] Disk space alert
  - [ ] Memory usage alert

### Dashboard Setup

- [ ] Grafana dashboard created
- [ ] Key metrics displayed
  - [ ] Request throughput
  - [ ] Error rate
  - [ ] Response time
  - [ ] Database connections
  - [ ] Notification queue size
- [ ] Alert rules configured
- [ ] On-call rotation updated

---

## Rollback Plan

### When to Rollback

Rollback if:
- [ ] Critical errors in logs
- [ ] High error rate (>10%)
- [ ] API endpoints returning 500
- [ ] Database connectivity issues
- [ ] Performance degradation >50%
- [ ] Security issues discovered
- [ ] New features not working

### Rollback Steps

**Backend Rollback:**
```bash
# 1. Stop current service
systemctl stop kronos-backend

# 2. Restore backup JAR
cp /opt/kronos/backup/kronos-backup-2026-05-22-1000.jar /opt/kronos/kronos.jar

# 3. Revert database migration (if needed)
./gradlew flywayUndo

# 4. Start service
systemctl start kronos-backend

# 5. Verify
curl -s https://api.kronos.com/actuator/health
```

- [ ] Backup JAR restored
- [ ] Service restarted
- [ ] Service healthy
- [ ] Database schema reverted (if needed)

**Frontend Rollback:**
```bash
# 1. Restore backup build
rm -rf /var/www/kronos
mv /var/www/kronos-backup-2026-05-22-1000 /var/www/kronos

# 2. Clear cache
# Clear CDN cache manually if needed

# 3. Verify
curl -s https://kronos.com | head -20
```

- [ ] Backup restored
- [ ] Application loads
- [ ] API connectivity verified

**Database Rollback:**
```bash
# 1. Stop application
systemctl stop kronos-backend

# 2. Restore database
psql kronos_prod < kronos_backup_2026-05-22.sql

# 3. Verify data
psql kronos_prod -c "SELECT COUNT(*) FROM tb_lgpd_request_notification;"

# 4. Start application
systemctl start kronos-backend
```

- [ ] Database restored
- [ ] Data verified
- [ ] Application restarted

### Post-Rollback

- [ ] Issues documented
- [ ] Root cause analysis scheduled
- [ ] Rollback logged in change management
- [ ] Team notified
- [ ] Customers notified (if applicable)

---

## Post-Deployment Activities

### Verification (Day 1)

- [ ] Monitor logs for errors (24 hours)
- [ ] Monitor performance metrics
- [ ] Monitor user feedback
- [ ] Verify notification delivery (sample 10 emails)
- [ ] Run smoke tests again
- [ ] Check database performance
- [ ] Verify backup/restore capability

### Sign-Off

- [ ] QA sign-off obtained
- [ ] Product owner sign-off obtained
- [ ] Security team sign-off obtained
- [ ] Ops team sign-off obtained
- [ ] Deployment marked as complete in change management

### Documentation

- [ ] Deployment report created
- [ ] Any issues documented
- [ ] Performance metrics recorded
- [ ] Lessons learned documented
- [ ] Runbook updated

### Team Communication

- [ ] Deployment announcement sent to team
- [ ] Users notified of new features
- [ ] Support team briefed on new workflows
- [ ] Documentation link sent to teams
- [ ] Training scheduled (if needed)

---

## Rollback Decision Matrix

| Symptom | Severity | Action |
|---------|----------|--------|
| Single endpoint failing | Low | Monitor, fix in hotfix |
| Multiple endpoints failing | High | Rollback immediately |
| High error rate (>10%) | High | Rollback immediately |
| Database connectivity lost | Critical | Rollback database, restart app |
| Email service down | Medium | Monitor, configure fallback |
| Performance degradation >30% | High | Investigate, consider rollback |
| New workflow not accessible | High | Rollback or hotfix |
| Security vulnerability found | Critical | Rollback immediately |

---

## Deployment Timeline

Estimated time for complete deployment:

- Database migration: 5-10 minutes (downtime required)
- Backend deployment: 5-10 minutes
- Frontend deployment: 2-5 minutes
- Smoke testing: 10-15 minutes
- **Total: 30-50 minutes**

Recommended deployment window:
- **Time:** Tuesday-Thursday, 2:00 AM - 3:00 AM UTC
- **Avoid:** Fridays, weekends, before holidays
- **Notify:** Users 24 hours in advance

---

## Sign-Off

### Pre-Deployment

- [ ] QA Lead: ________________________ Date: _______
- [ ] DevOps Lead: ______________________ Date: _______
- [ ] Security Lead: ______________________ Date: _______
- [ ] Product Owner: ______________________ Date: _______

### Post-Deployment

- [ ] QA Lead: ________________________ Date: _______
- [ ] DevOps Lead: ______________________ Date: _______
- [ ] Product Owner: ______________________ Date: _______

---

## Contact & Escalation

**Deployment Lead:** [Name] - [Email] - [Phone]  
**DevOps Team:** [Channel] - [Email] - [On-call number]  
**Database Team:** [Channel] - [Email] - [On-call number]  
**Security Team:** [Channel] - [Email]  
**Support/War Room:** [Channel] - [Bridge URL]

---

**Checklist Version:** 1.0  
**Last Updated:** 2026-05-22  
**Next Review:** 2026-06-22
