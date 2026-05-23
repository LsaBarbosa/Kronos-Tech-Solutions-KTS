# Sprint 1 - Phase 3: LGPD-S01-03 Checkpoint

**Status:** 📋 ANALYSIS COMPLETE - READY FOR IMPLEMENTATION

**Date:** 22 de maio de 2026

**Feature:** Enable liveness requirement in production (anti-spoofing)

---

## Summary

LGPD-S01-03 implements liveness detection to prevent biometric spoofing attacks. The backend infrastructure is substantially in place with config-driven enforcement. Frontend implementation requires liveness detection library integration and UI updates.

---

## Step 2: Current Architecture & Gap Analysis

### Backend Liveness Framework (EXISTING)

**BiometricProtectionService.java**
- ✅ `protectPublicLogin(faceImageBase64, livenessPassed)` - Enforces liveness for facial login
- ✅ `protectCheckIn(employeeId, faceImageBase64, livenessPassed)` - Enforces liveness for time record check-in
- ❌ `protectEnrollment(employeeId, faceImageBase64)` - **MISSING livenessPassed parameter**
- ✅ `ensureLiveness(Boolean livenessPassed)` - Checks if liveness is required and validates
- ✅ `livenessRequired` config property (line 39)

**Current Validation Logic**
```java
private void ensureLiveness(Boolean livenessPassed) {
    if (livenessRequired && !Boolean.TRUE.equals(livenessPassed)) {
        throw new ForbiddenException(LIVENESS_REQUIRED);
    }
}
```

**Configuration (application-prod.yml, line 200)**
```yaml
biometric:
  liveness-required: ${BIOMETRIC_LIVENESS_REQUIRED:false}
```

### DTOs Requiring Updates

**FaceLoginRequest.java** ✅ ALREADY HAS LIVENESS
```java
public record FaceLoginRequest(
    String faceImageBase64,
    Boolean livenessPassed  // ✅ Already implemented
)
```

**RegisterFaceRequest.java** ❌ MISSING LIVENESS
```java
public record RegisterFaceRequest(
    @NotBlank(message = IMAGE_DATA_REGISTER_NOT_BLANK)
    @Size(max = 1500000)
    String faceImageBase64,
    UUID employeeId
    // ❌ MISSING: Boolean livenessPassed
)
```

**GeolocationRequest.java** ✅ ALREADY HAS LIVENESS
```java
public record GeolocationRequest(
    // ... other fields ...
    Boolean livenessPassed  // ✅ Already implemented
)
```

### Service Layer Updates Required

**EmployeeService.enrollBiometricSelf()**
```java
public void enrollBiometricSelf(RegisterFaceRequest req) {
    // ... consent validation ...
    
    // Current:
    biometricProtectionService.protectEnrollment(employeeId, req.faceImageBase64());
    
    // Needed:
    // biometricProtectionService.protectEnrollment(
    //     employeeId, 
    //     req.faceImageBase64(),
    //     req.livenessPassed()  // ADD THIS
    // );
}
```

### Frontend Liveness Detection (MISSING)

**Current State**
- ✅ FaceLoginPayload type defined with livenessPassed field
- ✅ Test contract exists for loginWithFace with liveness
- ❌ No actual liveness detection implementation
- ❌ No liveness library in package.json

**Required Library Analysis**

Common options for browser-based liveness detection:

| Library | Approach | Pros | Cons | Note |
|---------|----------|------|------|------|
| **face-api.js** | TensorFlow-based | Open source, runs locally | Older library, less maintained | Could work for basic checks |
| **AWS Rekognition JS SDK** | Cloud-based | Most accurate, enterprise-grade | Requires server-side API call | Aligns with backend Rekognition |
| **TensorFlow.js + face-detection** | ML-based | Modern, flexible | Requires training data | Research needed |
| **Custom WebRTC + Video** | Device camera capture | Full control | Significant dev effort | Not feasible for current timeline |

**Recommendation**: Use AWS Rekognition JavaScript SDK or face-api.js for MVP. AWS Rekognition is preferred since backend already uses it.

### Frontend DTOs & Services

**FaceLoginPayload (terms.service.ts)** ✅ HAS LIVENESS
```typescript
export interface FaceLoginPayload {
    faceImageBase64: string;
    livenessPassed?: boolean;  // ✅ Already present
}
```

**BiometricEnrollmentRequest (employee.service.ts)** ❌ MISSING LIVENESS
```typescript
interface BiometricEnrollmentRequest {
    faceImageBase64: string;
    // ❌ MISSING: livenessPassed?: boolean;
}
```

**BiometricEnrollmentModal.tsx** ❌ MISSING LIVENESS FLOW
```typescript
// Current flow:
// 1. Select image file
// 2. Convert to Base64
// 3. Submit to enrollBiometric()

// Needed:
// 1. Select image file
// 2. Perform liveness detection
// 3. Convert to Base64
// 4. Submit with livenessPassed flag
```

---

## Step 3: Impact Matrix - Phase 3 Changes

### Backend Changes

| Component | File | Change | Complexity | Risk |
|-----------|------|--------|-----------|------|
| **BiometricProtectionService** | `BiometricProtectionService.java` | Update `protectEnrollment()` to accept and check `livenessPassed` | Low | Low |
| **RegisterFaceRequest** | `RegisterFaceRequest.java` | Add `Boolean livenessPassed` field (nullable) | Low | Low |
| **EmployeeService** | `EmployeeService.java` | Pass `req.livenessPassed()` to `protectEnrollment()` | Low | Low |
| **EmployeeController** | `EmployeeController.java` | No changes (DTO binding handles it) | None | None |
| **Test Service** | `EmployeeServiceTest.java` | Add tests for liveness requirement in enrollment | Medium | Low |
| **Config (Prod)** | `application-prod.yml` | Change default or document activation via env var | Low | Low |

### Frontend Changes

| Component | File | Change | Complexity | Risk |
|-----------|------|--------|-----------|------|
| **BiometricEnrollmentModal** | `BiometricEnrollmentModal.tsx` | Add liveness detection before submission | High | Medium |
| **employee.service.ts** | `employee.service.ts` | Add livenessPassed to BiometricEnrollmentRequest interface | Low | Low |
| **enrollBiometric()** | `employee.service.ts` | Pass livenessPassed in request payload | Low | Low |
| **Dependencies** | `package.json` | Add liveness detection library | Medium | Low |
| **Tests** | New test file | Add liveness detection tests | Medium | Low |

### Data Model Changes
- ❌ None required - LegalConsent already records consent, not liveness state
- ℹ️ Note: Liveness is enforced per request, not stored per user

### Breaking Changes
- ⚠️ **Backward Compatibility**: RegisterFaceRequest gains new optional field - non-breaking
- ⚠️ **API Versioning**: No versioning needed, field is optional (defaults to null)
- ⚠️ **Security Impact**: When enabled, will start rejecting enrollments without liveness - EXPECTED

---

## Step 4: Affected Files List

### Backend Files

**Core Service Layer**
```
src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java
  └─ Update protectEnrollment() signature
  └─ Add livenessPassed parameter
```

**Request/Response DTOs**
```
src/main/java/com/kts/kronos/adapter/in/web/dto/employee/RegisterFaceRequest.java
  └─ Add livenessPassed field (nullable Boolean)
```

**Application Services**
```
src/main/java/com/kts/kronos/application/service/EmployeeService.java
  └─ Update enrollBiometricSelf() to pass livenessPassed
```

**Configuration**
```
src/main/resources/application-prod.yml
  └─ Document biometric.liveness-required setting
  └─ Note: Activation via BIOMETRIC_LIVENESS_REQUIRED env var
```

**Test Files**
```
src/test/java/com/kts/kronos/application/service/EmployeeServiceTest.java
  └─ Add test: shouldRejectBiometricEnrollmentWithoutLiveness()
  └─ Add test: shouldAcceptBiometricEnrollmentWithLiveness()

src/test/java/com/kts/kronos/adapter/in/web/http/EmployeeControllerTest.java
  └─ Add test: enrollment endpoint with/without livenessPassed
```

### Frontend Files

**Components**
```
src/components/privacy/BiometricEnrollmentModal.tsx
  └─ Add liveness detection flow
  └─ Show "Checking liveness..." loading state
  └─ Add error handling for liveness check failure
  └─ Conditionally disable submit based on liveness result
```

**Services**
```
src/service/employee.service.ts
  └─ Update BiometricEnrollmentRequest interface
  └─ Add livenessPassed field
  └─ Pass livenessPassed in enrollBiometric() call
```

**Configuration**
```
package.json
  └─ Add liveness detection library (face-api.js or AWS SDK)
  └─ Update build configuration if needed
```

**Tests**
```
src/test/biometric-enrollment-liveness.test.ts (NEW)
  └─ Test liveness requirement in enrollment
  └─ Test livenessPassed in request payload
  └─ Test error handling for liveness failure

src/components/__tests__/BiometricEnrollmentModal.test.tsx (NEW)
  └─ Test modal with liveness detection enabled
  └─ Test submit disabled until liveness passes
  └─ Test error state if liveness fails
```

### Environment Variables to Configure

**Production Deployment**
```
BIOMETRIC_LIVENESS_REQUIRED=true  # Enable enforcement
```

**Optional (for strictness control)**
```
BIOMETRIC_LIVENESS_DETECTION_STRICTNESS=medium  # or: low, high
```

---

## Implementation Approach

### Phase 3A: Backend Foundation (1-2 hours)

1. **Update RegisterFaceRequest DTO**
   - Add `Boolean livenessPassed` (nullable)
   - No validation needed (protectEnrollment validates)

2. **Update BiometricProtectionService**
   - Change `protectEnrollment(employeeId, faceImageBase64)` 
   - To: `protectEnrollment(employeeId, faceImageBase64, livenessPassed)`
   - Call existing `ensureLiveness(livenessPassed)`

3. **Update EmployeeService**
   - Pass `req.livenessPassed()` to `protectEnrollment()`

4. **Add Tests**
   - Test enrollment rejected without liveness when enabled
   - Test enrollment accepted with livenessPassed=true

### Phase 3B: Frontend Implementation (2-4 hours)

1. **Choose Liveness Library**
   - Option A: face-api.js (lightweight, local)
   - Option B: AWS Rekognition JS SDK (most accurate, matches backend)
   - Recommendation: face-api.js for MVP speed

2. **Create Liveness Detection Hook**
   - `useLivenessDetection()` hook
   - Accepts image Base64 or File
   - Returns Promise<boolean> with detection result
   - Handles errors gracefully

3. **Update BiometricEnrollmentModal**
   - Add "Detecting liveness..." loading state
   - Perform liveness check after image selection
   - Disable submit until liveness passes
   - Show error if liveness check fails
   - Pass livenessPassed=true to enrollBiometric()

4. **Update employee.service.ts**
   - Add livenessPassed to BiometricEnrollmentRequest interface
   - Pass field in POST body

5. **Add Frontend Tests**
   - Test liveness check is performed
   - Test submit only enabled after successful liveness
   - Test error handling

### Phase 3C: Configuration & Validation (1 hour)

1. **Production Config**
   - Set BIOMETRIC_LIVENESS_REQUIRED=true
   - Verify behavior in staging first

2. **Testing**
   - Test rejection of enrollment without liveness
   - Test acceptance with liveness check
   - Verify error messages

3. **Documentation**
   - Document new behavior in README
   - Add to CHANGELOG

---

## Risk Assessment

### Low Risk
- ✅ Config-driven (can be disabled if issues)
- ✅ Optional field in DTO (backward compatible)
- ✅ Frontend graceful degradation (works without detection library in dev)

### Medium Risk
- ⚠️ Liveness detection accuracy depends on library quality
- ⚠️ User experience impact if detection fails frequently
- ⚠️ AWS SDK costs if using cloud-based detection

### Mitigation Strategies
1. **Soft Launch**: Enable for new enrollments only, not login
2. **Strictness Levels**: Dev mode has lower strictness than prod
3. **User Feedback**: Show helpful error messages
4. **Monitoring**: Track liveness failure rates

---

## Success Criteria

- ✅ Biometric enrollment requires liveness detection in production
- ✅ Biometric login requires liveness detection in production  
- ✅ Biometric check-in requires liveness detection in production
- ✅ Liveness can be disabled via environment variable
- ✅ Liveness failure shows user-friendly error message
- ✅ All tests passing (existing + new)
- ✅ Frontend build succeeds with no TypeScript errors
- ✅ No production warnings or regressions

---

## Known Dependencies

**Backend**
- No new dependencies needed (logic fully in existing service)

**Frontend**
- ⚠️ Need to add liveness detection library
  - face-api.js: `npm install face-api.js`
  - Or AWS SDK: Already available if configured

**Configuration**
- Environment variable: `BIOMETRIC_LIVENESS_REQUIRED`
- Library models (if using face-api.js) require weights download

---

## Timeline Estimate

| Phase | Task | Estimate | Notes |
|-------|------|----------|-------|
| 3A | Backend updates | 1-2h | Minor DTO/service updates |
| 3A | Backend tests | 1h | Add 2-3 test cases |
| 3B | Liveness library integration | 1-2h | Depends on library choice |
| 3B | Modal updates | 2-3h | UI flow, error handling |
| 3B | Frontend tests | 1-2h | Mock liveness results |
| 3C | Configuration & validation | 1h | Set env vars, test flows |
| **TOTAL** | | **7-11h** | Full Phase 3 implementation |

---

## Current Status by Component

| Component | Status | Notes |
|-----------|--------|-------|
| Backend infrastructure | ✅ Complete | protectPublicLogin/CheckIn working |
| Liveness config property | ✅ Complete | Defaults to false, can be enabled |
| RegisterFaceRequest | ❌ Needs update | Add livenessPassed field |
| protectEnrollment() | ❌ Needs update | Add livenessPassed parameter |
| Frontend detection library | ❌ Missing | Needs to be added |
| BiometricEnrollmentModal | ❌ Needs update | Add liveness flow |
| Tests | ⚠️ Partial | Login/checkin tested, enrollment not tested |

---

## Next Steps (Ready for Phase 3 Implementation)

1. ✅ Choose liveness detection library (face-api.js recommended)
2. ✅ Implement backend DTO & service updates (3A)
3. ✅ Implement frontend integration (3B)
4. ✅ Add comprehensive tests (3A + 3B)
5. ✅ Update configuration for production (3C)
6. ✅ Validate in staging environment (3C)
7. ✅ Deploy to production with BIOMETRIC_LIVENESS_REQUIRED=true

---

**Phase 3 Ready For Implementation**

Estimated effort: 7-11 hours
Priority: P1 (Production requirement)
Risk level: Low-Medium
Dependencies: Liveness detection library selection

