# Sprint 7 Pre-Implementation Analysis
## Solicitações LGPD e Atendimento Operacional (LGPD Requests and Operational Service)

**Date:** 2026-05-22  
**Status:** Analysis Phase - Ready for Implementation  
**Priority:** P1

---

## 1. Branch Confirmation

### Backend Repository
- **Path:** /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solutions-KTS
- **Current Branch:** feature/lgpd-compliance ✅
- **Latest Commit:** 4e0ca82 (Sprint 6 implementation)
- **Status:** Ready for Sprint 7

### Frontend Repository
- **Path:** /home/kronos/Documentos/Codigin/kronos/Kronos-Tech-Solution-User-Plataform
- **Current Branch:** feature/lgpd-compliance ✅
- **Latest Commit:** 737f0dd (LGPD-S01-01 biometric removal)
- **Status:** Ready for Sprint 7

---

## 2. Sprint 7 Scope

### LGPD-S07-01: Improve LGPD Request Workflow
**Priority:** P1  
**Type:** Product  
**Scope:** Backend + Frontend

**Objectives:**
1. Enhance request status workflow with new intermediate states
2. Separate public and internal notes
3. Require rejection justification
4. Allow data subject complementary requests

**Status Flow:**
```
OPEN → IN_ANALYSIS → WAITING_CONTROLLER → WAITING_LEGAL_REVIEW → WAITING_DATA_SUBJECT → COMPLETED
↓ (any point) → REJECTED
↓ (any point) → PARTIALLY_COMPLETED
↓ (any point) → CANCELLED
```

### LGPD-S07-02: Status Change Notifications
**Priority:** P1  
**Type:** Communication  
**Scope:** Backend (+ Optional Frontend)

**Objectives:**
1. Send notifications on request lifecycle events
2. Track notification delivery
3. Enable retry on failure
4. Respect tenant boundaries

**Notification Events:**
- Request created
- Responsibility assigned
- Status changed
- Request completed
- Request rejected
- SLA approaching expiration

---

## 3. Impact Matrix

### Architecture Layers Affected

| Layer | Component | Impact Level | Details |
|-------|-----------|--------------|---------|
| **Domain Model** | LgpdRequestStatus enum | HIGH | Add 6 new statuses |
| **Domain Model** | LgpdRequest record | MEDIUM | Update isTerminal() logic |
| **Database** | LgpdRequestEntity | MEDIUM | Support new statuses (no schema change) |
| **Database** | Migration | HIGH | Add notification tracking table |
| **Persistence** | LgpdRequestRepository | LOW | Query by status patterns |
| **Service Layer** | LgpdService | HIGH | New notification methods, status transitions |
| **API Layer** | LgpdController | HIGH | Validate rejection reason, note separation |
| **API DTOs** | Request/Response objects | HIGH | Include notification status fields |
| **Frontend** | Request list/detail views | HIGH | Display new statuses, notes, notifications |
| **Frontend** | Admin workflow UI | MEDIUM | Status transition controls |
| **Notifications** | New Service | HIGH | Email/internal notification system |
| **Logging** | Audit logs | MEDIUM | Track notification events |

### Cross-cutting Concerns

| Concern | Status | Details |
|---------|--------|---------|
| **Tenant Isolation** | CRITICAL | Notifications must respect companyId |
| **Authorization** | CRITICAL | Only MANAGER/CTO can change status |
| **Audit Trail** | HIGH | Every status change and notification logged |
| **Performance** | MEDIUM | Notification table indexed by requestId |
| **Reliability** | HIGH | Notification failure doesn't block request |

---

## 4. Detailed Files to be Modified/Created

### Backend Files

#### A. Domain Model & Enums
1. **LgpdRequestStatus.java** (MODIFY)
   - Add 6 new statuses: IN_ANALYSIS, WAITING_CONTROLLER, WAITING_LEGAL_REVIEW, WAITING_DATA_SUBJECT, PARTIALLY_COMPLETED, CANCELLED
   - Remove IN_PROGRESS (replace with IN_ANALYSIS)
   - Keep: OPEN, COMPLETED, REJECTED

2. **LgpdRequest.java** (MODIFY)
   - Update isTerminal() to include CANCELLED, PARTIALLY_COMPLETED
   - Consider adding reasonForRejection field validation

#### B. Persistence Layer - Entities
3. **LgpdRequestEntity.java** (NO CHANGE)
   - Already has closedReason, publicResolutionNotes, internalNotes
   - Status enum handles new values automatically

4. **LgpdRequestNotificationEntity.java** (CREATE)
   - New table to track notification attempts
   - Fields:
     - notificationId (UUID, PK)
     - requestId (UUID, FK)
     - notificationType (enum: REQUEST_CREATED, STATUS_CHANGED, COMPLETION, REJECTION, SLA_WARNING)
     - recipientUserId (UUID)
     - notificationChannel (EMAIL, INTERNAL)
     - sentAt (Instant)
     - status (PENDING, SENT, FAILED)
     - failureReason (text)
     - retryCount (int)
     - nextRetryAt (Instant)
     - createdAt (Instant)

#### C. Migrations
5. **V18__add_lgpd_request_notifications.sql** (CREATE)
   - Create tb_lgpd_request_notification table
   - Create indexes: idx_notification_request, idx_notification_status
   - Seed notification configuration (retention policy reference)

#### D. Repositories
6. **LgpdRequestNotificationRepository.java** (CREATE)
   - Interface extending JpaRepository
   - Custom methods:
     - findPendingNotifications()
     - findFailedNotifications()
     - countByRequestIdAndType()

7. **LgpdRequestRepository.java** (MODIFY)
   - May need custom query for status transitions
   - Current version likely sufficient (no changes needed)

#### E. Service Layer
8. **LgpdService.java** (MODIFY - HIGH IMPACT)
   - Add method: transitionStatus(requestId, newStatus, changedByUserId, notes)
   - Modify: completeRequest() - require publicResolutionNotes
   - Modify: rejectRequest() - require closedReason (already present)
   - Add method: requestDataSubjectComplement(requestId, complementRequest)
   - Add method: cancelRequest(requestId, reason)
   - Enhance: assignRequest() - send notification
   - Enhance: Create request - send notification
   - Add authorization checks for all status transitions

9. **LgpdRequestNotificationService.java** (CREATE)
   - New service to handle notification dispatch
   - Methods:
     - sendRequestCreatedNotification(request)
     - sendStatusChangeNotification(request, oldStatus)
     - sendCompletionNotification(request)
     - sendRejectionNotification(request)
     - sendSlaWarningNotification(request)
     - retryFailedNotifications()
   - Internal: buildEmailContent(), sendEmail(), logNotification()

10. **NotificationProvider.java** (CREATE - Port interface)
    - Port for sending notifications (abstraction)
    - Methods: sendEmail(), sendInternalMessage()

11. **EmailNotificationProviderImpl.java** (CREATE)
    - Implementation for email notifications
    - Use existing email infrastructure (SendGrid, etc.)
    - Respect configuration flags

12. **InternalNotificationProviderImpl.java** (CREATE)
    - Implementation for internal notifications
    - Store in message/notification inbox

#### F. API Layer - Controllers
13. **LgpdController.java** (MODIFY - HIGH IMPACT)
    - POST /lgpd/admin/requests/{requestId}/transition-status
      - Parameters: newStatus, notes (public), internalNotes
      - Validation: closedReason required if REJECTED
      - Authorization: MANAGER (own company) or CTO
    - POST /lgpd/admin/requests/{requestId}/request-complement
      - Allow requesting data from subject
      - Send notification
    - POST /lgpd/admin/requests/{requestId}/cancel
      - Cancel request with reason
      - Authorization: CTO or assigned manager
    - GET /lgpd/admin/requests?status=... (filter by new statuses)
    - GET /employee/me/lgpd-requests/{requestId}
      - Return only publicResolutionNotes to titleholder
      - Hide internalNotes

#### G. API DTOs
14. **LgpdRequestTransitionRequest.java** (CREATE)
    ```java
    newStatus: LgpdRequestStatus
    publicNotes: String
    internalNotes: String
    closedReason: String (optional, required if newStatus=REJECTED)
    ```

15. **LgpdRequestDetailsResponse.java** (MODIFY)
    - Add: notificationStatus (PENDING, SENT, FAILED)
    - Add: publicResolutionNotes (always included)
    - Add: internalNotes (only for MANAGER/CTO)
    - Conditional: closedReason (only if status=REJECTED)
    - Add: availableTransitions (array of allowed next statuses)

16. **RequestComplementRequest.java** (CREATE)
    ```java
    message: String (what additional data/clarification needed)
    dueAt: Instant (deadline for response)
    ```

#### H. Mappers
17. **LgpdRequestMapper.java** (MODIFY)
    - Add notification fields to domain mapping
    - Handle status enum transformations

#### I. Tests
18. **LgpdServiceTest.java** (MODIFY)
    - Add test: status transition validates state machine
    - Add test: rejection requires reason
    - Add test: completion requires public notes
    - Add test: multi-tenant isolation on transitions
    - Add test: authorization checks on transitions

19. **LgpdRequestNotificationServiceTest.java** (CREATE)
    - Test: each notification type sends correctly
    - Test: notification failure doesn't break request
    - Test: retry mechanism works
    - Test: notifications respect tenant boundaries
    - Test: notification content is correct

20. **LgpdControllerWebMvcTest.java** (MODIFY)
    - Add test: transition-status endpoint
    - Add test: request-complement endpoint
    - Add test: cancel endpoint
    - Add test: status filtering

### Frontend Files

#### A. Services
21. **inventory.service.ts** (MODIFY)
    - Add method: updateRequestStatus(requestId, newStatus, notes)
    - Add method: requestDataSubjectComplement(requestId, message)
    - Add method: cancelRequest(requestId, reason)
    - Add method: getAvailableTransitions(currentStatus)

22. **notification.service.ts** (CREATE)
    - Handle in-app notifications for status changes
    - Display to user when status changes

#### B. Components
23. **LgpdRequestListComponent** (MODIFY)
    - Filter by status (use new enum values)
    - Display status badges with new statuses
    - Show notification indicator (email sent, pending, failed)

24. **LgpdRequestDetailComponent** (MODIFY)
    - Display public vs internal notes (conditionally)
    - Show status transition controls (only for authorized users)
    - Display rejection reason (if applicable)
    - Show notification status/history

25. **LgpdRequestWorkflowComponent** (CREATE)
    - New component for status transitions
    - Status machine visualization
    - Form for status change with required fields
    - Validation: closedReason required for REJECTION
    - Validation: publicNotes required for COMPLETION

26. **RequestComplementFormComponent** (CREATE)
    - Form to request additional data from subject
    - Message field, optional due date
    - Send button triggers notification

#### C. Type Definitions
27. **lgpd.types.ts** (MODIFY)
    - Update LgpdRequestStatus enum
    - Add LgpdRequestNotification interface
    - Add RequestStatusTransition interface
    - Update LgpdRequest interface

#### D. Configuration
28. **api-routes.ts** (MODIFY)
    - Add: TRANSITION_STATUS: `admin/requests/{requestId}/transition-status`
    - Add: REQUEST_COMPLEMENT: `admin/requests/{requestId}/request-complement`
    - Add: CANCEL_REQUEST: `admin/requests/{requestId}/cancel`

29. **api.ts** (MODIFY - if needed)
    - No changes likely (standard HTTP methods)

#### E. Tests
30. **lgpd-request-list.component.spec.ts** (MODIFY)
    - Add test: filter by new statuses
    - Add test: notification indicator displays

31. **lgpd-request-detail.component.spec.ts** (MODIFY)
    - Add test: internal notes hidden from non-authorized users
    - Add test: rejection reason displays correctly
    - Add test: status transitions available based on current status

---

## 5. Database Migration Plan

### V18__add_lgpd_request_notifications.sql

```sql
-- Create notification tracking table
CREATE TABLE tb_lgpd_request_notification (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES tb_lgpd_request(request_id),
    notification_type VARCHAR(50) NOT NULL,  -- REQUEST_CREATED, STATUS_CHANGED, etc.
    recipient_user_id UUID NOT NULL,
    notification_channel VARCHAR(20) NOT NULL,  -- EMAIL, INTERNAL
    sent_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, SENT, FAILED
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lgpd_notification_request 
    ON tb_lgpd_request_notification(request_id);
CREATE INDEX idx_lgpd_notification_status 
    ON tb_lgpd_request_notification(status)
    WHERE status = 'PENDING';
CREATE INDEX idx_lgpd_notification_recipient 
    ON tb_lgpd_request_notification(recipient_user_id);
```

### Status Enum Handling
- No migration needed for status enum values (PostgreSQL ENUM type handles dynamically)
- OR: Create trigger to validate status transitions

---

## 6. Configuration Changes

### Backend (application.properties / application-prod.properties)

```properties
# LGPD Notification Configuration
app.lgpd.notifications.enabled=true
app.lgpd.notifications.email-provider=sendgrid
app.lgpd.notifications.retry-attempts=3
app.lgpd.notifications.retry-delay-seconds=300
app.lgpd.notifications.sla-warning-days=3

# Notification Templates
app.lgpd.notifications.template-path=classpath:templates/lgpd-notifications/
```

---

## 7. Authorization & Validation Rules

### Status Transition Matrix

| Current Status | Next Status | Required Role | Required Notes | Validation |
|---|---|---|---|---|
| OPEN | IN_ANALYSIS | MANAGER/CTO | (none) | Auto-assign if not assigned |
| IN_ANALYSIS | WAITING_CONTROLLER | MANAGER/CTO | (none) | (none) |
| WAITING_CONTROLLER | WAITING_LEGAL_REVIEW | CTO | (none) | (none) |
| WAITING_LEGAL_REVIEW | WAITING_DATA_SUBJECT | CTO | (none) | (none) |
| WAITING_DATA_SUBJECT | IN_ANALYSIS | MANAGER | Complement received | (none) |
| ANY | COMPLETED | MANAGER/CTO | publicNotes | Must have result data |
| ANY | REJECTED | MANAGER/CTO | closedReason | Reason mandatory |
| ANY | PARTIALLY_COMPLETED | CTO | publicNotes | Partial result explanation |
| ANY | CANCELLED | CTO | internalNotes | Cancellation reason |

### Multi-Tenant Validation

```java
// Before any status transition
DomainAuthorizationService.authorizeCompanyAccess(
    manager.companyId,
    request.companyId,
    "Cannot transition LGPD request from different company"
);
```

### Data Validation

| Field | Validation |
|-------|-----------|
| closedReason | Required when status=REJECTED, max 255 chars |
| publicResolutionNotes | Required when status=COMPLETED/PARTIALLY_COMPLETED, max 5000 chars |
| internalNotes | Optional, max 5000 chars |
| nextStatus | Must be valid transition from current status |

---

## 8. Logging & Audit Requirements

### Events to Log (via AuditLog)

- Status transition attempt (with new status)
- Status transition success (with timestamp)
- Status transition failure (with reason)
- Notification sent (with type and recipient)
- Notification failed (with failure reason)
- Data subject complement requested
- Request cancelled

### Sensitive Data Restrictions

- Do NOT log: publicResolutionNotes content (too large)
- Do NOT log: internalNotes content (too large)
- DO log: status change event type
- DO log: who initiated the change
- DO log: timestamp and result

---

## 9. Test Coverage Strategy

### Unit Tests (Backend)

- [ ] LgpdRequestStatus enum - all values defined
- [ ] LgpdRequest.isTerminal() - covers all terminal states
- [ ] Status transition validation - each transition rule
- [ ] Rejection without reason - blocked
- [ ] Completion without notes - blocked
- [ ] Multi-tenant isolation - query filters by companyId
- [ ] Notification record creation - for each event type

### Integration Tests (Backend)

- [ ] Complete request lifecycle: OPEN → IN_ANALYSIS → ... → COMPLETED
- [ ] Rejected path: OPEN → REJECTED with reason
- [ ] Cancelled path: IN_ANALYSIS → CANCELLED
- [ ] Data subject complement: request → provide → resume
- [ ] Notification delivery - mocked service
- [ ] Multi-tenant - manager cannot transition other company requests
- [ ] Authorization - unauthorized users get 403/404

### Frontend Tests

- [ ] Status enum values map correctly
- [ ] Filter by status works for all new statuses
- [ ] Form validation - closedReason required on rejection
- [ ] Form validation - publicNotes required on completion
- [ ] Status transition dropdown shows allowed next statuses only
- [ ] Internal notes hidden from non-authorized views
- [ ] Notification indicator displays correctly

### E2E Tests

- [ ] Manager creates request, system auto-sends notification
- [ ] Manager transitions to IN_ANALYSIS, notification sent
- [ ] CTO completes with public notes visible to data subject
- [ ] Rejection with mandatory reason - form blocks submission without reason
- [ ] Data subject sees only public notes when checking status
- [ ] Data subject can request complement, notification received

---

## 10. Implementation Sequence

### Phase 1: Domain & Database (1-2 days)
1. Update LgpdRequestStatus enum
2. Update LgpdRequest.isTerminal() logic
3. Create migration V18 for notification table
4. Create LgpdRequestNotificationEntity

### Phase 2: Service Layer (2-3 days)
5. Create LgpdRequestNotificationService
6. Create notification provider interfaces/implementations
7. Modify LgpdService with new transition methods
8. Add authorization checks

### Phase 3: API Layer (1-2 days)
9. Create DTOs (transition request, complement request)
10. Modify LgpdController with new endpoints
11. Update response DTOs with notification fields

### Phase 4: Frontend (2-3 days)
12. Update services and type definitions
13. Create workflow component
14. Modify list and detail components
15. Add filters and status displays

### Phase 5: Testing (2-3 days)
16. Write backend tests
17. Write frontend tests
18. E2E testing

### Phase 6: Documentation (1 day)
19. Update API contract
20. Create Sprint 7 completion report

---

## 11. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Notification failure blocks request | Low | High | Async with retry queue, don't block main transaction |
| Status mismatch between front/back | Medium | High | Validation on all transitions, status machine tests |
| Multi-tenant leak in notifications | Low | Critical | Explicit companyId check before sending |
| Email service unavailable | Medium | Medium | Graceful degradation, local notifications as fallback |
| Performance regression from notifications | Low | Medium | Async processing, background job scheduler |
| Missing test coverage on transitions | Medium | High | Mandatory test matrix before completion |

---

## 12. Success Criteria

### Functional
- [ ] All 9 statuses fully implemented and testable
- [ ] Status transitions follow business rules
- [ ] Rejection requires reason
- [ ] Completion requires public notes
- [ ] Data subject can request complement
- [ ] All transitions properly logged
- [ ] Notifications sent without blocking requests
- [ ] Notification retry works on failure

### Technical
- [ ] Zero regression in existing tests
- [ ] All new tests passing (unit + integration + E2E)
- [ ] Multi-tenant isolation verified
- [ ] Authorization checks in all endpoints
- [ ] No sensitive data in logs
- [ ] Code follows hexagonal architecture
- [ ] Migration runs successfully

### Performance
- [ ] Notification query completes in <100ms
- [ ] Status transition completes in <500ms
- [ ] No blocking on email send

### Security
- [ ] Tenant boundary enforced
- [ ] Authorization validated on every action
- [ ] Sensitive fields properly masked
- [ ] CSRF token validated on state changes

---

**Analysis Complete - Ready for Full Implementation**

Next Phase: Detailed implementation of all 6 phases following this analysis.
