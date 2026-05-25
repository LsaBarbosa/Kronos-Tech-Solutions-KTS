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
4. **Logs success messages** for each validated component
5. **No application startup if validation fails**

```
Application Startup
    ↓
ApplicationReadyEvent triggered
    ↓
ProductionSecurityPropertiesValidator.validateProductionConfiguration()
    ↓
if (!isProduction) return;  // Skip if not prod profile
    ↓
validateCookieSecurity() → Cookie security checked
validateCORS() → Origins validated
validateSwagger() → UI disabled check
validateJwtSecret() → Secret length check
validateAwsCredentials() → AWS region + credentials checked
validateActuatorEndpoints() → Sensitive endpoints blocked
validateAntivirus() → Upload scanning check
    ↓
Success: All validations passed → Application starts
OR
Failure: Validation failed → IllegalStateException thrown → Application stops immediately
```

---

## Validation Rules by Component

### 1. Cookie Security (`validateCookieSecurity`)

**Mandatory Properties**:
- `kronos.security.auth-cookie.secure` = `true`

**Rules**:
- ✅ Cookies transmitted only via HTTPS (`secure=true`)
- ✅ HttpOnly flag **hardcoded** in `AuthCookieService.baseCookie()` (cannot be disabled)
- ❌ Fails if `secure=false` in production

**Note on HttpOnly**: The `HttpOnly` flag is automatically set by `AuthCookieService` and cannot be disabled via configuration. This ensures cookies are inaccessible to JavaScript regardless of application configuration.

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
- `aws.region` (mandatory)
- `aws.access-key-id` (optional if using IAM Role)
- `aws.secret-access-key` (optional if using IAM Role)

**Validation Rules**:

#### Region (Always Required)
- ✅ `aws.region` must be configured (e.g., `us-east-1`)
- ❌ Fails if `aws.region` is missing or empty in production

#### Credentials (Two Supported Modes)

**Mode 1: Static Credentials** (explicit keys)
- ✅ Both `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must be provided together
- ❌ Fails if only one is provided without the other
- ✅ S3 and Rekognition operations work immediately with these keys

**Mode 2: IAM Role** (recommended in containers)
- ✅ Neither `AWS_ACCESS_KEY_ID` nor `AWS_SECRET_ACCESS_KEY` provided
- ✅ Application uses EC2 instance profile, ECS task role, or Kubernetes service account (IRSA)
- ❌ Fails if credentials are incomplete (only access key without secret, or vice versa)

**Error Examples**:
```
# Fails: Missing region
SECURITY ERROR: AWS region is not configured in production.

# Fails: Incomplete credentials (only access key)
SECURITY ERROR: AWS credentials are incomplete. Either provide both 
aws.access-key-id and aws.secret-access-key, or use IAM Role (provide neither)

# Passes: Static credentials
✓ AWS credentials mode: Static credentials (access-key-id + secret-access-key)

# Passes: IAM Role mode
✓ AWS credentials mode: IAM Role (will use instance profile, ECS task role, or web identity)
```

---

### 6. Actuator Endpoints (`validateActuatorEndpoints`)

**Mandatory Property**:
- `management.endpoints.web.exposure.include` (comma-separated list)

**Blocked Endpoints (Application Fails to Start)**:
- ❌ `*` (wildcard) - Exposes all endpoints
- ❌ `env` - Exposes environment variables (potential secrets leak)
- ❌ `heapdump` - Allows memory dump downloads (sensitive data)
- ❌ `beans` - Shows all Spring beans
- ❌ `configprops` - Shows all configuration properties
- ❌ `threaddump` - Allows thread dump downloads
- ❌ `flyway` - Shows database migration history
- ❌ `logfile` - Exposes application logs
- ❌ `loggers` - Allows logger level modification

**Safe Endpoints** (explicitly permitted):
- ✅ `health` - Application health status
- ✅ `info` - Application metadata
- ✅ `metrics` - Application metrics
- ✅ `prometheus` - Prometheus metrics endpoint

**Recommended Configuration**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info

# OR with metrics:
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

**Error Message**:
```
SECURITY ERROR: Sensitive Actuator endpoint 'env' is exposed in production. 
Remove from management.endpoints.web.exposure.include
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

### Critical Security Properties (Validation Fails Without These)

| Property | Env Variable | Description | Required |
|---|---|---|---|
| `kronos.security.auth-cookie.secure` | `KRONOS_SECURITY_AUTH_COOKIE_SECURE` | Cookies transmitted only via HTTPS | ✅ Prod |
| `frontend.allowed-origins` | `FRONTEND_ALLOWED_ORIGINS` | CORS domains (comma-separated HTTPS) | ✅ Prod |
| `jwt.secret` | `JWT_SECRET` | JWT signing secret (minimum 32 chars) | ✅ Prod |
| `springdoc.swagger-ui.enabled` | `SPRINGDOC_SWAGGER_UI_ENABLED` | Swagger UI must be disabled (`false`) | ✅ Prod |
| `springdoc.api-docs.enabled` | `SPRINGDOC_API_DOCS_ENABLED` | API docs must be disabled (`false`) | ✅ Prod |
| `management.endpoints.web.exposure.include` | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator endpoints (no sensitive ones) | ✅ Prod |
| `kronos.security.upload.antivirus.enabled` | `KRONOS_SECURITY_UPLOAD_ANTIVIRUS_ENABLED` | Antivirus must be enabled (`true`) | ✅ Prod |
| `aws.region` | `AWS_REGION` | AWS region (e.g., `us-east-1`) | ✅ Prod |

### AWS Credentials (Conditional - Choose One Mode)

| Property | Env Variable | Description | Mode |
|---|---|---|---|
| `aws.access-key-id` | `AWS_ACCESS_KEY_ID` | AWS IAM access key | Static Credentials |
| `aws.secret-access-key` | `AWS_SECRET_ACCESS_KEY` | AWS IAM secret key | Static Credentials |
| (none) | (none) | Use EC2/ECS/K8s IAM | IAM Role ✅ |

### Cookie Configuration (HttpOnly is Hardcoded)

| Property | Env Variable | Description | Default | Notes |
|---|---|---|---|---|
| `kronos.security.auth-cookie.same-site` | `KRONOS_SECURITY_AUTH_COOKIE_SAME_SITE` | CSRF protection | `Lax` | Use `Strict` for higher security |
| `kronos.security.auth-cookie.domain` | `KRONOS_SECURITY_AUTH_COOKIE_DOMAIN` | Cookie domain scope | - | Set to `.yourdomain.com` |
| (HttpOnly hardcoded) | - | JS-inaccessible cookies | `true` | **Cannot be disabled** |

### JWT Configuration

| Property | Env Variable | Description | Default |
|---|---|---|---|
| `jwt.expiration` | `JWT_EXPIRATION` | Access token TTL (seconds) | `3600` (1 hour) |
| `jwt.refresh-expiration` | `JWT_REFRESH_EXPIRATION` | Refresh token TTL (seconds) | `604800` (7 days) |

### S3 Buckets Configuration

| Property | Env Variable | Description |
|---|---|---|
| `aws.s3.bucket-name` | `AWS_S3_BUCKET` | Main S3 bucket |
| `aws.s3.bucket-name-docs` | `AWS_S3_BUCKET_NAME_DOCS` | Document uploads |
| `aws.s3.bucket-documents` | `AWS_S3_BUCKET_DOCUMENTS` | General documents |
| `aws.s3.bucket-employee-documents` | `AWS_S3_BUCKET_EMPLOYEE_DOCUMENTS` | Employee docs |
| `aws.s3.bucket-payslip` | `AWS_S3_BUCKET_PAYSLIP` | Payslip storage |
| `aws.s3.bucket-time-off` | `AWS_S3_BUCKET_TIME_OFF` | Time off documents |

---

## Profile-Based Behavior

### Production Profile (`prod`)

**All 7 validations are mandatory and block startup if they fail:**

1. ❌ Cookie security validation fails if `secure=false`
2. ❌ CORS validation fails if origins are invalid/missing
3. ❌ Swagger validation fails if UI is enabled
4. ❌ JWT validation fails if secret is missing or < 32 chars
5. ❌ AWS validation fails if:
   - `aws.region` is missing, or
   - Credentials are incomplete (only one key), or
   - Both keys are provided along with neither (ambiguous)
6. ❌ Actuator validation fails if any sensitive endpoint is exposed
7. ❌ Antivirus validation fails if scanning is disabled

**Result**: Application **fails to start immediately** if any mandatory validation fails. No startup warnings.

### Non-Production Profiles (`dev`, `local`, `test`)

- **All validations skipped** (no checks performed)
- ✅ Application starts regardless of configuration
- Allows flexible development/testing setup without security constraints
- Debug log only: `Production validation skipped: not running in prod profile`

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
