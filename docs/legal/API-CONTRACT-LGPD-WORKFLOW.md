# LGPD Request Workflow API Contract
## Endpoint Specifications & Examples

**Version:** 1.0  
**Date:** 2026-05-22  
**Status:** Production Ready

---

## Base URL

```
https://kronos-api.example.com/api/lgpd
```

---

## Authentication

All endpoints require JWT Bearer token in Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Content Type

All requests and responses use:

```
Content-Type: application/json
Charset: UTF-8
```

---

## 1. Status Transition Endpoint

### Endpoint
```
POST /admin/requests/{requestId}/transition-status
```

### Authorization

```
@PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
```

- **CTO:** Can transition any request globally
- **MANAGER:** Can transition requests in own company only

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| requestId | UUID | Yes | ID of the LGPD request to transition |

### Request Body

```json
{
  "newStatus": "IN_ANALYSIS",
  "publicNotes": "Starting analysis phase",
  "internalNotes": "Internal tracking info",
  "closedReason": null
}
```

**Schema:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| newStatus | LgpdRequestStatus | Yes | Valid enum value | Target status for transition |
| publicNotes | String | Conditional | Max 5000 chars | Required for COMPLETED/PARTIALLY_COMPLETED |
| internalNotes | String | No | Max 1000 chars | Internal notes not sent to employee |
| closedReason | String | Conditional | Max 255 chars | Required only for REJECTED status |

### Valid Status Transitions

```
OPEN:
  ├─ IN_ANALYSIS
  ├─ REJECTED (requires closedReason)
  └─ CANCELLED (CTO-only)

IN_ANALYSIS:
  ├─ WAITING_CONTROLLER
  ├─ REJECTED (requires closedReason)
  └─ CANCELLED (CTO-only)

WAITING_CONTROLLER:
  ├─ WAITING_LEGAL_REVIEW
  ├─ REJECTED (requires closedReason)
  └─ CANCELLED (CTO-only)

WAITING_LEGAL_REVIEW:
  ├─ WAITING_DATA_SUBJECT
  ├─ COMPLETED (requires publicNotes)
  ├─ PARTIALLY_COMPLETED (requires publicNotes)
  ├─ REJECTED (requires closedReason)
  └─ CANCELLED (CTO-only)

WAITING_DATA_SUBJECT:
  ├─ IN_ANALYSIS
  ├─ COMPLETED (requires publicNotes)
  ├─ PARTIALLY_COMPLETED (requires publicNotes)
  ├─ REJECTED (requires closedReason)
  └─ CANCELLED (CTO-only)

COMPLETED, REJECTED, PARTIALLY_COMPLETED, CANCELLED:
  └─ (no transitions allowed - terminal states)
```

### Example Request

```bash
curl -X POST \
  https://kronos-api.example.com/api/lgpd/admin/requests/550e8400-e29b-41d4-a716-446655440000/transition-status \
  -H 'Authorization: Bearer {jwt_token}' \
  -H 'Content-Type: application/json' \
  -d '{
    "newStatus": "IN_ANALYSIS",
    "publicNotes": "Request received and sent to analysis team",
    "internalNotes": "High priority - CEO request",
    "closedReason": null
  }'
```

### Success Response (200 OK)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "employeeId": "660e8400-e29b-41d4-a716-446655440001",
  "requestedByUserId": "770e8400-e29b-41d4-a716-446655440002",
  "companyId": "880e8400-e29b-41d4-a716-446655440003",
  "requestType": "ACCESS",
  "status": "IN_ANALYSIS",
  "description": "Request for access to personal data",
  "resolutionNotes": "Request received and sent to analysis team",
  "createdAt": "2026-05-20T10:00:00Z",
  "updatedAt": "2026-05-22T14:30:00Z",
  "resolvedAt": null,
  "resolvedByUserId": null
}
```

### Error Responses

#### 400 Bad Request - Missing Required Field
```json
{
  "error": "Validation Error",
  "message": "newStatus is required",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 400 Bad Request - Invalid Status Transition
```json
{
  "error": "Invalid Transition",
  "message": "Cannot transition from OPEN to WAITING_CONTROLLER. Valid transitions: [IN_ANALYSIS, REJECTED, CANCELLED]",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 400 Bad Request - Missing Mandatory Notes
```json
{
  "error": "Validation Error",
  "message": "publicNotes are required for COMPLETED status",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 400 Bad Request - Missing Rejection Reason
```json
{
  "error": "Validation Error",
  "message": "closedReason is required for REJECTED status",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 403 Forbidden - Insufficient Role
```json
{
  "error": "Access Denied",
  "message": "MANAGER role cannot transition cross-company requests",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 403
}
```

#### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "LGPD Request not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 404
}
```

---

## 2. Request Complement Endpoint

### Endpoint
```
POST /admin/requests/{requestId}/request-complement
```

### Authorization

```
@PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
```

- Requires request to be in WAITING_DATA_SUBJECT status
- MANAGER restricted to own company

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| requestId | UUID | Yes | ID of the LGPD request |

### Request Body

```json
{
  "message": "Please provide proof of residence and official ID copy"
}
```

**Schema:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| message | String | Yes | Min 10, Max 5000 chars | Information requested from employee |

### Example Request

```bash
curl -X POST \
  https://kronos-api.example.com/api/lgpd/admin/requests/550e8400-e29b-41d4-a716-446655440000/request-complement \
  -H 'Authorization: Bearer {jwt_token}' \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "We need proof of your current address (utility bill or lease agreement) to proceed with your data access request"
  }'
```

### Success Response (200 OK)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "employeeId": "660e8400-e29b-41d4-a716-446655440001",
  "requestedByUserId": "770e8400-e29b-41d4-a716-446655440002",
  "companyId": "880e8400-e29b-41d4-a716-446655440003",
  "requestType": "ACCESS",
  "status": "WAITING_DATA_SUBJECT",
  "description": "Request for access to personal data",
  "resolutionNotes": null,
  "createdAt": "2026-05-20T10:00:00Z",
  "updatedAt": "2026-05-22T14:35:00Z",
  "resolvedAt": null,
  "resolvedByUserId": null
}
```

**Side Effect:** Employee receives notification email with complement message

### Error Responses

#### 400 Bad Request - Invalid Status
```json
{
  "error": "Invalid State",
  "message": "Request is not in WAITING_DATA_SUBJECT status. Current status: IN_ANALYSIS",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 400 Bad Request - Message Too Long
```json
{
  "error": "Validation Error",
  "message": "message exceeds maximum length of 5000 characters",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 400 Bad Request - Message Too Short
```json
{
  "error": "Validation Error",
  "message": "message must be at least 10 characters",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 403 Forbidden
```json
{
  "error": "Access Denied",
  "message": "You do not have permission to request complement for this request",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 403
}
```

#### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "LGPD Request not found",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 404
}
```

---

## 3. Cancel Request Endpoint

### Endpoint
```
POST /admin/requests/{requestId}/cancel
```

### Authorization

```
@PreAuthorize("hasRole('CTO')")
```

- **CTO-only operation** - Restricted to CTO role only
- Global access - Can cancel any request

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| requestId | UUID | Yes | ID of the LGPD request to cancel |

### Request Body

```json
{
  "reason": "Duplicate request from same employee"
}
```

**Schema:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| reason | String | Yes | Max 255 chars | Reason for cancellation |

### Example Request

```bash
curl -X POST \
  https://kronos-api.example.com/api/lgpd/admin/requests/550e8400-e29b-41d4-a716-446655440000/cancel \
  -H 'Authorization: Bearer {cto_jwt_token}' \
  -H 'Content-Type: application/json' \
  -d '{
    "reason": "Employee requested to cancel - resolved issue offline"
  }'
```

### Success Response (200 OK)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "employeeId": "660e8400-e29b-41d4-a716-446655440001",
  "requestedByUserId": "770e8400-e29b-41d4-a716-446655440002",
  "companyId": "880e8400-e29b-41d4-a716-446655440003",
  "requestType": "ACCESS",
  "status": "CANCELLED",
  "description": "Request for access to personal data",
  "resolutionNotes": null,
  "createdAt": "2026-05-20T10:00:00Z",
  "updatedAt": "2026-05-22T14:40:00Z",
  "resolvedAt": "2026-05-22T14:40:00Z",
  "resolvedByUserId": "770e8400-e29b-41d4-a716-446655440002"
}
```

### Error Responses

#### 400 Bad Request - Empty Reason
```json
{
  "error": "Validation Error",
  "message": "reason is required",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 400 Bad Request - Reason Too Long
```json
{
  "error": "Validation Error",
  "message": "reason exceeds maximum length of 255 characters",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 400
}
```

#### 403 Forbidden - Not CTO
```json
{
  "error": "Access Denied",
  "message": "Only CTO role can cancel requests",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 403
}
```

#### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "LGPD Request not found",
  "timestamp": "2026-05-22T14:30:00Z",
  "status": 404
}
```

---

## Response Models

### LgpdRequestResponse

```typescript
interface LgpdRequestResponse {
  requestId: UUID;           // Unique request identifier
  employeeId: UUID;          // Employee who initiated request
  requestedByUserId: UUID;   // User who created the request
  companyId: UUID;           // Company context
  requestType: string;       // Type of request (ACCESS, DELETION, etc.)
  status: string;            // Current status (OPEN, IN_ANALYSIS, etc.)
  description: string;       // Request description
  resolutionNotes: string | null;  // Notes on how request was resolved
  createdAt: ISO8601;        // Request creation timestamp
  updatedAt: ISO8601;        // Last update timestamp
  resolvedAt: ISO8601 | null;      // When request was resolved/completed
  resolvedByUserId: UUID | null;   // User who resolved request
}
```

### Error Response

```typescript
interface ErrorResponse {
  error: string;              // Error type
  message: string;            // Human-readable error message
  timestamp: ISO8601;         // When error occurred
  status: number;             // HTTP status code
  path?: string;              // API path that caused error
  details?: object;           // Additional error details
}
```

---

## Notification System Integration

### Notifications Triggered by Endpoints

#### Status Transition Endpoint
- **Notification Type:** STATUS_CHANGED
- **Recipient:** Request creator (employee)
- **Content:** Old status, new status, timestamp
- **Channel:** EMAIL
- **Timing:** Async (sent after endpoint returns)

When transitioning to COMPLETED/PARTIALLY_COMPLETED:
- **Additional Notification:** REQUEST_COMPLETED
- **Content:** Completion notes, data availability

When transitioning to REJECTED:
- **Additional Notification:** REQUEST_REJECTED
- **Content:** Rejection reason, next steps

#### Request Complement Endpoint
- **Notification Type:** COMPLEMENT_REQUESTED
- **Recipient:** Request creator (employee)
- **Content:** Requested information message
- **Channel:** EMAIL
- **Timing:** Async (sent after endpoint returns)

#### Cancel Endpoint
- **Notification Type:** None (cancellation reason in history only)
- **Note:** No notification email sent to employee on cancellation

---

## Rate Limiting

```
Recommended: 100 requests per minute per user
Suggested Implementation: Token bucket algorithm
```

---

## Pagination (for list endpoints)

```
GET /admin/requests?page=0&size=20&status=IN_ANALYSIS&companyId=...

Parameters:
- page (int): 0-based page number
- size (int): Results per page (max 100)
- status (string): Filter by status
- type (string): Filter by request type
- companyId (UUID): Filter by company
```

---

## Status Codes Reference

| Code | Meaning | Scenarios |
|------|---------|-----------|
| 200 | OK | Successful request processing |
| 400 | Bad Request | Validation failed, invalid transition, missing fields |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Insufficient role, cross-company access denied |
| 404 | Not Found | Request ID doesn't exist |
| 500 | Server Error | Internal server error, notification service down |
| 503 | Service Unavailable | Email service unavailable (non-blocking) |

---

## Request/Response Headers

### Request Headers

```
Authorization: Bearer {jwt_token}
Content-Type: application/json
Accept: application/json
X-Request-ID: {uuid}           # Optional: for tracing
X-Correlation-ID: {uuid}       # Optional: for tracking
```

### Response Headers

```
Content-Type: application/json
X-Request-ID: {uuid}
X-Correlation-ID: {uuid}
Cache-Control: no-cache, no-store, must-revalidate
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1621696800
```

---

## Versioning

Current API Version: **1.0**

Future versions will be released as `/v2/`, `/v3/`, etc.

Backward compatibility maintained for existing endpoints.

---

## Changelog

### Version 1.0 (2026-05-22)
- Initial release
- POST /admin/requests/{requestId}/transition-status
- POST /admin/requests/{requestId}/request-complement
- POST /admin/requests/{requestId}/cancel
- Multi-tenant authorization
- Async notification dispatch
- Comprehensive error handling

---

## Support & Issues

For API issues or questions:
- Documentation: /docs/legal/
- Email: support@kronos-tech.com
- Status Page: https://status.kronos-tech.com
