# CSRF Protection Policy

## Overview

Cross-Site Request Forgery (CSRF) protection is implemented through cookie SameSite attribute and JWT token validation in request headers.

## Strategy

### SameSite Cookie Protection

The primary CSRF protection mechanism is the `SameSite=Lax` cookie attribute:

- **Lax Mode**: Cookies are sent with top-level navigations but not cross-site requests
- **Effective Against**: Form-based CSRF attacks
- **Transparent To**: Legitimate same-site navigation and API calls

### JWT Token Validation

Secondary CSRF protection through JWT validation:

- Tokens must be read from cookies (not body)
- Tokens are validated on every protected request
- Token claims are verified against request context

## Implementation

### Cookie Configuration

```java
setCookie("KRONOS_ACCESS_TOKEN", token,
    setSecure(true),
    setHttpOnly(true),
    setSameSite("Lax"),
    setMaxAge(900)
)
```

### Request Validation

All state-changing requests (POST, PATCH, PUT, DELETE) require:

1. Valid Authorization header or Cookie with JWT
2. Token claims must match request scope
3. Origin header validation for cross-origin requests

## Protected Endpoints

All endpoints that modify state are protected:

- User management (POST, PATCH, DELETE)
- Document upload/download/delete
- Time record operations
- Company settings
- Message operations
- Terms acceptance

## Safe Methods

GET requests are considered safe and don't require additional CSRF protection beyond authentication.

## Testing CSRF Protection

To test CSRF protection:

```bash
# This should fail - missing token
curl -X PATCH http://localhost:8080/users/123 \
  -H "Content-Type: application/json" \
  -d '{"username":"new"}'

# This should succeed - valid token
curl -X PATCH http://localhost:8080/users/123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"username":"new"}'
```

## Third-Party Integration

If integrating with external services:

- Use server-to-server authentication (not cookie-based)
- Implement webhook signature verification
- Use temporary, scoped access tokens

## Monitoring & Alerts

Log and alert on:

- Missing or invalid CSRF tokens
- Origin header mismatches
- Token validation failures on state-changing requests
- Suspicious patterns of failed CSRF checks

## Future Enhancements

- Implement CSRF token rotation on sensitive operations
- Add anti-CSRF JavaScript library for SPA hardening
- Implement double-submit cookie pattern for additional protection
