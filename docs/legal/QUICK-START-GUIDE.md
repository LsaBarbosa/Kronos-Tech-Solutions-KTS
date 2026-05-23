# Sprint 7 LGPD Workflow - Quick Start Guide

**Version:** 1.0  
**Date:** 2026-05-22  
**Purpose:** Get started with LGPD request workflow management in 15 minutes

---

## What You're Getting

A complete LGPD (Brazilian Data Protection Law) request management system with:
- ✅ 9-status workflow with state machine validation
- ✅ Automatic email notifications to employees
- ✅ Admin/manager interface for managing requests
- ✅ Multi-tenant isolation and authorization
- ✅ Complete REST API with documentation
- ✅ Frontend UI with workflow dialogs

---

## Installation & Setup (5 minutes)

### Prerequisites
- Java 17+
- PostgreSQL 13+
- Node.js 18+
- Maven or Gradle

### 1. Database Setup

```bash
# Create database
createdb kronos_prod

# Apply migrations (automatically on startup)
# Or manually:
psql kronos_prod < src/main/resources/db/migration/V18__add_lgpd_request_notifications.sql
```

### 2. Environment Configuration

Backend (`application.properties`):
```properties
spring.profiles.active=production
spring.datasource.url=jdbc:postgresql://localhost:5432/kronos_prod
spring.datasource.username=kronos_user
spring.datasource.password=${DB_PASSWORD}

# Email configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
app.mail.from=noreply@kronos-tech.com
app.lgpd.notifications.enabled=true
```

Frontend (`.env`):
```
VITE_API_URL=http://localhost:8080
VITE_APP_NAME=Kronos
```

### 3. Build & Run

**Backend:**
```bash
./gradlew build
java -jar build/libs/kronos.jar
```

**Frontend:**
```bash
npm install
npm run dev
```

---

## First Steps (10 minutes)

### Access the Admin Panel

1. **Navigate to Admin Dashboard**
   - URL: `http://localhost:5173/privacy/admin/requests`
   - Role Required: MANAGER or CTO

2. **View LGPD Requests**
   - See list of all LGPD requests
   - Filter by status, type, or company
   - Click on request to view details

3. **Manage a Request**
   - Status: Shows current state (OPEN, IN_ANALYSIS, etc.)
   - Actions: Click buttons to perform workflow actions

---

## Workflow Operations

### Operation 1: Transition Status

**Scenario:** Move request from OPEN to IN_ANALYSIS

```typescript
// Step 1: Click "Transicionar Status" button
// Step 2: Select new status: "Em Análise"
// Step 3: Add notes: "Received by analysis team"
// Step 4: Click "Confirmar"
// Result: Request status updated, email sent to employee
```

**API Call:**
```bash
curl -X POST http://localhost:8080/api/lgpd/admin/requests/{id}/transition-status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "IN_ANALYSIS",
    "publicNotes": "Received by analysis team"
  }'
```

### Operation 2: Request Additional Information

**Scenario:** Request proof of residence from employee

```typescript
// Step 1: Transition request to "Aguardando Sujeito de Dados"
// Step 2: Click "Solicitar Complemento"
// Step 3: Enter message: "Please provide utility bill or lease agreement"
// Step 4: Click "Enviar"
// Result: Email sent to employee with request
```

**API Call:**
```bash
curl -X POST http://localhost:8080/api/lgpd/admin/requests/{id}/request-complement \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Please provide proof of residence"
  }'
```

### Operation 3: Complete Request

**Scenario:** Mark request as complete after data extracted

```typescript
// Step 1: Click "Transicionar Status"
// Step 2: Select status: "Concluído"
// Step 3: Add resolution notes: "Data extracted and provided to employee"
// Step 4: Click "Confirmar"
// Result: Request marked complete, notification sent
```

### Operation 4: Reject Request

**Scenario:** Reject request due to invalid documentation

```typescript
// Step 1: Click "Transicionar Status"
// Step 2: Select status: "Rejeitado"
// Step 3: REQUIRED: Enter rejection reason
// Step 4: Click "Confirmar"
// Result: Request rejected, employee notified
```

### Operation 5: Cancel Request (CTO Only)

**Scenario:** Cancel duplicate request

```typescript
// Step 1: Click "Cancelar"
// Step 2: Enter reason: "Duplicate of request #123"
// Step 3: Click "Confirmar"
// Result: Request marked as CANCELLED
// Note: Only CTO can cancel
```

---

## Common Workflows

### Complete Workflow: Access Request

```
OPEN
  ↓ Click "Transicionar" → Select "Em Análise"
IN_ANALYSIS
  ↓ Click "Transicionar" → Select "Aguardando Controlador"
WAITING_CONTROLLER
  ↓ Click "Transicionar" → Select "Aguardando Revisão Legal"
WAITING_LEGAL_REVIEW
  ↓ Click "Transicionar" → Select "Aguardando Sujeito de Dados"
WAITING_DATA_SUBJECT
  ↓ (Optional) Click "Solicitar Complemento" 
  ↓ Click "Transicionar" → Select "Concluído" (with notes)
COMPLETED ✓
```

### Reject Workflow

```
OPEN
  ↓ Click "Transicionar"
  ↓ Select "Rejeitado" 
  ↓ Enter rejection reason
IN_ANALYSIS or any status
REJECTED ✓
```

### Cancellation Workflow (CTO Only)

```
Any status except CANCELLED
  ↓ Click "Cancelar"
  ↓ Enter cancellation reason
CANCELLED ✓
```

---

## API Reference Quick

### Endpoints

| Operation | Endpoint | Method | Role |
|-----------|----------|--------|------|
| Transition Status | `/api/lgpd/admin/requests/{id}/transition-status` | POST | MANAGER, CTO |
| Request Complement | `/api/lgpd/admin/requests/{id}/request-complement` | POST | MANAGER, CTO |
| Cancel Request | `/api/lgpd/admin/requests/{id}/cancel` | POST | CTO only |

### Status Transitions

```
OPEN → IN_ANALYSIS, REJECTED, CANCELLED
IN_ANALYSIS → WAITING_CONTROLLER, REJECTED, CANCELLED
WAITING_CONTROLLER → WAITING_LEGAL_REVIEW, REJECTED, CANCELLED
WAITING_LEGAL_REVIEW → WAITING_DATA_SUBJECT, COMPLETED, PARTIALLY_COMPLETED, REJECTED, CANCELLED
WAITING_DATA_SUBJECT → IN_ANALYSIS, COMPLETED, PARTIALLY_COMPLETED, REJECTED, CANCELLED
COMPLETED → (none)
REJECTED → (none)
PARTIALLY_COMPLETED → (none)
CANCELLED → (none)
```

---

## Key Features

### Authorization

- **MANAGER:** Can manage requests in own company
- **CTO:** Can manage all requests globally
- **PARTNER:** Read-only access

### Notifications

Automatic emails sent to employees for:
1. Request created
2. Status changed
3. Responsibility assigned
4. Request completed
5. Request rejected
6. Complement requested

### Multi-Tenant Isolation

- Managers cannot see other companies' requests
- CTOs have global access
- All company checks enforced at API level

### Validation

- Rejection **requires** reason
- Completion **requires** notes
- Complement request **only** in WAITING_DATA_SUBJECT status
- Cancellation **CTO-only**

---

## Testing

### Smoke Test

```bash
# 1. Check backend is running
curl http://localhost:8080/actuator/health

# 2. Check frontend loads
curl http://localhost:5173

# 3. Test API endpoint
curl http://localhost:8080/api/lgpd/admin/requests \
  -H "Authorization: Bearer {token}"
```

### Functional Test

1. Navigate to admin LGPD requests
2. Click on a request
3. Click "Transicionar Status"
4. Select a new status
5. Add notes if required
6. Click submit
7. See success message
8. Verify email sent to employee

---

## Troubleshooting

### Issue: "Cannot find symbol" errors

**Solution:** Clean and rebuild
```bash
./gradlew clean build
```

### Issue: Database migration fails

**Solution:** Check migration script
```bash
psql kronos_prod -f src/main/resources/db/migration/V18__*.sql
psql kronos_prod -c "SELECT * FROM flyway_schema_history WHERE version='18';"
```

### Issue: Emails not sending

**Solution:** Check email configuration
```properties
# Verify settings in application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
# Check logs for SMTP errors
```

### Issue: Authorization denied

**Solution:** Verify user role
```bash
# User must have MANAGER or CTO role
# Check JWT token contains correct role
# For cancel: must be CTO
```

### Issue: Cross-company access denied

**Solution:** Ensure company context
```bash
# MANAGER can only access own company
# Check request belongs to your company
# Use CTO account for cross-company access
```

---

## Documentation Reference

For more details, see:

| Document | Purpose |
|----------|---------|
| [API-CONTRACT-LGPD-WORKFLOW.md](./API-CONTRACT-LGPD-WORKFLOW.md) | Complete API specification |
| [LGPD-FRONTEND-INTEGRATION-GUIDE.md](../Kronos-Tech-Solution-User-Plataform/docs/LGPD-FRONTEND-INTEGRATION-GUIDE.md) | Frontend implementation details |
| [SPRINT-7-FINAL-REPORT.md](./SPRINT-7-FINAL-REPORT.md) | Complete project report |
| [DEPLOYMENT-CHECKLIST.md](./DEPLOYMENT-CHECKLIST.md) | Deployment procedures |
| [SPRINT-7-PHASE5-TEST-PLAN.md](./SPRINT-7-PHASE5-TEST-PLAN.md) | Test plan and cases |

---

## Key Configuration Files

### Backend Configuration
- `application.properties` - Spring Boot configuration
- `src/main/resources/db/migration/V18__*.sql` - Database migration
- `build.gradle` - Build configuration

### Frontend Configuration
- `.env` - Environment variables
- `vite.config.ts` - Vite configuration
- `src/service/lgpd.service.ts` - API service
- `src/components/privacy/AdminLgpdRequestDetails.tsx` - Main component

---

## Next Steps

1. **Deploy to Staging**
   - Follow DEPLOYMENT-CHECKLIST.md
   - Test all workflows
   - Load test with realistic data

2. **Execute Test Plan**
   - Run unit tests
   - Run integration tests
   - Run E2E tests
   - Achieve >80% coverage

3. **User Training**
   - Train managers on workflows
   - Train support team
   - Document FAQs

4. **Production Deployment**
   - Follow deployment checklist
   - Monitor logs and metrics
   - Prepare rollback plan

5. **Post-Deployment**
   - Monitor for errors
   - Gather user feedback
   - Plan enhancements

---

## Support

**Issues?**
1. Check troubleshooting section above
2. Review relevant documentation
3. Check application logs
4. Contact DevOps team

**Questions?**
1. See "Documentation Reference" table
2. Check API contract for endpoint details
3. Review code examples in guides

**Feedback?**
- Feature requests: Submit via project management system
- Bug reports: Report with logs and reproduction steps
- Improvements: Create pull request with changes

---

## Success Checklist

After setup, you should be able to:
- [ ] Access admin LGPD requests page
- [ ] View request list filtered by status
- [ ] Click on request to view details
- [ ] Transition request status
- [ ] See transition dialog with available options
- [ ] Submit status transition
- [ ] See success message
- [ ] Request complement (WAITING_DATA_SUBJECT)
- [ ] Cancel request (CTO only)
- [ ] Verify email sent to employee
- [ ] Check database has notification record

---

**Duration:** ~15 minutes to get started  
**Next:** 30 minutes for full workflow testing  
**Production:** Follow deployment checklist (30-50 minutes)

---

**Questions?** See [SPRINT-7-FINAL-REPORT.md](./SPRINT-7-FINAL-REPORT.md) for complete details.

Last Updated: 2026-05-22  
Status: ✅ Ready for Deployment
