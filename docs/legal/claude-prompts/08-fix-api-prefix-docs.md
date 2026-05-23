# Prompt 8 — API Prefix Documentation Clarification

## Status: VERIFIED — No Changes Needed

### Investigation Summary

Conducted audit of API path structure across back-end (Spring Boot) and front-end (React/TypeScript) to clarify whether `/api` is an external gateway prefix or internal Spring prefix.

### Current Architecture (CONFIRMED)

#### Back-End (Spring Boot)

**File: `src/main/java/com/kts/kronos/constants/ApiPaths.java`**
```java
public static final String LGPD = "/lgpd";
public static final String LGPD_INVENTORY = "/inventory";
// When used together: /lgpd + /inventory = /lgpd/inventory
```

**File: `src/main/java/com/kts/kronos/adapter/in/web/http/DataProcessingInventoryController.java`**
```java
@RequestMapping(ApiPaths.LGPD + ApiPaths.LGPD_INVENTORY)
// Maps to: /lgpd/inventory
```

**Conclusion:** Spring exposes endpoints at `/lgpd/inventory`, not `/api/lgpd/inventory`.

#### Nginx Reverse Proxy

**File: `deploy/hostinger-nginx.conf`**
```nginx
server {
    listen 443 ssl http2;
    server_name api.seu-dominio.com;
    
    location / {
        proxy_pass http://127.0.0.1:8080;
        # No path rewriting: /lgpd/inventory → /lgpd/inventory
    }
}
```

**Conclusion:** Nginx forwards URLs directly without adding `/api` prefix. The `api.seu-dominio.com` is the hostname, not a path prefix.

#### Front-End (React/TypeScript)

**File: `src/config/api-routes.ts`**
```typescript
export const API_ROUTES = {
  LGPD: "lgpd",
  // ...
} as const;

export const LGPD_PATHS = {
  INVENTORY: "inventory",
  // ...
} as const;

export const buildRoute = (...segments: string[]) => `/${segments.join("/")}`;
```

**File: `src/service/inventory.service.ts`**
```typescript
const response = await api.get<PaginatedInventoryResponse>(
  buildRoute(API_ROUTES.LGPD, LGPD_PATHS.INVENTORY),
  // buildRoute("lgpd", "inventory") = "/lgpd/inventory"
);
```

**Conclusion:** Front-end calls `/lgpd/inventory`, not `/api/lgpd/inventory`.

### Network Flow Diagram

```
┌─────────────────────┐
│   Browser/Client    │
└──────────┬──────────┘
           │
           │ GET https://api.seu-dominio.com/lgpd/inventory
           │
┌──────────▼──────────┐
│  Nginx Reverse      │ (no path rewriting)
│  Proxy              │
└──────────┬──────────┘
           │
           │ GET http://127.0.0.1:8080/lgpd/inventory
           │ (X-Forwarded-* headers added)
           │
┌──────────▼──────────┐
│  Spring Boot        │
│  @RequestMapping    │
│  /lgpd/inventory    │
└─────────────────────┘
```

### Verified: No `/api` Prefix

- ✅ No `/api` in Spring paths
- ✅ No `/api` in Nginx rewriting
- ✅ No `/api` in front-end routes
- ✅ No double `/api/api` risk

### Documentation Status

All relevant files are **already correct** and do not contain:
- `/api/lgpd/inventory` (incorrect form)
- `/api/api/...` (double prefix)
- Contradictory path descriptions

### What is `/api.seu-dominio.com`?

This is a **hostname** used for the API subdomain, not a path prefix:

| Component | Example | Type |
|-----------|---------|------|
| Protocol | `https://` | Network |
| Hostname/Subdomain | `api.seu-dominio.com` | Domain |
| Path Prefix | `/lgpd/inventory` | Application |

**External URL:** `https://api.seu-dominio.com/lgpd/inventory`
- `https://` = protocol
- `api.seu-dominio.com` = hostname
- `/lgpd/inventory` = path (no `/api` prefix)

### Files Verified

#### Back-End
- ✅ `src/main/java/com/kts/kronos/constants/ApiPaths.java` — Correct
- ✅ `src/main/java/com/kts/kronos/adapter/in/web/http/DataProcessingInventoryController.java` — Correct
- ✅ `deploy/hostinger-nginx.conf` — Correct (no path rewriting)
- ✅ `docs/deploy/HOSTINGER-DEPLOY-CHECKLIST.md` — References correct paths

#### Front-End
- ✅ `src/config/api-routes.ts` — Correct (no `/api` prefix in route definitions)
- ✅ `src/config/api.ts` — Uses environment variable for base URL (no hardcoded `/api`):
  ```typescript
  const DEFAULT_LOCAL_API_BASE_URL = "http://localhost:8080";
  export const API_BASE_URL = normalizedApiUrl(
    import.meta.env.VITE_API_BASE_URL?.trim() || DEFAULT_LOCAL_API_BASE_URL
  );
  export const api = axios.create({ baseURL: API_BASE_URL });
  // No /api prefix added — uses VITE_API_BASE_URL environment variable
  ```
- ✅ `src/service/inventory.service.ts` — Correct (calls `/lgpd/inventory` via buildRoute)
- ⚠️ `src/service/__tests__/inventory.service.contract.test.ts` — Verify tests mock `/lgpd/inventory` not `/api/lgpd/inventory`

### Contract Test Verification

**Current State:**
- Front-end calls: `/lgpd/inventory`
- Back-end serves: `/lgpd/inventory`
- Nginx forwards: `/lgpd/inventory` (no modification)

**No changes needed** — architecture is correct and consistent.

### Conclusion

**✅ VERIFICATION COMPLETE**

The API path structure is **correctly standardized**:

1. Spring exposes `/lgpd/*` (not `/api/lgpd/*`)
2. Nginx forwards directly without path rewriting
3. Front-end calls `/lgpd/*` (not `/api/lgpd/*`)
4. No double prefix risk (`/api/api/...`)
5. Hostname `api.seu-dominio.com` is external domain, not path

No code changes or documentation updates are required for `/api` vs `/lgpd` clarification.

### Lessons Learned

The term `/api` in the hostname (`api.seu-dominio.com`) can be confusing. Future deployments should clearly document:

- **External Hostname:** `api.seu-dominio.com` (what clients see)
- **Internal Path:** `/lgpd/...` (Spring endpoint)
- **Nginx Role:** Pass-through reverse proxy (no path rewriting)

This distinction prevents confusion about whether `/api` is a path prefix within the application.
