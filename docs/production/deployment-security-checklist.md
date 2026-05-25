# Production Deployment Security Checklist

**Purpose:** Quick reference for deploying Kronos to production with all security validations.

**Last Updated:** 2026-05-25  
**Related Docs:** `production-security-validator.md`, `lgpd-production-env-checklist.md`

---

## Pre-Deployment Validation

Run these commands before deploying to production:

```bash
# 1. Verify running on correct branch
git branch --show-current  # Should be: feature/lgpd-compliance or main

# 2. Verify no uncommitted changes
git status  # Should be clean

# 3. Compile back-end
./gradlew clean compileJava -q

# 4. Run security validator tests
./gradlew test --tests "*ProductionSecurityPropertiesValidator*" -q

# 5. Run full test suite
./gradlew test -q
```

---

## Environment Variables (Required for Production)

Set these variables in your production environment **before starting the application**.

### Authentication & Cookies

```env
# MANDATORY
KRONOS_SECURITY_AUTH_COOKIE_SECURE=true

# RECOMMENDED
KRONOS_SECURITY_AUTH_COOKIE_SAME_SITE=Strict
KRONOS_SECURITY_AUTH_COOKIE_DOMAIN=.yourdomain.com
```

✅ **Note**: HttpOnly is hardcoded in the application and cannot be disabled.

### JWT (JSON Web Tokens)

```env
# MANDATORY (minimum 32 characters, strong random string)
JWT_SECRET=your-very-long-secret-with-at-least-32-characters-here

# RECOMMENDED
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=604800
```

**Generate a secure JWT_SECRET:**
```bash
# Linux/Mac
openssl rand -base64 32

# Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### CORS (Cross-Origin Resource Sharing)

```env
# MANDATORY - comma-separated HTTPS domains only
FRONTEND_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com,https://app.yourdomain.com

# ❌ NEVER use these in production:
# FRONTEND_ALLOWED_ORIGINS=*                    # Wildcard blocked
# FRONTEND_ALLOWED_ORIGINS=http://yourdomain   # HTTP blocked
# FRONTEND_ALLOWED_ORIGINS=*.yourdomain.com    # Partial wildcard blocked
```

### AWS Credentials

**Choose ONE of these two modes:**

#### Mode 1: Static Credentials (provide both or neither)
```env
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

#### Mode 2: IAM Role (recommended for containers)
```env
AWS_REGION=us-east-1
# DO NOT SET AWS_ACCESS_KEY_ID or AWS_SECRET_ACCESS_KEY
# Application will use EC2 instance profile, ECS task role, or Kubernetes IRSA
```

### S3 Buckets

```env
AWS_S3_BUCKET_NAME_DOCS=kronos-docs-prod
AWS_S3_BUCKET_DOCUMENTS=kronos-docs-general-prod
AWS_S3_BUCKET_EMPLOYEE_DOCUMENTS=kronos-docs-employee-prod
AWS_S3_BUCKET_PAYSLIP=kronos-docs-payslip-prod
AWS_S3_BUCKET_TIME_OFF=kronos-docs-time-off-prod
```

### Actuator (Management Endpoints)

```env
# MANDATORY - comma-separated safe endpoints ONLY
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info

# Optional: add metrics for monitoring
# MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics

# ❌ NEVER expose these in production:
# env, heapdump, beans, configprops, threaddump, flyway, logfile, loggers, *
```

### Swagger / API Documentation

```env
# MANDATORY - must be false in production
SPRINGDOC_SWAGGER_UI_ENABLED=false
SPRINGDOC_API_DOCS_ENABLED=false
```

### Antivirus (Upload Scanning)

```env
# MANDATORY - must be true in production
KRONOS_SECURITY_UPLOAD_ANTIVIRUS_ENABLED=true
```

### Biometric (Liveness)

```env
# Fixed - do not change
BIOMETRIC_LIVENESS_REQUIRED=false
```

**Note**: Liveness is disabled by financial decision. Do not enable without approval.

---

## Application Startup Verification

After deploying, verify the application started correctly:

```bash
# 1. Check application logs for successful startup
docker logs <container-id> | grep "Production security validation completed successfully"

# 2. Verify health endpoint
curl -s https://yourdomain.com/actuator/health | jq .

# 3. Test authentication (replace with valid credentials)
curl -X POST https://yourdomain.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# 4. Verify CORS is working
curl -X OPTIONS https://yourdomain.com/api/endpoint \
  -H "Origin: https://yourdomain.com" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

---

## Validation Rules Applied at Startup

The application validates these rules **automatically** when running in `prod` profile:

### ✅ Passes Validation

```
✓ Cookie security validated (HttpOnly is guaranteed by AuthCookieService)
✓ CORS configuration validated
✓ Swagger disabled in production
✓ JWT secret configured
✓ AWS credentials mode: Static credentials (access-key-id + secret-access-key)
  OR
✓ AWS credentials mode: IAM Role (will use instance profile, ECS task role, or web identity)
✓ Actuator endpoints validation completed
✓ Antivirus protection enabled
Production security validation completed successfully
```

### ❌ Fails Validation (Application Stops)

```
SECURITY ERROR: Authentication cookies are not secure in production
SECURITY ERROR: frontend.allowed-origins is empty in production
SECURITY ERROR: wildcard (*) found in CORS origin
SECURITY ERROR: origin does not use HTTPS
SECURITY ERROR: Swagger UI is enabled in production
SECURITY ERROR: JWT secret is not configured or too short
SECURITY ERROR: AWS region is not configured in production
SECURITY ERROR: AWS credentials are incomplete
SECURITY ERROR: Sensitive Actuator endpoint 'env' is exposed in production
SECURITY ERROR: Antivirus protection is disabled in production
```

---

## Deployment Environments

### Local Development
- Profile: `local` or `dev`
- Security validations: **Skipped**
- Allows HTTP, insecure cookies, Swagger UI enabled

### Staging/Testing
- Profile: `test` or `staging`
- Security validations: **Skipped**
- Can test security configurations, but not enforced

### Production
- Profile: `prod`
- Security validations: **All enforced**
- Application fails to start if any validation fails
- No warnings: validations either pass or block startup

---

## Troubleshooting Startup Failures

### Problem: "JWT secret is not configured or too short"
**Solution**: Set `JWT_SECRET` with at least 32 characters
```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

### Problem: "AWS region is not configured"
**Solution**: Set `AWS_REGION`
```bash
export AWS_REGION=us-east-1
```

### Problem: "AWS credentials are incomplete"
**Cause**: Only `AWS_ACCESS_KEY_ID` or only `AWS_SECRET_ACCESS_KEY` is set
**Solution**: Either:
- Set both keys, or
- Unset both keys and use IAM Role

### Problem: "Actuator endpoint 'env' is exposed"
**Solution**: Remove `env` from `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`
```bash
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
```

### Problem: "CORS origin does not use HTTPS"
**Solution**: Ensure all origins in `FRONTEND_ALLOWED_ORIGINS` start with `https://`
```bash
export FRONTEND_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

---

## Post-Deployment Monitoring

### Health Checks
```bash
# Health endpoint (no authentication required)
curl https://yourdomain.com/actuator/health

# Expected response:
# {"status":"UP","components":{"...":"UP"}}
```

### Audit Logging
- All LGPD requests are logged with audit trail
- Check audit logs for access to sensitive endpoints
- Review audit reports weekly

### Certificate Expiration
- Monitor HTTPS/TLS certificate expiration
- Renew at least 30 days before expiration
- All CORS origins must use HTTPS

---

## Security Best Practices

1. **Secrets Management**
   - Store all secrets in environment variables or secrets manager
   - Never commit secrets to git
   - Rotate `JWT_SECRET` periodically

2. **AWS Credentials**
   - Prefer IAM Role over static credentials in containers
   - Rotate static credentials every 90 days
   - Restrict IAM policy to least privilege

3. **CORS Configuration**
   - Use specific domains, never wildcards
   - Use HTTPS only
   - Add subdomains only if needed

4. **Monitoring**
   - Monitor application startup logs for validation messages
   - Set up alerts for validation failures
   - Review audit logs regularly

5. **Backup & Disaster Recovery**
   - Backup S3 buckets regularly
   - Test restore procedures
   - Maintain disaster recovery documentation

---

## Support

For issues or questions:
- **Documentation**: See `production-security-validator.md`
- **LGPD Compliance**: See `lgpd-production-env-checklist.md`
- **Code**: `src/main/java/com/kts/kronos/config/ProductionSecurityPropertiesValidator.java`

