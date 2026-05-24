# Production Security Properties Validator

## Overview

`ProductionSecurityPropertiesValidator` is a Spring Boot component that automatically validates critical security configurations at application startup when running in the `prod` profile. It enforces LGPD compliance and security best practices.

**Location**: `src/main/java/com/kts/kronos/config/ProductionSecurityPropertiesValidator.java`

---

## Validation Flow

The validator runs on application startup via `@EventListener(ApplicationReadyEvent.class)`:

1. **Checks if production profile is active** → if not, validation is skipped with debug log
2. **Runs 7 mandatory validation checks** in sequence
3. **Throws `IllegalStateException`** if any mandatory check fails
4. **Logs warnings** for non-blocking issues (e.g., missing AWS credentials)
5. **Logs success messages** for each validated component

```
Application Startup
    ↓
ApplicationReadyEvent triggered
    ↓
ProductionSecurityPropertiesValidator.validateProductionConfiguration()
    ↓
if (!isProduction) return;  // Skip if not prod profile
    ↓
validateCookieSecurity() → Cookie flags checked
validateCORS() → Origins validated
validateSwagger() → UI disabled check
validateJwtSecret() → Secret length check
validateAwsCredentials() → AWS setup (warns on missing)
validateActuatorEndpoints() → Endpoints validated
validateAntivirus() → Upload scanning check
    ↓
Success: All validations passed → Application starts
OR
Failure: Validation failed → IllegalStateException thrown → Application stops
```

---

## Validation Rules by Component

### 1. Cookie Security (`validateCookieSecurity`)

**Mandatory Properties**:
- `kronos.security.auth-cookie.secure` = `true`
- `kronos.security.auth-cookie.http-only` = `true`

**Rules**:
- ✅ Cookies transmitted only via HTTPS (`secure=true`)
- ✅ Cookies inaccessible to JavaScript (`http-only=true`)
- ❌ Fails if either flag is `false` in production

**Error Message**:
```
SECURITY ERROR: Authentication cookies are not secure in production. 
Set kronos.security.auth-cookie.secure=true
```

---

### 2. CORS Validation (`validateCORS`)

**Mandatory Property**:
- `frontend.allowed-origins` (comma-separated HTTPS domains)

**Format Rules** (all must pass):
- ✅ **HTTPS only**: Every origin must start with `https://`
- ✅ **No wildcards**: `*` and `*.domain.com` are rejected
- ✅ **No paths**: Origins must not contain `/path`
- ✅ **No query strings**: Origins must not contain `?param=value`
- ✅ **No fragments**: Origins must not contain `#section`
- ✅ **No spaces**: Internal whitespace causes rejection
- ✅ **Valid URLs**: Malformed URLs are rejected via `java.net.URL` parsing
- ✅ **Valid host**: Origin must have a valid hostname
- ✅ **Non-empty list**: Configuration cannot be empty

**Valid Examples**:
```
https://kronostechsolutions.com
https://www.kronostechsolutions.com
https://app.kronostechsolutions.com
https://kronostechsolutions.com:8443
https://kronostechsolutions.com,https://www.kronostechsolutions.com
```

**Invalid Examples** (all rejected):
```
*                                                    # Full wildcard
*.kronostechsolutions.com                           # Partial wildcard
http://kronostechsolutions.com                      # HTTP not HTTPS
kronostechsolutions.com                             # No scheme
https://kronostechsolutions.com/path                # Contains path
https://kronostechsolutions.com?x=1                 # Contains query
https://kronostechsolutions.com#section             # Contains fragment
https://krono tech.com                              # Contains space
https://kronostechsolutions.com,,https://www...    # Empty value in list
https://[invalid                                    # Malformed URL
```

**Whitespace Handling**:
- Whitespace is trimmed from each origin before validation
- `" https://domain.com , https://www.domain.com "` → Valid

---

### 3. Swagger UI (`validateSwagger`)

**Mandatory Property**:
- `springdoc.swagger-ui.enabled` = `false`

**Rules**:
- ✅ Swagger UI must be disabled in production
- ❌ Fails if `enabled=true`

**Rationale**: Swagger exposes API documentation and examples that could aid attackers.

---

### 4. JWT Secret (`validateJwtSecret`)

**Mandatory Property**:
- `jwt.secret` (minimum 32 characters)

**Rules**:
- ✅ Secret must be configured
- ✅ Secret must be at least 32 characters long
- ❌ Fails if absent or shorter than 32 characters

**Example**:
```
jwt.secret=this-is-a-very-secure-secret-with-32-characters
```

---

### 5. AWS Credentials (`validateAwsCredentials`)

**Related Properties**:
- `aws.access-key-id`
- `aws.secret-access-key`
- `aws.region`

**Behavior**:
- ⚠️ **Warning only** (does not fail validation)
- Logs warning if access key or secret key is missing
- Logs warning if region is not configured
- Does not block application startup if AWS is unavailable

**Note**: S3 operations will fail at runtime if credentials are not configured, but application will start.

---

### 6. Actuator Endpoints (`validateActuatorEndpoints`)

**Mandatory Property**:
- `management.endpoints.web.exposure.include` (comma-separated list)

**Endpoints That Are Logged as Warnings**:
- ⚠️ `env` - Exposes environment variables
- ⚠️ `heapdump` - Allows memory dump downloads
- ⚠️ `configprops` - Shows all configuration properties
- ⚠️ `*` (wildcard) - Exposes all endpoints

**Safe Endpoints**:
- ✅ `health` - Application health status
- ✅ `info` - Application metadata
- ✅ `metrics` - Application metrics

**Recommended Configuration**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

### 7. Antivirus Protection (`validateAntivirus`)

**Mandatory Property**:
- `kronos.security.upload.antivirus.enabled` = `true`

**Rules**:
- ✅ Upload antivirus must be enabled in production
- ❌ Fails if `enabled=false`

**Rationale**: All user uploads must be scanned for malware before storage.

---

## Environment Variables Reference

| Property Name | Environment Variable | Description | Required | Default |
|---|---|---|---|---|
| `kronos.security.auth-cookie.secure` | `KRONOS_SECURITY_AUTH_COOKIE_SECURE` | HTTPS-only cookies | ✅ Prod | `false` |
| `kronos.security.auth-cookie.http-only` | `KRONOS_SECURITY_AUTH_COOKIE_HTTP_ONLY` | JS-inaccessible cookies | ✅ Prod | `false` |
| `kronos.security.auth-cookie.same-site` | `KRONOS_SECURITY_AUTH_COOKIE_SAME_SITE` | CSRF protection (Strict/Lax) | ⚠️ Recommended | `Lax` |
| `kronos.security.auth-cookie.domain` | `KRONOS_SECURITY_AUTH_COOKIE_DOMAIN` | Cookie domain scope | ⚠️ Recommended | - |
| `frontend.allowed-origins` | `FRONTEND_ALLOWED_ORIGINS` | CORS domains (comma-separated) | ✅ Prod | `*` |
| `jwt.secret` | `JWT_SECRET` | JWT signing secret (min 32 chars) | ✅ Prod | - |
| `jwt.expiration` | `JWT_EXPIRATION` | Access token TTL (seconds) | ⚠️ Recommended | `3600` |
| `jwt.refresh-expiration` | `JWT_REFRESH_EXPIRATION` | Refresh token TTL (seconds) | ⚠️ Recommended | `604800` |
| `springdoc.swagger-ui.enabled` | `SPRINGDOC_SWAGGER_UI_ENABLED` | Swagger UI visibility | ✅ Prod | `true` |
| `springdoc.api-docs.enabled` | `SPRINGDOC_API_DOCS_ENABLED` | OpenAPI schema visibility | ✅ Prod | `true` |
| `management.endpoints.web.exposure.include` | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator endpoints (comma-separated) | ✅ Prod | - |
| `kronos.security.upload.antivirus.enabled` | `KRONOS_SECURITY_UPLOAD_ANTIVIRUS_ENABLED` | Antivirus scanning | ✅ Prod | `false` |
| `aws.access-key-id` | `AWS_ACCESS_KEY_ID` | AWS authentication | ⚠️ For S3 | - |
| `aws.secret-access-key` | `AWS_SECRET_ACCESS_KEY` | AWS authentication | ⚠️ For S3 | - |
| `aws.region` | `AWS_REGION` | AWS region (e.g., us-east-1) | ⚠️ For S3 | - |

---

## Profile-Based Behavior

### Production Profile (`prod`)

- **All 7 validations enforced** as mandatory checks
- ❌ Application **fails to start** if any mandatory validation fails
- Cookie security, CORS, Swagger, JWT, Antivirus are hard stops
- AWS and Actuator endpoints trigger warnings only

### Non-Production Profiles (`dev`, `local`, `test`)

- **All validations skipped** (no checks performed)
- ✅ Application starts regardless of configuration
- Allows flexible development/testing setup

---

## Implementation Details

### Component Lifecycle

```java
@Component
@Slf4j
public class ProductionSecurityPropertiesValidator {
    
    // 1. Constructor: Captures production profile status
    public ProductionSecurityPropertiesValidator(Environment environment) {
        this.environment = environment;
        this.isProduction = Arrays.asList(environment.getActiveProfiles())
            .contains("prod");
    }
    
    // 2. Startup listener: Runs validation on app ready
    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionConfiguration() {
        if (!isProduction) return;  // Skip non-prod
        
        // Run 7 validations in sequence
        validateCookieSecurity();   // Hard stop
        validateCORS();              // Hard stop
        validateSwagger();           // Hard stop
        validateJwtSecret();         // Hard stop
        validateAwsCredentials();    // Warning only
        validateActuatorEndpoints(); // Warning only
        validateAntivirus();         // Hard stop
    }
}
```

### CORS Validation Algorithm

```
validateCORS(corsAllowedOrigins):
  1. Check if origins string is empty → FAIL
  2. Split by comma
  3. For each origin:
    a. Trim whitespace
    b. Check if empty → FAIL
    c. Check if contains "*" → FAIL
    d. Check if contains spaces → FAIL
    e. Check if starts with "https://" → FAIL
    f. Parse as URL object:
       - Validate scheme (https) → FAIL
       - Validate host exists → FAIL
       - Validate no path (except "/") → FAIL
       - Validate no query string → FAIL
       - Validate no fragment → FAIL
    g. Catch MalformedURLException → FAIL
  4. All origins valid → PASS
```

---

## Deployment Checklist

### Before Deploying to Production

- [ ] `kronos.security.auth-cookie.secure=true`
- [ ] `kronos.security.auth-cookie.http-only=true`
- [ ] `frontend.allowed-origins` set to specific HTTPS domains
- [ ] `jwt.secret` configured with 32+ character random string
- [ ] `springdoc.swagger-ui.enabled=false`
- [ ] `springdoc.api-docs.enabled=false`
- [ ] `management.endpoints.web.exposure.include=health,info`
- [ ] `kronos.security.upload.antivirus.enabled=true`
- [ ] AWS credentials configured (if using S3)
- [ ] Application starts without validation errors

### Post-Deployment Verification

1. Check application logs for validation success message:
   ```
   Production security validation completed successfully
   ```

2. Verify CORS configuration by testing from an allowed origin:
   ```bash
   curl -H "Origin: https://allowed-origin.com" https://api.kronos.com/health
   ```

3. Confirm Swagger is not accessible:
   ```bash
   curl https://api.kronos.com/swagger-ui.html  # Should return 404
   ```

4. Verify actuator endpoints:
   ```bash
   curl https://api.kronos.com/actuator/health  # Should work
   curl https://api.kronos.com/actuator/env      # Should fail or require auth
   ```

---

## Troubleshooting

### "Cookie secure not configured in production"
**Fix**: Set `kronos.security.auth-cookie.secure=true`

### "Wildcard CORS origin not allowed in production"
**Fix**: Replace `*` with specific HTTPS domains
```
# Before
FRONTEND_ALLOWED_ORIGINS=*

# After
FRONTEND_ALLOWED_ORIGINS=https://example.com,https://www.example.com
```

### "JWT secret not configured or too short"
**Fix**: Generate 32+ character random string
```bash
openssl rand -base64 32
```

### "Swagger UI is enabled in production"
**Fix**: Set `springdoc.swagger-ui.enabled=false`

### "Actuator /env endpoint is exposed"
**Fix**: Remove `env` from management endpoints
```
management.endpoints.web.exposure.include=health,info
```

### "Antivirus protection is disabled in production"
**Fix**: Set `kronos.security.upload.antivirus.enabled=true`

---

## See Also

- **LGPD Checklist**: `docs/production/lgpd-production-env-checklist.md`
- **Security Configuration**: `docs/security/api-security.md`
- **Compliance Documentation**: `docs/legal/lgpd-compliance.md`
- **Source Code**: `src/main/java/com/kts/kronos/config/ProductionSecurityPropertiesValidator.java`

---

**Last Updated**: 2026-05-24  
**Version**: 1.0  
**Audience**: DevOps, Security Team, Backend Engineers
