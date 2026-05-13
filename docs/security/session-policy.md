# Session Policy & Cookie Configuration

## Overview

Session management in Kronos is handled through HTTP-only cookies containing JWT tokens. This document defines the security policies and technical implementation for session handling.

## Cookie Configuration

### Security Flags

All session cookies are configured with the following security flags:

- **HttpOnly**: `true` - Prevents JavaScript from accessing the cookie, protecting against XSS attacks
- **Secure**: `true` in production, `false` in development - Ensures cookies are only sent over HTTPS
- **SameSite**: `Lax` - Provides CSRF protection by restricting cross-site cookie transmission

### Cookie Properties

- **Name**: `KRONOS_ACCESS_TOKEN`
- **Max-Age**: 15 minutes (900 seconds)
- **Path**: `/`
- **Domain**: Configured per environment

### JWT Token Details

- **Algorithm**: HS256
- **Expiration**: 15 minutes from issue
- **Refresh**: Cookie is renewed on each authenticated request
- **Claims**: userId, employeeId, username, role, biometricAccepted

## Implementation Details

### Cookie Creation

Cookies are set via `AuthCookieService` during login and after biometric acceptance:

```java
new HttpCookie("KRONOS_ACCESS_TOKEN", token)
    .setHttpOnly(true)
    .setSecure(true)
    .setSameSite("Lax")
    .setMaxAge(900)
    .setPath("/")
```

### Cookie Expiration

On logout, cookies are explicitly expired with max-age=0:

```java
new HttpCookie("KRONOS_ACCESS_TOKEN", "")
    .setMaxAge(0)
```

## Biometric Authorization

When a user successfully completes biometric authorization:

1. Token is regenerated with `biometricAccepted = true`
2. New token is returned in Set-Cookie header
3. Old token becomes invalid

When biometric is revoked:

1. Token is regenerated with `biometricAccepted = false`
2. Biometric routes return 403 Forbidden

## CORS & Origin Validation

- CORS is restricted to the configured frontend domain
- Credentials mode is `include` for cookies
- Pre-flight requests are validated

## Monitoring

Session-related events are logged:

- Login successful/failed
- Biometric acceptance/revocation
- Token validation failures
- Suspicious activity (rapid token changes, failed validations)

## Future Considerations

- Implement refresh token rotation
- Add session revocation list for logout from all devices
- Consider rate limiting on token validation failures
