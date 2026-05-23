# LGPD API Contract & Workflow

## API Architecture: Internal vs External Paths

### Path Standardization Pattern

Kronos implements a standard API gateway pattern for LGPD endpoints:

```
┌────────────────────────────────────────────────────────────┐
│  External Client (Web/Mobile)                              │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ HTTPS: /api/lgpd/**
                 │
┌────────────────▼─────────────────────────────────────────┐
│  Nginx Reverse Proxy (API Gateway)                        │
│  server_name: api.seu-dominio.com                        │
│  Rewrite: /api/lgpd/** → /lgpd/**                       │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ HTTP: /lgpd/**
                 │
┌────────────────▼─────────────────────────────────────────┐
│  Spring Boot Backend                                       │
│  @RequestMapping("/lgpd/**")                              │
│  Internal HTTP: /lgpd/inventory, /lgpd/requests, etc.    │
└─────────────────────────────────────────────────────────┘
```

### URL Examples

| Concept | URL | Component |
|---------|-----|-----------|
| **External URL** | `https://api.seu-dominio.com/api/lgpd/inventory` | What clients call |
| **Nginx Rewrites** | `/api/lgpd/inventory` → `/lgpd/inventory` | Path transformation |
| **Internal Path** | `/lgpd/inventory` | Spring endpoint |

### Why This Pattern?

1. **API Versioning**: `/api/v1/`, `/api/v2/` easy to implement
2. **Service Routing**: Multiple backends can be routed via gateway
3. **Feature Flags**: Different path versions for canary deployments
4. **Clear Boundaries**: Explicit distinction between public API and internal services

## Endpoint Contract

### LGPD Inventory Endpoints

#### List All Inventories

**External (Client Request):**
```
GET /api/lgpd/inventory?page=0&size=10
Host: api.seu-dominio.com
```

**Internal (Spring Receives):**
```
GET /lgpd/inventory?page=0&size=10
Host: 127.0.0.1:8080
X-Forwarded-Host: api.seu-dominio.com
X-Forwarded-Proto: https
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "inventoryId": "uuid-1",
      "processCode": "DPI-001",
      "processName": "Data Processing Inventory",
      "dataCategory": "employee_data",
      "totalElements": 100,
      "totalPages": 10,
      "currentPage": 0,
      "size": 10
    }
  ]
}
```

#### Get Inventory by Process Code

**External:**
```
GET /api/lgpd/inventory/{processCode}
Host: api.seu-dominio.com
```

**Internal:**
```
GET /lgpd/inventory/{processCode}
Host: 127.0.0.1:8080
```

#### Create Inventory

**External:**
```
POST /api/lgpd/inventory
Host: api.seu-dominio.com
Content-Type: application/json
X-CSRF-Token: [token]

{
  "processCode": "DPI-NEW",
  "processName": "New Process",
  "dataCategory": "employee_data",
  "sensitiveData": true,
  ...
}
```

**Internal:**
```
POST /lgpd/inventory
Host: 127.0.0.1:8080
```

#### Update Inventory

**External:**
```
PATCH /api/lgpd/inventory/{inventoryId}
Host: api.seu-dominio.com
Content-Type: application/json
X-CSRF-Token: [token]

{ "processName": "Updated Name", ... }
```

**Internal:**
```
PATCH /lgpd/inventory/{inventoryId}
Host: 127.0.0.1:8080
```

### LGPD Request Workflow Endpoints

#### Create LGPD Request

**External:**
```
POST /api/lgpd/requests
Host: api.seu-dominio.com
```

#### Get Request Details

**External:**
```
GET /api/lgpd/requests/{requestId}
Host: api.seu-dominio.com
```

#### Transition Request Status

**External:**
```
POST /api/lgpd/admin/requests/{requestId}/transition-status
Host: api.seu-dominio.com
```

#### Get Anonymization Result

**External:**
```
GET /api/lgpd/admin/requests/{requestId}/anonymization-result
Host: api.seu-dominio.com
```

## HTTP Headers

### Request Headers (Nginx → Spring)

Nginx adds forwarding headers so Spring can reconstruct the original request:

```
X-Forwarded-For: 203.0.113.45          # Original client IP
X-Forwarded-Host: api.seu-dominio.com  # Original hostname
X-Forwarded-Proto: https                # Original protocol
X-Forwarded-Port: 443                   # Original port
X-Real-IP: 203.0.113.45                 # Direct client IP
X-Original-URI: /api/lgpd/inventory    # Original request path (before rewrite)
```

### CORS Headers (Spring → Client)

If CORS is enabled:

```
Access-Control-Allow-Origin: https://app.seu-dominio.com
Access-Control-Allow-Methods: GET, POST, PATCH, DELETE
Access-Control-Allow-Headers: Content-Type, X-CSRF-Token, X-Correlation-Id
Access-Control-Allow-Credentials: true
```

## Client Integration

### Front-End Configuration

**Environment Setup (.env.production):**
```env
VITE_API_BASE_URL=https://api.seu-dominio.com
```

**Axios Base Configuration:**
```typescript
// src/config/api.ts
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const api = axios.create({ baseURL: API_BASE_URL });
```

**Route Definition:**
```typescript
// src/config/api-routes.ts
export const LGPD_PATHS = {
  INVENTORY: "api/lgpd/inventory",  // Includes external /api prefix
  // ...
};
```

**Service Usage:**
```typescript
// src/service/inventory.service.ts
const response = await api.get(`/${LGPD_PATHS.INVENTORY}`);
// Resolves to: https://api.seu-dominio.com/api/lgpd/inventory
```

## Testing & Validation

### Contract Test Example

```typescript
// src/service/__tests__/inventory.service.contract.test.ts
describe("Inventory Service - API Contract", () => {
  it("should call /api/lgpd/inventory endpoint", async () => {
    const mockResponse = { content: [], totalElements: 0, currentPage: 0, totalPages: 0, size: 10 };
    
    mock.onGet("/api/lgpd/inventory").reply(200, mockResponse);
    
    const result = await listInventories();
    
    expect(result).toEqual(mockResponse);
  });

  it("should call /api/lgpd/inventory/{processCode} endpoint", async () => {
    const mockResponse = { inventoryId: "123", processCode: "DPI-001", ... };
    
    mock.onGet("/api/lgpd/inventory/DPI-001").reply(200, mockResponse);
    
    const result = await getInventoryByProcessCode("DPI-001");
    
    expect(result).toEqual(mockResponse);
  });
});
```

### Nginx Configuration Test

```bash
# Test rewrite rule (simulate request)
curl -v https://api.seu-dominio.com/api/lgpd/inventory

# Should see:
# > GET /api/lgpd/inventory HTTP/2
# < HTTP/2 200
# (Nginx rewrites internally to /lgpd/inventory)
```

### Integration Test

```bash
# End-to-end test of full path
curl -X GET \
  "https://api.seu-dominio.com/api/lgpd/inventory?page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}"

# Expected: 200 OK with inventory list
```

## Deployment Checklist

### Nginx Configuration

- [ ] Review `deploy/hostinger-nginx.conf` for `/api/lgpd` rewrite rules
- [ ] Verify SSL certificates are installed for `api.seu-dominio.com`
- [ ] Test rewrite rules: `/api/lgpd/** → /lgpd/**`
- [ ] Verify headers (X-Forwarded-*) are passed to backend
- [ ] Confirm no `/api/api` double-prefix occurs
- [ ] Load test to verify path rewriting performance

### Back-End Configuration

- [ ] Spring beans use `@RequestMapping(ApiPaths.LGPD + ...)`
- [ ] LGPD paths start with `/lgpd` not `/api/lgpd`
- [ ] Test `/lgpd/inventory` endpoint directly (via localhost:8080)
- [ ] Verify internal path works independently of external gateway

### Front-End Configuration

- [ ] `VITE_API_BASE_URL=https://api.seu-dominio.com` in production
- [ ] All LGPD routes prefixed with `/api/lgpd/`
- [ ] No hardcoded paths (use `LGPD_PATHS` constants)
- [ ] Contract tests mock `/api/lgpd/**` URLs
- [ ] Staging/QA use correct API gateway URL

### Integration Testing

- [ ] End-to-end test of `https://api.seu-dominio.com/api/lgpd/inventory`
- [ ] Verify CSRF tokens work across Nginx boundary
- [ ] Verify CORS headers are correct (if single-origin policy)
- [ ] Test authentication/authorization flow
- [ ] Verify correlation IDs propagate correctly
- [ ] Load test full pipeline (client → nginx → spring)

## Troubleshooting

### Path Not Found (404)

**Symptom:** `GET /api/lgpd/inventory` returns 404

**Checklist:**
1. Verify Nginx location rule matches: `location ~ ^/api/lgpd/(.*)$`
2. Check rewrite rule: `rewrite ^/api/lgpd/(.*)$ /lgpd/$1 break;`
3. Verify backend is listening on `/lgpd/inventory` (not `/api/lgpd/inventory`)
4. Check Nginx error logs: `tail -f /var/log/nginx/error.log`

### Double Prefix (/api/api)

**Symptom:** Client sees `/api/api/lgpd/inventory` in error messages

**Cause:** Front-end is calling `/api/...` AND Nginx is adding `/api` again

**Fix:**
- Front-end should call `/api/lgpd/...` (NOT `/lgpd/...`)
- Nginx rewrites `/api/lgpd/**` → `/lgpd/**` (removes `/api`)
- Backend serves `/lgpd/**` (no `/api` prefix)

### Headers Not Forwarded

**Symptom:** Backend receives wrong hostname in request

**Check:**
```nginx
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Real-IP $remote_addr;
```

All required headers must be present in Nginx config.

## References

- [RFC 3986: URI Generic Syntax](https://tools.ietf.org/html/rfc3986)
- [HTTP Header X-Forwarded-*](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For)
- [Nginx HTTP Rewrite Module](https://nginx.org/en/docs/http/ngx_http_rewrite_module.html)
- [CORS with API Gateway](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
