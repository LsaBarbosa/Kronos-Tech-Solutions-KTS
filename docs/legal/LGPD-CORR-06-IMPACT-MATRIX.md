# LGPD-CORR-06: Impact Matrix
## Confirmação explícita na exportação do titular

**Date:** 2026-05-23  
**Sprint:** LGPD-CORR-06  
**Components Affected:** Frontend (React/TypeScript)  

---

## Risk Assessment Summary

| Category | Risk Level | Mitigation |
|----------|-----------|-----------|
| **API Changes** | ✅ NONE | No backend changes, API compatibility maintained |
| **User Flow** | ⚠️ LOW | Added confirmation step, users cannot accidentally download |
| **Data Privacy** | ✅ IMPROVED | Modal warns about sensitive data contents |
| **Test Coverage** | ✅ COMPREHENSIVE | All new components tested |
| **Backward Compatibility** | ✅ FULL | No breaking changes |

---

## Components Impact

### Frontend Changes

| Component | Type | Impact | Breaking? |
|-----------|------|--------|-----------|
| `PrivacyCenter.tsx` | Modified | Add modal state management and control | ❌ NO |
| `ExportConfirmationModal.tsx` | NEW | New confirmation modal component | ❌ NO |
| `ExportManifestDisplay.tsx` | NEW | New post-export summary component | ❌ NO |
| `lgpd.service.ts` | NO CHANGE | Existing API function unchanged | ❌ NO |
| `api-routes.ts` | NO CHANGE | Existing routes unchanged | ❌ NO |

### Backend Changes

| Component | Type | Impact | Breaking? |
|-----------|------|--------|-----------|
| **LgpdController.java** | NO CHANGE | Export endpoint unchanged | ❌ NO |
| **LgpdService.java** | NO CHANGE | Export logic unchanged | ❌ NO |
| **LgpdUseCase.java** | NO CHANGE | Export interface unchanged | ❌ NO |

---

## User Flow Impact

### Before LGPD-CORR-06
```
User clicks "Exportar Meus Dados"
    ↓
Download starts immediately (risky!)
    ↓
File saved with timestamp name
```

### After LGPD-CORR-06 (Task 06-01)
```
User clicks "Exportar Meus Dados"
    ↓
Confirmation Modal opens
    ↓
Modal shows warning about:
  - CPF, PIS, endereço, salário
  - documentos, histórico de ponto
  - geolocalização, mensagens
  - logs, consentimentos
    ↓
User can Cancel or Confirm
    ↓
If Cancel: Modal closes, no download (improved UX)
    ↓
If Confirm: Download starts
    ↓
File saved with timestamp name
```

### After Task 06-02 (Post-Export Summary)
```
After download completes:
    ↓
Export Summary Toast/Card appears showing:
  - Export date/time
  - Geolocation inclusion status
  - Exported sections list
  - Secure storage warning
    ↓
User dismisses summary
```

---

## Data Sensitivity Analysis

### Modal Warning Content (Safe)
✅ Generic warnings about data categories  
✅ No actual user data displayed  
✅ No sensitive field values  

Example:
```
"O arquivo pode conter CPF, PIS, endereço, salário..."
(Not: "CPF: 123.456.789-00")
```

### Post-Export Summary (Safe)
✅ Timestamp only (no user identifiers)  
✅ Boolean flags (true/false for geolocation)  
✅ Section names only (no actual data)  
✅ Generic security advice  

Example:
```
✅ Exportado em: 23 maio 2026, 01:22 UTC
✅ Geolocalização precisa: Não incluída
✅ Seções: CPF, Endereço, Ponto, Consentimentos
⚠️ Guarde este arquivo em local seguro
```

---

## Test Coverage Plan

### Unit Tests

#### ExportConfirmationModal.test.tsx
- ✅ Modal renders with warning text
- ✅ Cancel button closes modal without calling export
- ✅ Confirm button triggers export
- ✅ Loading state shows during export
- ✅ Error handling shows error toast
- ✅ Accessibility: proper ARIA labels

#### ExportManifestDisplay.test.tsx
- ✅ Summary displays after successful export
- ✅ Shows export timestamp
- ✅ Shows geolocation inclusion status
- ✅ Lists exported sections
- ✅ Shows security warning
- ✅ Dismiss button works

#### PrivacyCenter.test.tsx
- ✅ Export button opens modal (not direct download)
- ✅ Successful export flow works end-to-end
- ✅ Failed export shows error toast
- ✅ Modal cancel doesn't trigger export
- ✅ Export manifest displays after success

---

## File Changes Summary

### Files to Create (NEW)

```
src/components/privacy/ExportConfirmationModal.tsx
src/components/privacy/__tests__/ExportConfirmationModal.test.tsx
src/components/privacy/ExportManifestDisplay.tsx
src/components/privacy/__tests__/ExportManifestDisplay.test.tsx
```

### Files to Modify

```
src/pages/PrivacyCenter.tsx
  - Add modal state: [showExportModal, setShowExportModal]
  - Add manifest state: [exportManifest, setExportManifest]
  - Replace handleExportData with modal flow
  - Add <ExportConfirmationModal /> component
  - Add <ExportManifestDisplay /> component
```

### Files NOT Changed

```
src/service/lgpd.service.ts (API call unchanged)
src/config/api-routes.ts (Routes unchanged)
src/config/api.ts (Axios config unchanged)
Backend Java files (NO CHANGES)
```

---

## Deployment Impact

### Frontend
- ✅ No new environment variables
- ✅ No new dependencies
- ✅ No API contract changes
- ✅ No database migrations
- ✅ Full backward compatible

### Backend
- ✅ Zero changes
- ✅ Existing export endpoint works as before
- ✅ No load impact
- ✅ No storage changes

---

## Rollback Plan

If issues occur:

1. **Remove Modal Component**: Delete ExportConfirmationModal.tsx
2. **Revert PrivacyCenter**: Restore original handleExportData (direct download)
3. **Keep Manifest Display**: Optional, can remain as it's informational only
4. **No Backend Changes**: Nothing to rollback on backend

---

## Success Criteria

✅ Modal appears before export  
✅ Users cannot accidentally download  
✅ Export data includes the same content as before  
✅ Post-export summary displays safely  
✅ All tests pass  
✅ No breaking changes to API contract  
✅ No new sensitive data logged  

---

## Security Checklist

- ✅ Modal text contains NO actual user data
- ✅ Summary toast contains NO personal information
- ✅ No CPF/PIS/Email logged in console
- ✅ No base64 data exposed in UI
- ✅ No token/session data in UI
- ✅ Export flow maintains tenant isolation (employeeId from auth context)
- ✅ No new audit logging needed (export already logged on backend)

---

## Performance Impact

- ✅ No new API calls (uses existing exportEmployeeData)
- ✅ Modal component lightweight (< 5KB minified)
- ✅ Summary component lightweight (< 2KB minified)
- ✅ No database queries added
- ✅ No memory leaks (proper cleanup on unmount)

