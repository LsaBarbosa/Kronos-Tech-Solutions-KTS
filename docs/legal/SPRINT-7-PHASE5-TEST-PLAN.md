# Sprint 7 Phase 5: Testing Plan
## LGPD Request Workflow and Notification System

**Status:** IN PROGRESS  
**Date:** 2026-05-22  
**Phase:** 5/6 - Testing (Unit, Integration, E2E)

---

## Overview

Phase 5 comprehensive testing for all backend services, API endpoints, and frontend components implemented in Phases 1-4. Testing covers:
- Unit tests for service layer methods
- Unit tests for notification service and providers
- Integration tests for REST endpoints  
- E2E tests for complete workflows
- Authorization and security validation
- Multi-tenant isolation verification

---

## Completed Test Infrastructure

### Files Created
1. **LgpdServiceTransitionTest.java** ✅
   - Tests status transition state machine
   - Tests validation rules for rejection (requires closedReason)
   - Tests validation rules for completion (requires publicNotes)
   - Tests CTO-only cancellation enforcement
   - Tests complement request state validation
   - Tests getAvailableTransitions() for all statuses

2. **Fixed Existing Test**
   - LgpdControllerWebMvcTest.java - Fixed IN_PROGRESS → IN_ANALYSIS reference

---

## Unit Test Requirements (BACKEND)

### 1. LgpdService Status Transition Tests

**File:** `LgpdServiceTransitionTest.java`

**Test Cases:**

#### Status Transition Matrix
- [ ] OPEN → IN_ANALYSIS (valid)
- [ ] OPEN → REJECTED (requires closedReason)
- [ ] OPEN → CANCELLED (CTO-only)
- [ ] IN_ANALYSIS → WAITING_CONTROLLER (valid)
- [ ] IN_ANALYSIS → REJECTED (requires closedReason)
- [ ] WAITING_CONTROLLER → WAITING_LEGAL_REVIEW (valid)
- [ ] WAITING_LEGAL_REVIEW → WAITING_DATA_SUBJECT (valid)
- [ ] WAITING_LEGAL_REVIEW → COMPLETED (requires publicNotes)
- [ ] WAITING_LEGAL_REVIEW → PARTIALLY_COMPLETED (requires publicNotes)
- [ ] WAITING_DATA_SUBJECT → IN_ANALYSIS (valid, for complement)
- [ ] WAITING_DATA_SUBJECT → COMPLETED (requires publicNotes)
- [ ] WAITING_DATA_SUBJECT → PARTIALLY_COMPLETED (requires publicNotes)

#### Validation Tests
- [ ] Rejection without closedReason throws IllegalArgumentException
- [ ] Rejection with closedReason succeeds
- [ ] Completion without publicNotes throws IllegalArgumentException
- [ ] Completion with publicNotes succeeds
- [ ] Partial completion without publicNotes throws IllegalArgumentException
- [ ] Partial completion with publicNotes succeeds

#### Authorization Tests
- [ ] Manager can transition own company requests
- [ ] Manager cannot transition cross-company requests
- [ ] CTO can cancel requests
- [ ] Manager cannot cancel requests
- [ ] Unauthorized access throws exception

#### Notification Tests
- [ ] Status change triggers notifyStatusChanged()
- [ ] Rejection triggers notifyRejectionRequest()
- [ ] Completion triggers notifyCompletionRequest()
- [ ] Complement request triggers notifyComplementRequest()

### 2. LgpdService Complement Request Tests

**File:** `LgpdServiceComplementTest.java`

**Test Cases:**
- [ ] Complement request only succeeds in WAITING_DATA_SUBJECT status
- [ ] Complement request in OPEN status throws IllegalStateException
- [ ] Complement request in COMPLETED status throws IllegalStateException
- [ ] Notification is sent when complement requested
- [ ] Returns the updated request

### 3. LgpdService Cancellation Tests

**File:** `LgpdServiceCancellationTest.java`

**Test Cases:**
- [ ] CTO can cancel request
- [ ] Manager cannot cancel request
- [ ] Partner cannot cancel request
- [ ] Cancellation requires reason
- [ ] Cancellation saves to history
- [ ] Terminal status prevents cancellation

### 4. State Machine Helper Tests

**File:** `LgpdServiceStateTransitionHelperTest.java`

**Test Cases:**
- [ ] getAvailableTransitions(OPEN) returns [IN_ANALYSIS, REJECTED, CANCELLED]
- [ ] getAvailableTransitions(IN_ANALYSIS) returns [WAITING_CONTROLLER, REJECTED, CANCELLED]
- [ ] getAvailableTransitions(WAITING_CONTROLLER) returns [WAITING_LEGAL_REVIEW, REJECTED, CANCELLED]
- [ ] getAvailableTransitions(WAITING_LEGAL_REVIEW) returns [WAITING_DATA_SUBJECT, COMPLETED, PARTIALLY_COMPLETED, REJECTED, CANCELLED]
- [ ] getAvailableTransitions(WAITING_DATA_SUBJECT) returns [IN_ANALYSIS, COMPLETED, PARTIALLY_COMPLETED, REJECTED, CANCELLED]
- [ ] getAvailableTransitions(COMPLETED) returns []
- [ ] getAvailableTransitions(REJECTED) returns []
- [ ] getAvailableTransitions(PARTIALLY_COMPLETED) returns []
- [ ] getAvailableTransitions(CANCELLED) returns []

### 5. LgpdRequestNotificationService Tests

**File:** `LgpdRequestNotificationServiceTest.java`

**Test Cases:**

#### Notification Dispatch Tests
- [ ] notifyRequestCreated() sends email to employee
- [ ] notifyStatusChanged() includes old and new status
- [ ] notifyResponsibilityAssigned() sends to assigned user
- [ ] notifyCompletionRequest() includes completion notes
- [ ] notifyRejectionRequest() includes rejection reason
- [ ] notifyComplementRequest() includes complement message

#### Employee Lookup Tests
- [ ] Skips notification if employee not found
- [ ] Logs warning when employee not found
- [ ] Uses employee email for notifications

#### Error Handling Tests
- [ ] Gracefully handles notification provider failures
- [ ] Marks notification as FAILED when exception thrown
- [ ] Stores failure reason for retry
- [ ] Sets next retry time with exponential backoff

#### Retry Logic Tests
- [ ] retryFailedNotifications() reschedules failed notifications
- [ ] Retry count increments with each attempt
- [ ] Status changes to PENDING for retry
- [ ] Marks as FAILED after 3 max attempts
- [ ] Stores "Max retry attempts exceeded" reason

#### Notification Persistence Tests
- [ ] Saves notification entity to repository
- [ ] Sets correct notification type
- [ ] Sets notification channel (EMAIL/INTERNAL)
- [ ] Records sent timestamp on success
- [ ] Records failure reason on failure

### 6. NotificationProvider Tests

**File:** `EmailNotificationProviderImplTest.java`

**Test Cases:**

#### Email Sending Tests
- [ ] Sends MIME multipart email successfully
- [ ] Sets From address correctly
- [ ] Sets To address correctly
- [ ] Sets Subject correctly
- [ ] Sends both plain text and HTML content
- [ ] Uses UTF-8 encoding

#### Configuration Tests
- [ ] Respects notifications.enabled = true
- [ ] Skips sending when notifications.enabled = false
- [ ] Uses configurable mail.from address
- [ ] Defaults to noreply@kronos-tech.com

#### Error Handling Tests
- [ ] Throws NotificationException on MessagingException
- [ ] Includes recipient email in exception message
- [ ] Includes subject in exception message

#### Method Tests
- [ ] sendEmailNotification() sends email
- [ ] sendInternalNotification() throws exception (not implemented in Email provider)

**File:** `InternalNotificationProviderImplTest.java`

**Test Cases:**
- [ ] Logs internal notifications
- [ ] Accepts notification type and reference ID
- [ ] Returns successfully (stub implementation)

---

## Integration Test Requirements (BACKEND)

### 1. LGPD Controller Status Transition Endpoint Tests

**File:** `LgpdControllerStatusTransitionIntegrationTest.java`

**Endpoint:** `POST /api/lgpd/admin/requests/{requestId}/transition-status`

**Test Cases:**

#### Happy Path Tests
- [ ] MANAGER can transition OPEN → IN_ANALYSIS
- [ ] MANAGER can transition IN_ANALYSIS → WAITING_CONTROLLER
- [ ] MANAGER can transition WAITING_LEGAL_REVIEW → WAITING_DATA_SUBJECT
- [ ] MANAGER can reject with closedReason
- [ ] MANAGER can complete with publicNotes
- [ ] CTO can perform all transitions

#### Validation Tests
- [ ] Returns 400 when newStatus is missing
- [ ] Returns 400 when rejecting without closedReason
- [ ] Returns 400 when completing without publicNotes
- [ ] Returns 400 for invalid status value

#### Authorization Tests
- [ ] Returns 403 when user lacks MANAGER/CTO role
- [ ] Returns 404 for non-existent request ID
- [ ] Returns 403 when MANAGER accesses cross-company request
- [ ] Returns 200 when CTO accesses any company request

#### Response Validation Tests
- [ ] Returns updated LgpdRequestResponse
- [ ] Status field reflects new status
- [ ] Updated timestamp is current
- [ ] Notes are included in response

### 2. LGPD Controller Complement Request Endpoint Tests

**File:** `LgpdControllerComplementIntegrationTest.java`

**Endpoint:** `POST /api/lgpd/admin/requests/{requestId}/request-complement`

**Test Cases:**
- [ ] MANAGER can request complement
- [ ] Request must be in WAITING_DATA_SUBJECT status
- [ ] Returns 400 if message is empty
- [ ] Returns 400 if message exceeds max length
- [ ] Returns 400 if status is not WAITING_DATA_SUBJECT
- [ ] Sends notification to employee
- [ ] Returns updated request

### 3. LGPD Controller Cancel Endpoint Tests

**File:** `LgpdControllerCancelIntegrationTest.java`

**Endpoint:** `POST /api/lgpd/admin/requests/{requestId}/cancel`

**Test Cases:**
- [ ] CTO can cancel request
- [ ] Returns 403 when MANAGER attempts cancel
- [ ] Returns 403 when Partner attempts cancel
- [ ] Returns 400 when reason is empty
- [ ] Returns 400 when reason exceeds max length
- [ ] Returns 200 with CANCELLED status
- [ ] Saves cancellation reason

### 4. Multi-tenant Isolation Tests

**File:** `LgpdControllerMultiTenantTest.java`

**Test Cases:**
- [ ] MANAGER A cannot transition MANAGER B's requests
- [ ] MANAGER A cannot see COMPANY B's requests
- [ ] CTO can see all companies
- [ ] Cross-company request returns 403
- [ ] Filtering by companyId works correctly

### 5. Authentication/Authorization Tests

**File:** `LgpdControllerSecurityTest.java`

**Test Cases:**
- [ ] Unauthenticated requests return 401
- [ ] Invalid JWT token returns 401
- [ ] Expired JWT token returns 401
- [ ] User with wrong role returns 403
- [ ] User with correct role returns 200

---

## E2E Test Requirements

### 1. Complete Request Lifecycle E2E Test

**File:** `LgpdRequestCompleteLifecycleE2ETest.java`

**Scenario:** Access Request from OPEN to COMPLETED

```
OPEN 
  ↓ (transition to IN_ANALYSIS)
IN_ANALYSIS 
  ↓ (transition to WAITING_CONTROLLER)
WAITING_CONTROLLER 
  ↓ (transition to WAITING_LEGAL_REVIEW)
WAITING_LEGAL_REVIEW 
  ↓ (transition to WAITING_DATA_SUBJECT)
WAITING_DATA_SUBJECT 
  ├─ Request complement from employee
  ├─ Verify employee receives notification
  ├─ (transition to IN_ANALYSIS or stay)
  └─ (transition to COMPLETED with notes)
COMPLETED
```

**Validation:**
- [ ] Each transition is persisted
- [ ] History entries created for each transition
- [ ] Notifications sent at each step
- [ ] Employee receives status updates
- [ ] Final status is COMPLETED

### 2. Rejection Workflow E2E Test

**File:** `LgpdRequestRejectionWorkflowE2ETest.java`

**Scenario:** Request rejected from IN_ANALYSIS

```
OPEN 
  ↓ (transition to IN_ANALYSIS)
IN_ANALYSIS 
  ↓ (reject with reason)
REJECTED
```

**Validation:**
- [ ] Rejection reason is stored
- [ ] Employee receives rejection notification
- [ ] Notification includes rejection reason
- [ ] Public and internal notes are tracked

### 3. Cancellation Workflow E2E Test

**File:** `LgpdRequestCancellationWorkflowE2ETest.java`

**Scenario:** CTO cancels request

```
OPEN/IN_ANALYSIS/...
  ↓ (CTO cancels)
CANCELLED
```

**Validation:**
- [ ] Only CTO can cancel
- [ ] Cancellation reason is stored
- [ ] Request transitions to CANCELLED
- [ ] History entry shows cancellation

### 4. Data Subject Complement Workflow E2E Test

**File:** `LgpdRequestComplementWorkflowE2ETest.java`

**Scenario:** Request complement from data subject

```
WAITING_DATA_SUBJECT
  ↓ (request complement)
WAITING_DATA_SUBJECT (with notification)
  ↓ (employee provides info)
  ↓ (transition to IN_ANALYSIS for re-evaluation or COMPLETED)
IN_ANALYSIS or COMPLETED
```

**Validation:**
- [ ] Can only request in WAITING_DATA_SUBJECT
- [ ] Employee receives complement request notification
- [ ] Notification includes message requesting info
- [ ] Transition occurs after employee provides info

### 5. Notification Delivery E2E Test

**File:** `LgpdRequestNotificationDeliveryE2ETest.java`

**Scenario:** Verify all notifications are sent

**Coverage:**
- [ ] REQUEST_CREATED - sent when created
- [ ] STATUS_CHANGED - sent on any transition
- [ ] RESPONSIBILITY_ASSIGNED - sent when assigned
- [ ] REQUEST_COMPLETED - sent on completion
- [ ] REQUEST_REJECTED - sent on rejection
- [ ] COMPLEMENT_REQUESTED - sent when complement requested

**Validation:**
- [ ] Notification saved to database
- [ ] Notification marked as SENT
- [ ] Email delivered to correct recipient
- [ ] Notification content is correct

---

## Frontend Test Requirements

### 1. Component Unit Tests

**File:** `AdminLgpdRequestDetails.spec.ts`

**Test Cases:**
- [ ] Displays all 9 status colors correctly
- [ ] Shows correct status labels in Portuguese
- [ ] Transition dialog shows available transitions
- [ ] Rejection requires closedReason
- [ ] Completion requires notes
- [ ] Complement button only shown for WAITING_DATA_SUBJECT
- [ ] Cancel button visible for non-terminal statuses
- [ ] Calls service functions correctly

### 2. Service Layer Tests

**File:** `lgpd.service.spec.ts`

**Test Cases:**
- [ ] transitionRequestStatus() calls correct endpoint
- [ ] requestComplementFromDataSubject() calls correct endpoint
- [ ] cancelLgpdRequest() calls correct endpoint
- [ ] getAvailableTransitions() returns correct statuses
- [ ] Error responses are handled

### 3. Integration Tests

**File:** `LgpdAdminWorkflow.spec.ts`

**Test Cases:**
- [ ] User can view request details
- [ ] User can select transition status
- [ ] User can fill in required notes
- [ ] User can submit transition
- [ ] User sees confirmation message
- [ ] Request detail updates after transition

---

## Test Metrics & Coverage Goals

| Metric | Target | Status |
|--------|--------|--------|
| Unit Test Coverage (Services) | >85% | ⏳ IN PROGRESS |
| Unit Test Coverage (Controllers) | >80% | ⏳ IN PROGRESS |
| Integration Test Coverage | >70% | ⏳ IN PROGRESS |
| E2E Test Coverage | >60% | ⏳ IN PROGRESS |
| Authorization Tests | 100% | ⏳ IN PROGRESS |
| Multi-tenant Tests | 100% | ⏳ IN PROGRESS |
| Notification Tests | 100% | ⏳ IN PROGRESS |
| Total Test Count (Backend) | 150+ | ⏳ IN PROGRESS |
| Total Test Count (Frontend) | 50+ | ⏳ IN PROGRESS |

---

## Test Data Requirements

### LGPD Requests Test Data

```java
// Sample test request
UUID requestId = UUID.randomUUID();
UUID employeeId = UUID.randomUUID();
UUID companyId = UUID.randomUUID();

LgpdRequest request = new LgpdRequest(
    requestId,
    employeeId,
    userId,
    companyId,
    LgpdRequestType.ACCESS,
    LgpdRequestStatus.OPEN,
    "Test LGPD Request",
    null,
    Instant.now(),
    Instant.now(),
    null,
    null,
    null,
    Instant.now().plusSeconds(86400),
    "HIGH",
    null,
    null,
    null
);
```

### Employee Test Data

```java
// Sample test employee
Employee employee = new Employee(
    employeeId,
    companyId,
    "João Silva",
    "12345678900",
    "joao.silva@test.com",
    "Senior Engineer",
    true,
    Instant.now(),
    Instant.now()
);
```

### User Test Data

```java
// Sample test user
User user = new User(
    UUID.randomUUID(),
    "manager",
    "Manager User",
    Role.MANAGER,
    true,
    companyId
);
```

---

## Known Limitations & Dependencies

| Item | Impact | Status |
|------|--------|--------|
| Email service not fully configured | Cannot test actual email delivery | ⚠️ Test with mocks |
| Internal notifications stub only | Cannot test inbox delivery | ℹ️ Use stub validation |
| No SMS support | Limited notification channels | ℹ️ Test EMAIL/INTERNAL only |
| JWT configuration in tests | May need mock resolver | ⚠️ Use @WithMockUser |
| Database transaction isolation | Transaction boundaries may affect tests | ℹ️ Use @Transactional |

---

## Execution Plan

### Week 1 (Current)
- [ ] Complete unit tests for LgpdService transitions (3-4 hours)
- [ ] Complete unit tests for notification service (2-3 hours)
- [ ] Complete notification provider tests (1-2 hours)
- [ ] Fix/adapt controller integration tests (2-3 hours)

### Week 2
- [ ] Complete multi-tenant isolation tests (2-3 hours)
- [ ] Complete authorization/security tests (2-3 hours)
- [ ] Create E2E workflow tests (4-5 hours)
- [ ] Create frontend component tests (3-4 hours)

### Week 3
- [ ] Execute full test suite
- [ ] Achieve >80% code coverage
- [ ] Document test results
- [ ] Fix any failing tests

---

## Success Criteria

- [x] All unit tests compile without errors
- [ ] All unit tests pass (backend)
- [ ] All integration tests pass
- [ ] All E2E tests pass
- [ ] Code coverage >80% (backend)
- [ ] Code coverage >75% (frontend)
- [ ] All authorization tests pass
- [ ] All multi-tenant isolation tests pass
- [ ] Notification delivery verified
- [ ] No security vulnerabilities detected

---

## Files Requiring Tests

**Backend Test Files to Create:**
1. `LgpdServiceTransitionTest.java` ✅ Created
2. `LgpdServiceComplementTest.java` - Create
3. `LgpdServiceCancellationTest.java` - Create
4. `LgpdServiceStateTransitionHelperTest.java` - Create
5. `LgpdRequestNotificationServiceTest.java` - Create
6. `EmailNotificationProviderImplTest.java` - Create
7. `InternalNotificationProviderImplTest.java` - Create
8. `LgpdControllerStatusTransitionIntegrationTest.java` - Create
9. `LgpdControllerComplementIntegrationTest.java` - Create
10. `LgpdControllerCancelIntegrationTest.java` - Create
11. `LgpdControllerMultiTenantTest.java` - Create
12. `LgpdControllerSecurityTest.java` - Create
13. `LgpdRequestCompleteLifecycleE2ETest.java` - Create
14. `LgpdRequestRejectionWorkflowE2ETest.java` - Create
15. `LgpdRequestCancellationWorkflowE2ETest.java` - Create
16. `LgpdRequestComplementWorkflowE2ETest.java` - Create
17. `LgpdRequestNotificationDeliveryE2ETest.java` - Create

**Frontend Test Files to Create:**
1. `AdminLgpdRequestDetails.spec.ts` - Create
2. `lgpd.service.spec.ts` - Create
3. `LgpdAdminWorkflow.spec.ts` - Create

---

## Phase 5 Completion Checklist

- [ ] All test files created
- [ ] All tests compile successfully
- [ ] All tests execute without errors
- [ ] Code coverage reports generated
- [ ] Test documentation completed
- [ ] Test results documented
- [ ] Defects logged and tracked
- [ ] Performance tests executed
- [ ] Security tests executed
- [ ] Ready for Phase 6 (Documentation)

---

**Next Phase:** Phase 6 - Documentation & Final Report
**Estimated Phase 5 Duration:** 5-7 days
**Target Completion:** 2026-05-29

Generated: 2026-05-22  
Last Updated: 2026-05-22
