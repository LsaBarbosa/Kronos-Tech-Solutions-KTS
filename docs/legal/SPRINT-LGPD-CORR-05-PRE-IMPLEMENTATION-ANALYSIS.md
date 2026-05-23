# Sprint LGPD-CORR-05: Pre-Implementation Analysis

**Sprint:** LGPD-CORR-05 — Inventário LGPD e prefixo `/api`  
**Date:** 2026-05-23  
**Status:** PRE-IMPLEMENTATION  
**Objective:** Remover risco de divergência entre rotas do front-end e do back-end no inventário LGPD

---

## 1. Branch Status

✅ **Backend:** `feature/lgpd-compliance`  
✅ **Frontend:** `feature/lgpd-compliance`

---

## 2. Current State Analysis

### Backend Routes
- **Controller:** `/api/lgpd/inventory` (hardcoded in DataProcessingInventoryController)
- **Other LGPD routes:** Use ApiPaths constants (LGPD, LGPD_REQUESTS, etc.)
- **Inconsistency:** Inventory routes NOT defined in ApiPaths.java

### Frontend Routes
- **Base pattern:** `buildRoute(API_ROUTES.LGPD, LGPD_PATHS.*)`
- **API path constants:** Defined in `api-routes.ts`
- **Inconsistency:** inventory.service.ts line 105 uses hardcoded string instead of constant
- **API baseURL:** Configured via `VITE_API_BASE_URL` environment variable

### Current Path Structure
```
Frontend builds:     /lgpd/inventory, /lgpd/requests, etc.
Axios adds:          http://localhost:8080 (or env baseURL)
Final URL:           http://localhost:8080/lgpd/inventory
Backend expects:     /api/lgpd/inventory
```

⚠️ **Risk:** Frontend routes missing `/api` prefix! Only works if reverse proxy strips it.

---

## 3. Impact Matrix

### Task 05-01: Standardize LGPD Route Prefixes

| Component | Impact Level | Change Type | Risk | Notes |
|-----------|---|---|---|---|
| ApiPaths.java | HIGH | Add constants | LOW | Add LGPD_INVENTORY, LGPD_INVENTORY_ACTIVE, LGPD_INVENTORY_BY_CODE |
| DataProcessingInventoryController | MEDIUM | Replace hardcoded paths | LOW | Use ApiPaths constants instead of strings |
| buildRoute function | MEDIUM | Ensure consistency | NONE | Already correct, verify no changes needed |
| Frontend api-routes.ts | MEDIUM | Verify constants | NONE | Already has LGPD_PATHS defined correctly |
| Frontend api.ts | MEDIUM | Verify baseURL handling | LOW | Ensure /api prefix included in requests |
| Tests | HIGH | Add contract tests | MEDIUM | Create tests to verify front/back URL matching |

### Task 05-02: Fix Inventory Update by inventoryId

| Component | Impact Level | Change Type | Risk | Notes |
|-----------|---|---|---|---|
| DataProcessingInventoryController | LOW | Verify endpoint | NONE | PATCH /{inventoryId} already exists |
| DataProcessingInventoryService | LOW | Verify implementation | NONE | Update by inventoryId already implemented |
| inventory.service.ts | MEDIUM | Fix hardcoded string | LOW | Line 105: replace hardcoded with constant |
| InventoryForm.tsx | MEDIUM | Update logic | LOW | Ensure loads by processCode, updates by inventoryId |
| Types | LOW | Verify interfaces | NONE | DataProcessingInventoryResponse already has inventoryId |
| Tests | MEDIUM | Add integration tests | LOW | Test create→fetch→update flow |

---

## 4. Files to Be Modified

### Backend Files

1. **`src/main/java/com/kts/kronos/constants/ApiPaths.java`**
   - Add inventory route constants
   - Add: `LGPD_INVENTORY = "/inventory"`
   - Add: `LGPD_INVENTORY_ACTIVE = "/inventory/active"`
   - Add: `LGPD_INVENTORY_BY_CODE = "/inventory/{processCode}"`

2. **`src/main/java/com/kts/kronos/adapter/in/web/http/DataProcessingInventoryController.java`**
   - Replace hardcoded `/api/lgpd/inventory` with ApiPaths constants
   - Update all @GetMapping, @PostMapping, @PatchMapping annotations
   - Verify all paths use consistent pattern

3. **`src/test/java/com/kts/kronos/adapter/in/web/http/DataProcessingInventoryControllerTest.java`** (NEW)
   - Contract test: verify /api/lgpd/inventory endpoints are accessible
   - Test: no /api/api duplication
   - Test: inventory CRUD operations

### Frontend Files

1. **`src/config/api-routes.ts`**
   - Verify LGPD_PATHS.INVENTORY constants are correctly defined
   - No changes needed (already correct)

2. **`src/service/inventory.service.ts`**
   - Fix line 105: replace `inventory/${inventoryId}` with LGPD_PATHS constant
   - Ensure all paths use `buildRoute(API_ROUTES.LGPD, LGPD_PATHS.*)`
   - Add `/api` prefix verification comment

3. **`src/components/privacy/InventoryForm.tsx`**
   - Verify it loads inventory by `processCode`
   - Verify it saves `inventoryId` from response
   - Verify PATCH uses `inventoryId` in path

4. **`src/components/privacy/AdminInventory.tsx`**
   - Verify pagination and search work correctly
   - No changes needed (already uses service functions)

5. **`src/config/api.ts`**
   - Verify `VITE_API_BASE_URL` configuration
   - Add comment documenting `/api` prefix handling

---

## 5. Testing Strategy

### Backend Tests (NEW)

1. **DataProcessingInventoryControllerTest**
   - Test all endpoints return 200
   - Verify path construction matches ApiPaths constants
   - Test authorization (CTO only)
   - Test no /api/api duplication in logs

2. **Contract Tests**
   - Verify /api/lgpd/inventory endpoints exist
   - Verify PATCH /api/lgpd/inventory/{inventoryId} works
   - Verify GET /api/lgpd/inventory/{processCode} works

### Frontend Tests (NEW)

1. **InventoryForm Integration Tests**
   - Test: create inventory → receive inventoryId
   - Test: load inventory by processCode
   - Test: update using inventoryId (not processCode)
   - Test: PATCH /api/lgpd/inventory/{inventoryId} called

2. **Service Tests**
   - Verify all service functions use correct constants
   - Verify buildRoute generates /api prefixed URLs
   - Verify no hardcoded paths

---

## 6. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|-----------|
| Frontend missing /api prefix | HIGH | Verify api.ts baseURL includes /api or ensure axios interceptor adds it |
| Reverse proxy stripping /api | MEDIUM | Add contract tests to verify exact URLs match |
| Partial inventory updates | LOW | Test full create→fetch→update flow end-to-end |
| Authorization bypass | LOW | Verify CTO-only check on backend and frontend |

---

## 7. Route Standardization Decision

### Final Pattern (Recommended)

**Backend:** All endpoints under `/api/lgpd/**`
```
/api/lgpd/inventory
/api/lgpd/inventory/active
/api/lgpd/inventory/{processCode}
/api/lgpd/inventory/{inventoryId}
/api/lgpd/requests
/api/lgpd/admin/requests
```

**Frontend:** Build routes consistently
```typescript
buildRoute(API_ROUTES.LGPD, LGPD_PATHS.INVENTORY)
buildRoute(API_ROUTES.LGPD, LGPD_PATHS.INVENTORY_ACTIVE)
buildRoute(API_ROUTES.LGPD, LGPD_PATHS.INVENTORY_BY_CODE(code))
buildRoute(API_ROUTES.LGPD, LGPD_PATHS.REQUESTS)
```

**Axios Configuration:**
```typescript
// api.ts
const baseURL = `${process.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`
// This ensures /api is included: http://localhost:8080/api/lgpd/inventory
```

---

## 8. Pre-Implementation Checklist

- [x] Branches confirmed (feature/lgpd-compliance on both repos)
- [x] Sprint requirements documented (backlog section 4)
- [x] Files identified and analyzed
- [x] Impact matrix created
- [x] Risk assessment completed
- [x] Testing strategy defined
- [x] Route standardization decision made

**Ready to proceed with implementation:** ✅

---

**Document ID:** LGPD-CORR-05-PRE-IMPL-2026-05-23  
**Version:** 1.0  
**Status:** READY FOR IMPLEMENTATION
