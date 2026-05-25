# Rate Limits Documentation

## Overview

Kronos implements rate limiting across multiple authentication and security-sensitive operations to protect against brute force attacks and abuse. This document details all rate limit configurations, their default values, and production recommendations.

## Configuration Location

Rate limits are configured in the application configuration files:
- **Default values**: `src/main/resources/application.yml`
- **Production overrides**: `src/main/resources/application-prod.yml`

All rate limit settings can be overridden via environment variables for runtime control without redeployment.

---

## 1. Login Rate Limiting

### 1.1 Login by IP Address

Limits login attempts across all users from a single IP address.

**Configuration Key**: `kronos.security.rate-limit.login.ip`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 10 attempts | `LOGIN_RATE_LIMIT_IP_LIMIT` |
| window | 60 seconds | `LOGIN_RATE_LIMIT_IP_WINDOW_SECONDS` |

**Use Case**: Prevents distributed brute force attacks where multiple accounts are targeted from the same IP.

**Example**:
- Default: 10 login attempts per IP per 60 seconds
- If exceeded: Requests from that IP are rejected with HTTP 429 (Too Many Requests)

---

### 1.2 Login by Username

Limits login attempts on a specific username across all IP addresses.

**Configuration Key**: `kronos.security.rate-limit.login.username`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 5 attempts | `LOGIN_RATE_LIMIT_USERNAME_LIMIT` |
| window | 300 seconds (5 min) | `LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS` |

**Use Case**: Prevents targeted brute force attacks on a specific user account.

**Example**:
- Default: 5 failed login attempts per username per 5 minutes
- If exceeded: Requests for that username are rejected until the window expires

---

### 1.3 Login Cooldown Periods

Progressive cooldown periods after repeated failed login attempts on a single username.

**Configuration Key**: `kronos.security.rate-limit.login.cooldown-minutes`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| cooldown periods | 5, 15, 30 (minutes) | `LOGIN_RATE_LIMIT_COOLDOWN_MINUTES` |

**Use Case**: Implements progressive delays to slow down brute force attacks while allowing legitimate users to retry.

**Example**:
- 1st batch of 5 failures: 5-minute cooldown before retry
- 2nd batch of 5 failures: 15-minute cooldown before retry
- 3rd batch of 5 failures: 30-minute cooldown before retry

**Format**: Comma-separated values (e.g., `5,15,30`)

---

## 2. Password Recovery Rate Limiting

### 2.1 Recovery by CPF

Limits password recovery requests using CPF (Brazilian taxpayer ID).

**Configuration Key**: `kronos.security.rate-limit.recovery.cpf`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 3 requests | `RECOVERY_RATE_LIMIT_CPF_LIMIT` |
| window | 3600 seconds (1 hour) | `RECOVERY_RATE_LIMIT_WINDOW_SECONDS` |

**Use Case**: Prevents flooding recovery mechanisms with invalid CPF lookups.

---

### 2.2 Recovery by Email

Limits password recovery requests using email address.

**Configuration Key**: `kronos.security.rate-limit.recovery.email`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 3 requests | `RECOVERY_RATE_LIMIT_EMAIL_LIMIT` |
| window | 3600 seconds (1 hour) | `RECOVERY_RATE_LIMIT_WINDOW_SECONDS` |

**Use Case**: Prevents email enumeration attacks and spam.

---

### 2.3 Recovery by IP Address

Limits all password recovery requests from a single IP address.

**Configuration Key**: `kronos.security.rate-limit.recovery.ip`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 10 requests | `RECOVERY_RATE_LIMIT_IP_LIMIT` |
| window | 3600 seconds (1 hour) | `RECOVERY_RATE_LIMIT_WINDOW_SECONDS` |

**Use Case**: Protects against mass password reset attempts from malicious IPs.

---

## 3. Admin Operations Rate Limiting

### 3.1 Admin Check

Limits administrative verification operations (e.g., checking admin status, verifying permissions).

**Configuration Key**: `kronos.security.rate-limit.admin-check`

| Property | Default (Dev) | Default (Prod) | Environment Variable |
|----------|--------|--------|----------------------|
| limit | 30 requests | 20 requests | `ADMIN_CHECK_RATE_LIMIT` |
| window | 60 seconds | 60 seconds | `ADMIN_CHECK_RATE_LIMIT_WINDOW_SECONDS` |

**Use Case**: Prevents abuse of admin endpoints and protects sensitive permission checks.

**Production Note**: Set to 20 requests per minute (stricter than development) for tighter security in production.

---

## 4. Biometric Operations Rate Limiting

Biometric operations have separate rate limits to prevent abuse of biometric endpoints while allowing normal user workflows.

### 4.1 Biometric Login (Face Recognition)

Limits login attempts using facial biometric authentication.

**Configuration Key**: `biometric.login-face`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 5 attempts | `BIOMETRIC_LOGIN_FACE_LIMIT` |
| window | 60 seconds | `BIOMETRIC_LOGIN_FACE_WINDOW_SECONDS` |

**Use Case**: Prevents brute force attacks on biometric login while allowing multiple legitimate tries within a reasonable timeframe.

**Example**:
- A user can attempt biometric login up to 5 times per minute
- After exceeding this, the endpoint returns HTTP 429 until the window expires

---

### 4.2 Biometric Check-In

Limits time record check-in operations using biometric authentication.

**Configuration Key**: `biometric.checkin`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 20 attempts | `BIOMETRIC_CHECKIN_LIMIT` |
| window | 60 seconds | `BIOMETRIC_CHECKIN_WINDOW_SECONDS` |

**Use Case**: Allows reasonable retry attempts for biometric check-in while preventing rapid-fire requests.

**Example**:
- Up to 20 check-in attempts per IP per minute
- Higher limit than login due to operational requirements (multiple employees, scanning retries)

---

### 4.3 Biometric Enrollment

Limits new biometric enrollment operations (registering new face templates).

**Configuration Key**: `biometric.enrollment`

| Property | Default | Environment Variable |
|----------|---------|----------------------|
| limit | 10 attempts | `BIOMETRIC_ENROLLMENT_LIMIT` |
| window | 600 seconds (10 min) | `BIOMETRIC_ENROLLMENT_WINDOW_SECONDS` |

**Use Case**: Protects enrollment endpoints from abuse while allowing users to retry enrollment if needed.

**Rationale**: Longer window (10 minutes) allows enrollment to be a more deliberate process than check-in/login attempts.

---

## 5. Environment Variables Summary

### All Rate Limit Environment Variables

```bash
# Login Rate Limiting
LOGIN_RATE_LIMIT_IP_LIMIT=10
LOGIN_RATE_LIMIT_IP_WINDOW_SECONDS=60
LOGIN_RATE_LIMIT_USERNAME_LIMIT=5
LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS=300
LOGIN_RATE_LIMIT_COOLDOWN_MINUTES=5,15,30

# Password Recovery
RECOVERY_RATE_LIMIT_CPF_LIMIT=3
RECOVERY_RATE_LIMIT_EMAIL_LIMIT=3
RECOVERY_RATE_LIMIT_IP_LIMIT=10
RECOVERY_RATE_LIMIT_WINDOW_SECONDS=3600

# Admin Operations
ADMIN_CHECK_RATE_LIMIT=30  # Default: 30, Production: 20
ADMIN_CHECK_RATE_LIMIT_WINDOW_SECONDS=60

# Biometric Operations
BIOMETRIC_LOGIN_FACE_LIMIT=5
BIOMETRIC_LOGIN_FACE_WINDOW_SECONDS=60
BIOMETRIC_CHECKIN_LIMIT=20
BIOMETRIC_CHECKIN_WINDOW_SECONDS=60
BIOMETRIC_ENROLLMENT_LIMIT=10
BIOMETRIC_ENROLLMENT_WINDOW_SECONDS=600
```

---

## 6. Production Recommendations

### 6.1 Stricter Configuration for Production

Production environments should use tighter rate limits than development:

| Endpoint | Development | Production Recommended | Rationale |
|----------|-------------|----------------------|-----------|
| Login IP | 10/min | 5-8/min | Reduce attack surface |
| Login Username | 5/5min | 3/5min | Tighter account protection |
| Admin Check | 30/min | 20/min | Already configured in prod |
| Biometric Login | 5/min | 3-5/min | Reduce biometric endpoint abuse |
| Biometric Checkin | 20/min | 10-15/min | Balance user experience with security |

### 6.2 Monitoring and Alerting

Implement monitoring for:
- **Rate limit exceeded events**: Log all 429 responses for audit trails
- **Brute force patterns**: Alert on sustained rate limiting from specific IPs
- **Geographic anomalies**: Monitor login attempts from unusual locations
- **Failed recovery attempts**: Track excessive password reset attempts

### 6.3 Infrastructure Considerations

- **API Gateway**: Consider implementing additional rate limiting at the API gateway level (WAF, reverse proxy)
- **Load Balancer**: Distribute rate limit tracking across instances using Redis or shared cache
- **Regional Blocking**: For repeated violations, implement temporary IP blocking (e.g., via WAF)

### 6.4 Deployment Checklist

Before production deployment, verify:

```bash
# Check environment variables are set
env | grep -i "rate_limit\|biometric"

# Verify production profile is active
grep SPRING_PROFILES_ACTIVE=prod /etc/kronos/env

# Validate rate limit values
curl http://localhost:8080/actuator/configprops | grep rate-limit

# Test rate limiting works
for i in {1..20}; do
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}' \
    -w "\nStatus: %{http_code}\n"
done
# Should see HTTP 429 after limit exceeded
```

---

## 7. HTTP Status Codes

When rate limits are exceeded, the API returns:

- **HTTP 429 (Too Many Requests)**: Rate limit exceeded
- **Retry-After Header**: Indicates seconds until the next request can be retried

**Example Response**:
```
HTTP/1.1 429 Too Many Requests
Retry-After: 45

{
  "error": "Too many requests",
  "detail": "Rate limit exceeded. Please try again later.",
  "retryAfter": 45
}
```

---

## 8. Implementation Details

### Technology Stack

- **Storage**: In-memory cache (local) or distributed cache (Redis) for rate limit counters
- **Key Format**: `{endpoint}:{identifier}` (e.g., `login:192.168.1.1`, `login:username@example.com`)
- **Expiration**: Automatic key expiration based on window size

### Affected Components

- **AuthService**: Enforces login rate limits before credential validation
- **CompanyService**: Applies admin-check rate limits on company lookups
- **EmployeeService**: Applies recovery rate limits on password reset endpoints
- **BiometricService**: Enforces biometric operation rate limits

---

## 9. Troubleshooting

### Issue: Legitimate Users Blocked

**Symptom**: Valid users getting 429 responses

**Solution**:
1. Check if user's IP is included in trusted proxies
2. Verify window size is appropriate for the use case
3. Consider increasing limit if usage patterns exceed thresholds
4. Implement IP whitelisting for internal/VPN traffic

### Issue: Rate Limits Not Enforcing

**Symptom**: Exceeding limits but still getting 200 responses

**Solution**:
1. Verify environment variables are set: `env | grep RATE_LIMIT`
2. Check application logs for configuration errors
3. Restart application after environment variable changes
4. Confirm correct profile is active (e.g., `-Dspring.profiles.active=prod`)

### Issue: False Positives on Shared Networks

**Symptom**: Multiple legitimate users behind NAT/proxy hit rate limits

**Solution**:
1. Enable header forwarding: `CLIENT_IP_TRUST_FORWARDED_HEADERS=true`
2. Configure trusted proxy CIDRs: `CLIENT_IP_TRUSTED_PROXY_CIDRS=10.0.0.0/8`
3. Use X-Forwarded-For header from reverse proxy
4. Consider implementing user-level rate limiting in addition to IP-level

---

## 10. Changelog

### Version 1.0 (2026-05-24)

- Initial documentation of all rate limit configurations
- Added production recommendations
- Included environment variable reference
- Added troubleshooting guide

---

## References

- **Spring Security**: Rate limit implementation in AuthenticationRateLimitService
- **NIST SP 800-63B**: Password Guidelines and Brute Force Protection
- **OWASP**: Brute Force Protection Best Practices
