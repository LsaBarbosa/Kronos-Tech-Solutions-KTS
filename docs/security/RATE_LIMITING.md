# Rate Limiting — Kronos Tech Solutions

## Overview

**Rate limiting** protege contra ataques de força bruta e abuso de API ao limitar o número de requisições de um cliente em um período de tempo.

O Kronos implementa rate limiting em múltiplas camadas:
- **Authentication:** Login e recuperação de senha
- **Biometric:** Enrollment, checkin, login facial
- **Admin:** Buscas de verificação
- **Frontend:** Observabilidade de eventos

---

## Architecture

### Components

#### 1. AuthenticationRateLimitService
Serviço principal que gerencia rate limiting de autenticação.

**Métodos:**
- `checkLoginAllowed(String username)` — valida se login é permitido
- `onLoginFailure(String username)` — registra falha de login
- `onLoginSuccess(String username)` — registra sucesso e reseta contadores
- `checkPasswordRecoveryAllowed(String cpf, String email)` — valida recuperação
- `checkAdminSearchRateLimit()` — valida buscas admin

**Estratégia de armazenamento:**
```
Redis (if enabled) → Fall back to In-Memory
```

#### 2. RedisRateLimitStore
Implementação distribuída usando Redis para compartilhamento de estado em múltiplas instâncias.

**Métodos:**
- `increment(bucket, scope, ttl)` — incrementa contador com tempo de expiração
- `incrementPenalty(bucket, scope, ttl)` — incrementa nível de penalidade
- `isCoolingDown(bucket, scope)` — verifica se em período de cooldown
- `setCooldown(bucket, scope, ttl)` — ativa cooldown por duração
- `reset(bucket, scope)` — reseta todos os contadores

#### 3. RestExceptionHandler
Converte `TooManyRequestsException` em HTTP 429 (Too Many Requests).

---

## Configuration

### Environment Variables

#### Redis Connection (if using Redis)
```bash
REDIS_HOST=<redis-server-ip>        # Default: localhost
REDIS_PORT=6379                      # Default: 6379
REDIS_PASSWORD=<redis-password>      # Default: empty
REDIS_TIMEOUT=2000ms                 # Default: 2000ms
REDIS_SSL_ENABLED=false              # Default: false
```

#### Kronos Redis Config
```bash
KRONOS_REDIS_ENABLED=true            # Enable/disable Redis for rate limiting
REDIS_NAMESPACE=kronos               # Key prefix
REDIS_KEY_HMAC_SECRET=<secret>       # HMAC secret for key hashing
```

#### Login Rate Limiting
```bash
LOGIN_RATE_LIMIT_IP_LIMIT=10         # Max attempts per IP per window
LOGIN_RATE_LIMIT_IP_WINDOW_SECONDS=60

LOGIN_RATE_LIMIT_USERNAME_LIMIT=5    # Max attempts per username per window
LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS=300

LOGIN_RATE_LIMIT_COOLDOWN_MINUTES=5,15,30  # Progressive cooldown: 5min → 15min → 30min
```

#### Password Recovery Rate Limiting
```bash
RECOVERY_RATE_LIMIT_CPF_LIMIT=3
RECOVERY_RATE_LIMIT_EMAIL_LIMIT=3
RECOVERY_RATE_LIMIT_IP_LIMIT=10
RECOVERY_RATE_LIMIT_WINDOW_SECONDS=3600
```

#### Admin/Biometric Rate Limiting
```bash
ADMIN_CHECK_RATE_LIMIT=20
ADMIN_CHECK_RATE_LIMIT_WINDOW_SECONDS=60

BIOMETRIC_LOGIN_FACE_LIMIT=5
BIOMETRIC_LOGIN_FACE_WINDOW_SECONDS=60

BIOMETRIC_CHECKIN_LIMIT=20
BIOMETRIC_CHECKIN_WINDOW_SECONDS=60

BIOMETRIC_ENROLLMENT_LIMIT=10
BIOMETRIC_ENROLLMENT_WINDOW_SECONDS=600
```

### Default Configuration (application-prod.yml)

```yaml
app:
  security:
    rate-limit:
      login:
        ip:
          limit: ${LOGIN_RATE_LIMIT_IP_LIMIT:10}          # 10 per 60s
          window-seconds: ${LOGIN_RATE_LIMIT_IP_WINDOW_SECONDS:60}
        username:
          limit: ${LOGIN_RATE_LIMIT_USERNAME_LIMIT:5}     # 5 per 300s
          window-seconds: ${LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS:300}
        cooldown-minutes: ${LOGIN_RATE_LIMIT_COOLDOWN_MINUTES:5,15,30}
      recovery:
        cpf:
          limit: ${RECOVERY_RATE_LIMIT_CPF_LIMIT:3}       # 3 per 3600s
        email:
          limit: ${RECOVERY_RATE_LIMIT_EMAIL_LIMIT:3}     # 3 per 3600s
        ip:
          limit: ${RECOVERY_RATE_LIMIT_IP_LIMIT:10}       # 10 per 3600s
        window-seconds: ${RECOVERY_RATE_LIMIT_WINDOW_SECONDS:3600}
      admin-check:
        limit: ${ADMIN_CHECK_RATE_LIMIT:20}               # 20 per 60s
        window-seconds: ${ADMIN_CHECK_RATE_LIMIT_WINDOW_SECONDS:60}
```

---

## Rate Limits Reference

| Endpoint | Limite | Janela | Cooldown | Chave |
|----------|--------|--------|----------|-------|
| `/api/auth/login` | 5 por username | 300s | 5/15/30 min | Username |
| `/api/auth/login` | 10 por IP | 60s | N/A | IP |
| `/api/auth/recover-password` | 3 por CPF | 3600s | N/A | CPF |
| `/api/auth/recover-password` | 3 por Email | 3600s | N/A | Email |
| `/api/auth/recover-password` | 10 por IP | 3600s | N/A | IP |
| `/api/auth/login-face` | 5 por IP | 60s | N/A | IP |
| `/api/auth/checkin` | 20 por IP | 60s | N/A | IP |
| `/api/auth/enrollment` | 10 por IP | 600s | N/A | IP |

---

## How It Works

### Login Flow

1. **Request arrives** → AuthController.login()
2. **Pre-check** → `authenticationRateLimitService.checkLoginAllowed(username)`
   - If limit exceeded → `TooManyRequestsException` → HTTP 429
   - If in cooldown → `TooManyRequestsException` → HTTP 429
3. **Authenticate** → AuthService.login()
4. **On failure** → `authenticationRateLimitService.onLoginFailure(username)`
   - Increment counter
   - If counter >= limit → activate cooldown with penalty
5. **On success** → `authenticationRateLimitService.onLoginSuccess(username)`
   - Reset counter for username

### Progressive Cooldown

After N failed attempts within the window:

1. **1st threshold reached** → 5 minute cooldown
2. **2nd threshold reached** → 15 minute cooldown
3. **3rd threshold reached** → 30 minute cooldown

Each threshold resets attempts counter but adds progressive penalties.

### Storage

#### Redis (Distributed)
```
rate-limit:login:ip:{ip_address}           → counter
rate-limit:login:username:{username}       → counter
rate-limit:login:username:{username}:cd    → cooldown marker
rate-limit:login:username:{username}:pn    → penalty level
```

#### In-Memory (Fallback)
ConcurrentHashMap with Deque-based sliding window per bucket.

---

## Testing

### Manual Test

```bash
# Basic test (15 attempts, 100ms delay)
./scripts/security/test-rate-limiting.sh localhost 8080 15 100

# Aggressive test (50 attempts, 10ms delay)
./scripts/security/test-rate-limiting.sh localhost 8080 50 10

# Production test (adjust host/port)
./scripts/security/test-rate-limiting.sh prod.example.com 443 15 100
```

### Expected Results

With default config (5 attempts per 300 seconds):

```
Attempt 1-5: 401 (authentication failed - normal)
Attempt 6: 429 (rate limited - BLOCKED!)
Attempts 7+: 429 (still blocked)

After 5 minute cooldown: 401 (retry allowed)
```

### Automated Tests

```bash
# Run unit tests
gradle test --tests "*RateLimitService*"

# Run integration tests
gradle integrationTest --tests "*RateLimit*"
```

---

## Monitoring

### Metrics (via Prometheus)

```
kronos_rate_limit_blocked_total{bucket="login"}
kronos_rate_limit_allowed_total{bucket="login"}
kronos_redis_rate_limit_unavailable_total
```

### Logs

Look for:
```
event=rate_limit_blocked operation=auth_login target=<ref> reason=ip_limit
event=rate_limit_blocked operation=auth_login target=<ref> reason=username_cooldown
event=rate_limit_blocked operation=password_recovery target=<ref> reason=cpf_limit
```

### Alerting

Recommended alerts:

1. **High block rate**
   - Condition: `rate_limit_blocked > 50 per minute`
   - Action: Check for brute force attack

2. **Redis unavailable**
   - Condition: `redis_unavailable{service="rate-limit"} > 5`
   - Action: Check Redis connectivity

3. **Cooldown activated**
   - Condition: `rate_limit_blocked{reason="cooldown"} > 10 per hour`
   - Action: Check for distributed attack

---

## Troubleshooting

### 429 not returned (rate limiting not working)

**Checklist:**

1. **Redis connectivity**
   ```bash
   # Test Redis connection
   redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD ping
   # Expected: PONG
   ```

2. **Environment variables**
   ```bash
   # Verify variables are set
   echo $KRONOS_REDIS_ENABLED              # Should be: true
   echo $LOGIN_RATE_LIMIT_USERNAME_LIMIT   # Should be: 5
   echo $LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS  # Should be: 300
   ```

3. **Application logs**
   ```bash
   # Check for Redis connection errors
   grep -i "redis.*unavailable\|redis.*connection" logs/app.log
   
   # Check for rate limiting logs
   grep "rate_limit" logs/app.log
   ```

4. **In-memory fallback active**
   ```bash
   # Check if Redis is disabled
   grep "KRONOS_REDIS_ENABLED" logs/app.log
   # If see "redis_rate_limit_unavailable", Redis is down (fallback to memory)
   ```

5. **Limit configuration**
   ```bash
   # Verify limits aren't too high
   # Default: 5 attempts per 300 seconds
   # If limit > 100, rate limiting will seem disabled
   ```

### Rate limiting too aggressive

**Solution:** Increase limits via environment variables:

```bash
# Allow 20 per 600 seconds instead of 5 per 300
LOGIN_RATE_LIMIT_USERNAME_LIMIT=20
LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS=600
```

### Legitimate users blocked

**Solutions:**

1. **Whitelist trusted IPs:**
   ```yaml
   CLIENT_IP_TRUSTED_PROXY_CIDRS=203.0.113.0/24,198.51.100.0/24
   ```

2. **Increase window or limit:**
   ```bash
   LOGIN_RATE_LIMIT_USERNAME_LIMIT=10  # Was: 5
   ```

3. **Monitor false positives:**
   - Track 429 responses per username
   - Alert if > expected threshold
   - Add to whitelist if legitimate

---

## Security Considerations

### Strengths

✅ **Protects against:** Brute force, dictionary attacks, credential stuffing  
✅ **Distributed:** Redis allows sharing state across instances  
✅ **Progressive:** Cooldown increases with repeat offenses  
✅ **Fallback:** In-memory option if Redis unavailable  
✅ **Privacy:** No sensitive data in logs (IP/username hashed)  

### Limitations

⚠️ **IP-based limiting:** Bypassed by proxies, VPNs, shared IPs  
⚠️ **Username limiting:** Only effective if attacker knows usernames  
⚠️ **Distributed attacks:** Can't defend against 1000 IPs each trying once  
⚠️ **Memory leak risk:** In-memory fallback grows unbounded if not configured properly  

### Recommendations

1. **Always use Redis** for production distributed deployments
2. **Monitor for attacks** — high 429 rates indicate ongoing attack
3. **Use WAF/ModSecurity** (AÇÃO 3) for additional protection
4. **Implement CAPTCHA** on repeated blocks
5. **Log and alert** on suspicious patterns

---

## References

- OWASP: [Brute Force Attack](https://owasp.org/www-community/attacks/Brute_force_attack)
- OWASP: [Rate Limiting](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html#rate-limiting)
- Spring Security: [DDoS Protection](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html)

---

## Change Log

| Date | Author | Change |
|------|--------|--------|
| 2026-06-21 | Claude Code | Initial implementation and documentation |

**Last Updated:** 2026-06-21  
**Maintained by:** Security Team

