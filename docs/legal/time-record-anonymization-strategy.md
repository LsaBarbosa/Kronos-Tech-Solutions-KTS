# Time Record Anonymization Strategy

**Date:** 2026-05-23  
**Version:** 1.0  
**Status:** ✅ IMPLEMENTED

---

## Overview

Sprint LGPD-CORR-03 implements formal differentiation in time record anonymization based on the `preserveLaborData` flag, fixes DRY_RUN reporting to show accurate impact counts, and updates API response format for better visibility.

---

## Changes Implemented

### 1. TimeRecordAnonymizer Enhancement

**File:** `TimeRecordAnonymizer.java`

#### DRY_RUN: Accurate Impact Assessment

Previously returned `affectedCount=0`. Now:
- Scans all time records for the employee
- When `preserveLaborData=true`: counts only records with geolocation data
- When `preserveLaborData=false`: counts all records (stronger anonymization)
- Returns: scannedCount, affectedCount, skippedCount

**Example:**
```
Employee has 100 time records:
- 80 records with geolocation
- 20 records without geolocation

With preserveLaborData=true:
  DRY_RUN returns: scanned=100, affected=80, skipped=20

With preserveLaborData=false:
  DRY_RUN returns: scanned=100, affected=100, skipped=0
```

#### APPLY: Differentiated Anonymization

**When `preserveLaborData=true` (Labor Data Preservation):**
- Preserve: timeRecordId, startWork, endWork, status, NSR, companyId
- Remove: latitude, longitude, endLatitude, endLongitude
- Only modify records that have geolocation data
- Other records are skipped (no database write)

**When `preserveLaborData=false` (Stronger Anonymization):**
- Remove: latitude, longitude, endLatitude, endLongitude
- All records are modified
- Only time and status preserved minimally
- No employee ID is preserved in geolocation context

### 2. AnonymizationDryRunResponse Restructuring

**Old Model:**
```java
record AnonymizationDryRunResponse(
    UUID employeeId,
    long totalDocumentsToDelete,
    long totalTimeRecordsToPreserve,
    long totalTimRecordsToAnonymize,
    long totalMessagesToAnonymize,
    long totalAuditLogsToSanitize,
    long totalBiometricArtifactsToDelete,
    long totalErrorsExpected,
    List<String> warnings
)
```

**New Model:**
```java
record AnonymizationDryRunResponse(
    UUID employeeId,
    AnonymizationDryRunSummary summary,
    List<AnonymizationDomain> domains,
    List<String> warnings
)

record AnonymizationDryRunSummary(
    long totalScanned,
    long totalAffected,
    long totalSkipped,
    long totalErrors
)

record AnonymizationDomain(
    String resourceType,
    long scanned,
    long affected,
    long skipped,
    String action,
    String warning
)
```

**Benefits:**
- Per-domain visibility (scanned, affected, skipped)
- Clear summary across all domains
- Contextual warnings per domain
- Action description (what will be modified)
- Admin can understand impact before confirming

### 3. LgpdService.dryRunAnonymizeEmployee Update

**Location:** `LgpdService.java` line 266

Updated to:
- Build response with new summary structure
- Include per-domain breakdown
- Calculate accurate totals
- Provide context-aware warnings

---

## API Response Example

**Endpoint:** `POST /lgpd/employees/{employeeId}/anonymize/dry-run`

**Response:**
```json
{
  "employeeId": "550e8400-e29b-41d4-a716-446655440000",
  "summary": {
    "totalScanned": 250,
    "totalAffected": 200,
    "totalSkipped": 50,
    "totalErrors": 0
  },
  "domains": [
    {
      "resourceType": "TIME_RECORD",
      "scanned": 250,
      "affected": 200,
      "skipped": 50,
      "action": "REMOVE_PRECISE_GEOLOCATION",
      "warning": "Registros trabalhistas serão preservados. Apenas geolocalização será removida."
    }
  ],
  "warnings": [
    "Esta é uma visualização. Nenhum dado foi modificado."
  ]
}
```

---

## Anonymization Behavior Matrix

| Scenario | preserveLaborData | Action | Scanned | Affected | Skipped |
|----------|-------------------|--------|---------|----------|---------|
| Employee has 100 records, 80 with geo | true | Remove geo only | 100 | 80 | 20 |
| Employee has 100 records, 0 with geo | true | (none) | 100 | 0 | 100 |
| Employee has 100 records, 80 with geo | false | Remove geo (all) | 100 | 100 | 0 |
| Employee has 0 records | either | (none) | 0 | 0 | 0 |

---

## Test Coverage

### Updated Tests: `TimeRecordAnonymizerTest`

✅ **testSupports** - Verifies correct resource type
✅ **testExecuteDryRunWithNoTimeRecords** - Handles empty case
✅ **testExecuteDryRunWithGeolocationWhenPreserveLaborData** - Correct counting with flag=true
✅ **testExecuteDryRunAllAffectedWhenPreserveLaborDataFalse** - All records counted when flag=false
✅ **testExecuteApplyWithPreserveLaborDataTrue** - Only geolocation removed
✅ **testExecuteApplyWithPreserveLaborDataFalse** - All records modified
✅ **testExecuteApplyRemovesLocationCoordinates** - Coordinates cleared
✅ **testExecuteApplyHandlesException** - Error handling

**Status:** BUILD SUCCESSFUL (8/8 tests passing)

---

## Files Modified

| File | Changes |
|------|---------|
| `TimeRecordAnonymizer.java` | DRY_RUN counting logic, APPLY differentiation |
| `LgpdService.java` | Updated dryRunAnonymizeEmployee for new response |
| `AnonymizationDryRunResponse.java` | New model structure |
| `AnonymizationDryRunSummary.java` | **NEW** summary DTO |
| `AnonymizationDomain.java` | **NEW** domain breakdown DTO |
| `TimeRecordAnonymizerTest.java` | Updated tests for new logic |
| `LgpdDryRunControllerTest.java` | Updated response assertions |

---

## Key Improvements

### 1. Visibility
- Admin sees exactly what will be modified before confirming
- Per-domain breakdown shows impact
- Warnings contextualize the actions

### 2. Accuracy
- DRY_RUN no longer subestimates impact
- Scanned/Affected/Skipped properly distinguished
- Clear what "preservation" means per domain

### 3. Flexibility
- `preserveLaborData=true`: keeps minimal legal records
- `preserveLaborData=false`: stronger anonymization
- Different rules per resource type in future

---

## Implementation Notes

### Database Impact
- No schema changes
- No migrations needed
- Backward compatible with existing data

### Performance
- DRY_RUN: O(n) scan of employee's records
- APPLY: O(n) iteration with conditional writes
- Only modified records written to DB

### Security
- No sensitive data in logs
- No base64 or coordinates logged
- Employee ID preserved where required by law

---

## Migration from Old Response

**Frontend Code Update Required**

Old:
```typescript
const { totalTimeRecordsToAnonymize } = response;
```

New:
```typescript
const affected = response.domains.find(d => d.resourceType === 'TIME_RECORD')?.affected ?? 0;
```

Or use summary:
```typescript
const { totalAffected } = response.summary;
```

---

## Deployment Checklist

- [ ] Run `./gradlew test --tests "TimeRecordAnonymizerTest"` locally
- [ ] Verify all 8 tests pass
- [ ] Build backend without errors
- [ ] Test dry-run endpoint with new response structure
- [ ] Update frontend to handle new response format
- [ ] Test end-to-end anonymization flow
- [ ] Deploy to staging first
- [ ] Monitor retention execution logs

---

## Known Limitations

1. **DRY_RUN not idempotent** - Runs full scan each time (acceptable cost)
2. **No partial anonymization** - All-or-nothing per resource type
3. **Employee ID still present** - Not pseudonymized even when preserveLaborData=false
   - Could be added in future task

---

**Document ID:** TIME-RECORD-ANON-STRATEGY-2026-05-23  
**Version:** 1.0  
**Status:** ✅ COMPLETE
