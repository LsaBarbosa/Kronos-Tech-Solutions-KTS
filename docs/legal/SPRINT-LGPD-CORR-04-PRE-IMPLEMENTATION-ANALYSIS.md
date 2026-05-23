# Sprint LGPD-CORR-04: Pre-Implementation Analysis

**Sprint:** LGPD-CORR-04 — Controle de falhas parciais na anonimização  
**Date:** 2026-05-23  
**Status:** PRE-IMPLEMENTATION  
**Objective:** Impedir que uma anonimização parcial seja tratada como sucesso total

---

## 1. Branch Status

✅ **Backend:** `feature/lgpd-compliance` (commit 81c57f6 - done)  
✅ **Frontend:** `feature/lgpd-compliance` (commit 5892902 - [Sprint 10] Frontend Privacy Center enhancement)

---

## 2. Impact Matrix

### Task 04-01: Create Consolidated Anonymization Status

| Component | Impact Level | Change Type | Risk | Notes |
|-----------|---|---|---|---|
| Domain Model | HIGH | Enum + Consolidation Logic | LOW | New status field required in `AnonymizationExecutionResult` |
| `AnonymizationPlanExecutor` | HIGH | Orchestration Logic | LOW | Must compute overall status from processor results |
| Tests | HIGH | New Test Cases | MEDIUM | Must cover SUCCESS, PARTIAL_SUCCESS, FAILED, BLOCKED scenarios |
| Logging | MEDIUM | Enhanced Tracking | LOW | Log consolidated status for audit |

### Task 04-02: Block LGPD Request Conclusion on Partial/Failed Anonymization

| Component | Impact Level | Change Type | Risk | Notes |
|-----------|---|---|---|---|
| `LgpdService` | HIGH | Business Logic | MEDIUM | Must validate anonymization status before allowing status transition to COMPLETED |
| `LgpdRequest` Entity | MEDIUM | Schema/Model | MEDIUM | Add optional reference to anonymization result |
| `LgpdRequestRepository` | LOW | Query Method | LOW | May need findWithAnonymizationResult() method |
| Authorization | LOW | Policy | LOW | CTO/MANAGER must not bypass check without explicit justification |
| Tests | HIGH | Integration Tests | MEDIUM | Must cover all transitions (SUCCESS→COMPLETED, PARTIAL→BLOCKED, FAILED→IN_ANALYSIS) |

### Task 04-03: Frontend Anonymization Result Display

| Component | Impact Level | Change Type | Risk | Notes |
|-----------|---|---|---|---|
| `AdminLgpdRequestDetails` | MEDIUM | UI Enhancement | LOW | Add result summary card/section |
| New Component `AnonymizationResultSummary` | MEDIUM | New Component | LOW | Standalone component for result display |
| `api-routes.ts` | LOW | Constants | NONE | Add new route constant |
| `lgpd.service.ts` | LOW | API Service | NONE | Add getAnonymizationResult() function |
| Tests | MEDIUM | Component Tests | LOW | Test result display states |

---

## 3. Files to Be Modified

### Backend Files

#### Core Changes (Required for 04-01 and 04-02)

1. **`src/main/java/com/kts/kronos/domain/model/AnonymizationExecutionResult.java`**
   - Add `consolidatedStatus` field (enum: SUCCESS, PARTIAL_SUCCESS, FAILED, BLOCKED)
   - Add factory method: `static consolidate(List<AnonymizationExecutionResult>)`
   - Add getter for failed domains

2. **`src/main/java/com/kts/kronos/application/service/anonymization/AnonymizationPlanExecutor.java`**
   - Modify `executePlanWithResults()` to consolidate multi-processor results
   - Add status consolidation logic
   - Log consolidated result

3. **`src/main/java/com/kts/kronos/application/service/LgpdService.java`**
   - Modify `anonymizeEmployee()` to store/check anonymization result
   - Add validation in `transitionStatus()` to block COMPLETED/PARTIALLY_COMPLETED on FAILED/BLOCKED
   - Add `validateAnonymizationBefore Conclusion()` method

4. **`src/main/java/com/kts/kronos/domain/model/LgpdRequest.java`**
   - Add optional field: `anonymizationResultId` (UUID)
   - Add optional field: `anonymizationConsolidatedStatus` (enum)

5. **`src/main/java/com/kts/kronos/adapter/in/web/http/LgpdController.java`**
   - Add endpoint: `GET /admin/requests/{requestId}/anonymization-result`
   - Add endpoint to view detailed anonymization result with domain breakdown

#### Supporting Changes (Tests & DTOs)

6. **`src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdRequestDetailsResponse.java`**
   - Add `anonymizationResult` field
   - Include consolidated status in response

7. **`src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/AnonymizationConsolidatedResultResponse.java`** (NEW)
   - consolidatedStatus (enum)
   - successCount, partialCount, failedCount, blockedCount
   - domainResults (array with resourceType, status, errorMessage)
   - executionLog details

8. **`src/test/java/com/kts/kronos/application/service/anonymization/AnonymizationPlanExecutorConsolidationTest.java`** (NEW)
   - Test consolidation: all SUCCESS → overall SUCCESS
   - Test consolidation: one FAILED → overall PARTIAL_SUCCESS
   - Test consolidation: multiple FAILED → overall FAILED
   - Test blocking when status BLOCKED

9. **`src/test/java/com/kts/kronos/application/service/LgpdServiceConclusionBlockTest.java`** (NEW)
   - Test COMPLETED allowed when anonymization SUCCESS
   - Test COMPLETED blocked when anonymization PARTIAL_SUCCESS
   - Test COMPLETED blocked when anonymization FAILED
   - Test PARTIALLY_COMPLETED allowed with justification

10. **`src/test/java/com/kts/kronos/adapter/in/web/http/LgpdControllerAnonymizationResultTest.java`** (NEW)
    - Test GET /admin/requests/{requestId}/anonymization-result
    - Test authorization (CTO/MANAGER only)
    - Test result detail display

### Frontend Files

#### Core Changes (Required for 04-03)

1. **`src/components/privacy/AnonymizationResultSummary.tsx`** (NEW)
   - Component to display consolidated anonymization status
   - Domain-by-domain breakdown
   - Error messages and warnings
   - Visual indicators (SUCCESS/PARTIAL_SUCCESS/FAILED/BLOCKED)

2. **`src/pages/AdminLgpdRequestDetails.tsx`** (or similar)
   - Integrate `AnonymizationResultSummary` component
   - Show result after anonymization completes
   - Disable "Conclude Request" button if status FAILED/BLOCKED

3. **`src/config/api-routes.ts`**
   - Add constant: `ANONYMIZATION_RESULT: (requestId: string) => ...`

4. **`src/service/lgpd.service.ts`**
   - Add function: `getAnonymizationResult(requestId: string)`
   - Add type: `AnonymizationConsolidatedResultResponse`

#### Supporting Changes (Tests)

5. **`src/components/privacy/__tests__/AnonymizationResultSummary.test.tsx`** (NEW)
   - Test rendering SUCCESS status
   - Test rendering PARTIAL_SUCCESS with failed domains
   - Test rendering FAILED status
   - Test error message display

6. **`src/pages/__tests__/AdminLgpdRequestDetails.test.tsx`** (Modified)
   - Add tests for anonymization result integration
   - Test "Conclude Request" button state based on result

---

## 4. Detailed Change Overview

### 04-01: Status Consolidation

**Current Behavior:**
- `AnonymizationExecutionResult` has status "SUCCESS" or "ERROR"
- Multiple processors may fail but executor reports overall as "SUCCESS"
- No visibility into which domains failed

**New Behavior:**
- `AnonymizationExecutionResult` includes `consolidatedStatus` enum
- Values: SUCCESS (all OK), PARTIAL_SUCCESS (some failed), FAILED (critical failure), BLOCKED (security lock)
- List of failed domains returned with error messages
- Every processor result tracked individually

**Code Changes:**
```java
// Before
public enum AnonymizationStatus { SUCCESS, ERROR }

// After
public enum AnonymizationConsolidatedStatus {
  SUCCESS,
  PARTIAL_SUCCESS,
  FAILED,
  BLOCKED
}
```

### 04-02: Request Conclusion Blocking

**Current Behavior:**
- Admin can mark request as COMPLETED even if anonymization failed
- No validation of anonymization result before conclusion

**New Behavior:**
- Validation check in `transitionStatus()` method
- COMPLETED only allowed when anonymization status = SUCCESS
- PARTIALLY_COMPLETED allowed when PARTIAL_SUCCESS (with admin justification)
- Other statuses return error with required actions

**Authorization:**
- CTO: Can attempt override with strong justification
- MANAGER: Cannot override; must wait for legal review
- EMPLOYEE: Cannot transition to COMPLETED (read-only view)

### 04-03: Frontend Result Display

**Current Behavior:**
- Admin initiates anonymization, no detailed result view
- Result status not visible in request details

**New Behavior:**
- New `AnonymizationResultSummary` component shows:
  - Consolidated status (SUCCESS/PARTIAL_SUCCESS/FAILED/BLOCKED)
  - Scanned/Affected/Skipped/Error counts by domain
  - Error messages for failed domains
  - Timestamp of execution
  - Recommended actions

**Component Placement:**
- In `AdminLgpdRequestDetails` page
- Displayed after anonymization execution
- Updates request status buttons based on result

---

## 5. Database Changes

**Schema Changes Required:** NONE (using existing fields and new optional columns)

**Possible Migrations:** 
- Add column `anonymizationConsolidatedStatus` to `lgpd_request` (nullable)
- Add column `anonymizationResultId` to `lgpd_request` (nullable, foreign key)

---

## 6. API Changes

### New Endpoints

```http
GET /admin/requests/{requestId}/anonymization-result
```

**Response:**
```json
{
  "consolidatedStatus": "PARTIAL_SUCCESS",
  "executedAt": "2026-05-23T10:30:00Z",
  "summary": {
    "totalScanned": 500,
    "totalAffected": 450,
    "totalSkipped": 50,
    "totalErrors": 5
  },
  "domainResults": [
    {
      "resourceType": "TIME_RECORD",
      "status": "SUCCESS",
      "scanned": 250,
      "affected": 250,
      "skipped": 0,
      "errorCount": 0
    },
    {
      "resourceType": "DOCUMENT",
      "status": "FAILED",
      "scanned": 250,
      "affected": 200,
      "skipped": 50,
      "errorCount": 5,
      "errorMessage": "S3 connection timeout"
    }
  ],
  "failedDomains": ["DOCUMENT"],
  "warnings": [
    "Document deletion failed due to S3 unavailability. Storage cleanup required."
  ]
}
```

### Modified Endpoints

```http
PATCH /admin/requests/{requestId}/conclude
```

**New Validation:**
- Reject if anonymization status is FAILED or BLOCKED
- Allow if anonymization status is SUCCESS or PARTIAL_SUCCESS (with justification)
- Return error code: `LGPD_REQUEST_CONCLUSION_BLOCKED_BY_ANONYMIZATION_STATUS`

---

## 7. Testing Strategy

### Backend Tests (High Priority)

1. **AnonymizationPlanExecutorConsolidationTest**
   - All processors SUCCESS → consolidated SUCCESS
   - One processor FAILED → consolidated PARTIAL_SUCCESS
   - Multiple processors FAILED → consolidated FAILED
   - Security lock → consolidated BLOCKED

2. **LgpdServiceConclusionBlockTest**
   - SUCCESS allows COMPLETED
   - PARTIAL_SUCCESS blocks COMPLETED, allows PARTIALLY_COMPLETED with reason
   - FAILED blocks COMPLETED and PARTIALLY_COMPLETED
   - BLOCKED blocks all transitions

3. **LgpdControllerAnonymizationResultTest**
   - GET /admin/requests/{requestId}/anonymization-result
   - Proper response structure
   - Authorization checks (CTO/MANAGER)
   - 404 when no result found

### Frontend Tests (Medium Priority)

1. **AnonymizationResultSummary.test.tsx**
   - Renders SUCCESS state
   - Renders PARTIAL_SUCCESS with failed domains highlighted
   - Renders FAILED state with error details
   - Renders BLOCKED state with security message
   - Displays domain breakdown correctly

2. **AdminLgpdRequestDetails.test.tsx**
   - Integrates result summary component
   - "Conclude Request" button enabled for SUCCESS
   - "Conclude Request" button disabled for FAILED/BLOCKED
   - Shows required actions for PARTIAL_SUCCESS

---

## 8. Dependencies & Prerequisites

### From Prior Sprints (Already Completed)

✅ Sprint LGPD-CORR-01: Retention processor coverage  
✅ Sprint LGPD-CORR-02: Scheduler activation and APPLY safety lock  
✅ Sprint LGPD-CORR-03: TimeRecord anonymization strategy and dry-run accuracy

### No New External Dependencies Required

- Uses existing Spring components
- No new external libraries
- React component library already available

---

## 9. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|-----------|
| Breaking existing anonymization flow | MEDIUM | Implement with feature flag; default to new behavior only in new requests |
| Admin unable to conclude valid partial completions | MEDIUM | Provide PARTIALLY_COMPLETED status with explicit justification field |
| Frontend unable to fetch result (async delay) | LOW | Implement polling or WebSocket; graceful fallback to "loading" state |
| Database migration failure | LOW | Make new columns nullable; add rollback script |
| Authorization bypass on override | MEDIUM | Audit every override attempt; CTO-only with mandatory reason |

---

## 10. Pre-Implementation Checklist

- [x] Branches confirmed (feature/lgpd-compliance on both repos)
- [x] Backlog file read (LGPD-CORR-04 requirements documented)
- [x] Impact matrix generated (above)
- [x] Files to be modified listed (above)
- [x] Testing strategy defined (above)
- [x] Risk assessment completed (above)

**Ready to proceed with implementation:** ✅

---

**Document ID:** LGPD-CORR-04-PRE-IMPL-2026-05-23  
**Version:** 1.0  
**Status:** READY FOR IMPLEMENTATION
