# LGPD Staging & Production Configuration Validation Checklist

**Data:** 2026-05-25  
**Versão:** 1.0  
**Escopo:** Validação de configuração em staging e produção  
**Status:** Checklist Operacional

---

## 1. Variáveis de Ambiente — Validação Pré-Deploy

### 1.1 Autenticação e Segurança

```bash
# Verificar JWT_SECRET está configurado (não usar padrão)
echo "JWT_SECRET configurado: $([ -n "$JWT_SECRET" ] && echo "✓ SIM" || echo "✗ NÃO")"
echo "JWT_SECRET comprimento: $(echo -n "$JWT_SECRET" | wc -c) caracteres (min 32)"

# Verificar JWT_EXPIRATION (recomendado: 3600s = 1h)
echo "JWT_EXPIRATION: $JWT_EXPIRATION"

# Verificar REFRESH_TOKEN_EXPIRATION (recomendado: 604800s = 7 dias)
echo "REFRESH_TOKEN_EXPIRATION: $REFRESH_TOKEN_EXPIRATION"

# Verificar SECRET_KEY para Sessions (se aplicável)
echo "SECRET_KEY configurado: $([ -n "$SECRET_KEY" ] && echo "✓ SIM" || echo "✗ NÃO")"
```

**Critério de Sucesso:**
- [ ] JWT_SECRET está definido e tem mínimo 32 caracteres
- [ ] JWT_EXPIRATION é 3600 ou menos
- [ ] REFRESH_TOKEN_EXPIRATION é razoável (< 30 dias)
- [ ] Nenhuma secret padrão de exemplo em produção

---

### 1.2 CORS (Cross-Origin Resource Sharing)

```bash
# Verificar CORS_ALLOWED_ORIGINS
echo "CORS_ALLOWED_ORIGINS: $CORS_ALLOWED_ORIGINS"

# Verificar que não é "*" em produção
if [ "$CORS_ALLOWED_ORIGINS" = "*" ]; then
  echo "✗ ERRO: CORS wildcard (*) não permitido em produção"
else
  echo "✓ CORS está restritivo"
fi

# Verificar CORS_ALLOW_CREDENTIALS
echo "CORS_ALLOW_CREDENTIALS: $CORS_ALLOW_CREDENTIALS"
```

**Critério de Sucesso:**
- [ ] CORS_ALLOWED_ORIGINS não é "*"
- [ ] CORS_ALLOWED_ORIGINS contém apenas domínios conhecidos
- [ ] CORS_ALLOW_CREDENTIALS é false (a menos que necessário)
- [ ] Sem localhost em produção

---

### 1.3 Cookies e Sessão

```bash
# Verificar configuração de cookies
echo "AUTH_COOKIE_SECURE: $AUTH_COOKIE_SECURE"
echo "AUTH_COOKIE_HTTP_ONLY: $AUTH_COOKIE_HTTP_ONLY"
echo "AUTH_COOKIE_SAME_SITE: $AUTH_COOKIE_SAME_SITE"
echo "AUTH_COOKIE_DOMAIN: $AUTH_COOKIE_DOMAIN"
echo "AUTH_COOKIE_MAX_AGE: $AUTH_COOKIE_MAX_AGE"
```

**Critério de Sucesso:**
- [ ] AUTH_COOKIE_SECURE = true (HTTPS only)
- [ ] AUTH_COOKIE_HTTP_ONLY = true (JavaScript não acessa)
- [ ] AUTH_COOKIE_SAME_SITE = STRICT ou LAX
- [ ] AUTH_COOKIE_DOMAIN não inclui localhost em produção

---

## 2. HTTPS e TLS

### 2.1 Certificado SSL

```bash
# Verificar certificado SSL válido
echo "Verificando certificado SSL..."
openssl s_client -connect localhost:443 -servername kronos.com < /dev/null 2>/dev/null | \
  openssl x509 -noout -dates

# Verificar que não é self-signed em produção
openssl s_client -connect localhost:443 < /dev/null 2>/dev/null | \
  openssl x509 -noout -text | grep "Self Signed" && echo "✗ ERRO: Certificado auto-assinado" || echo "✓ Certificado válido"
```

**Critério de Sucesso:**
- [ ] Certificado SSL válido (não expirado)
- [ ] Não é auto-assinado em produção
- [ ] TLS 1.2 mínimo (idealmente 1.3)
- [ ] Cipher suites seguros

---

### 2.2 HSTS (HTTP Strict Transport Security)

```bash
# Verificar HSTS header
curl -i https://kronos.com 2>/dev/null | grep -i "Strict-Transport-Security"

# Resultado esperado:
# Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

**Critério de Sucesso:**
- [ ] HSTS header está presente
- [ ] max-age >= 31536000 (1 ano)
- [ ] includeSubDomains está presente

---

## 3. AWS Configuração

### 3.1 AWS S3 (Armazenamento de Dados)

```bash
# Verificar se S3 bucket é privado
aws s3api get-bucket-acl --bucket kronos-prod-bucket \
  --query 'Grants[*].[Grantee.Type, Permission]' \
  --output text

# Verificar se público está bloqueado
aws s3api get-public-access-block --bucket kronos-prod-bucket \
  --query 'PublicAccessBlockConfiguration' \
  --output json

# Exemplo esperado:
# {
#   "BlockPublicAcls": true,
#   "IgnorePublicAcls": true,
#   "BlockPublicPolicy": true,
#   "RestrictPublicBuckets": true
# }
```

**Critério de Sucesso:**
- [ ] S3 bucket é privado (não público)
- [ ] Block Public Access está ativado (todos true)
- [ ] Encryption está ativado (AES-256 ou KMS)
- [ ] Versionamento está ativado

---

### 3.2 AWS Encryption at Rest

```bash
# Verificar encryption padrão do bucket
aws s3api get-bucket-encryption --bucket kronos-prod-bucket \
  --query 'ServerSideEncryptionConfiguration' \
  --output json

# Verificar que é AES-256 ou CMK (KMS)
```

**Critério de Sucesso:**
- [ ] Encryption padrão está configurado
- [ ] Tipo: AES-256 ou CMK (KMS)
- [ ] Aplicado a todos os novos objetos

---

### 3.3 AWS IAM (Acesso e Credenciais)

```bash
# Verificar que usa IAM Role (não static credentials)
aws sts get-caller-identity

# Resultado esperado (com IAM Role):
# {
#   "UserId": "AIDAI...:role-name",
#   "Account": "123456789012",
#   "Arn": "arn:aws:iam::123456789012:role/eks-pod-role"
# }

# NUNCA usar static credentials em produção
echo "AWS_ACCESS_KEY_ID: $([ -n "$AWS_ACCESS_KEY_ID" ] && echo "✗ ERRO: Static credentials encontradas" || echo "✓ Usando IAM Role")"
```

**Critério de Sucesso:**
- [ ] Usando IAM Role (não static credentials)
- [ ] Política de menos privilégio (apenas S3, sem admin)
- [ ] Sem acesso desnecessário a serviços

---

## 4. Proxy e Reverse Proxy

### 4.1 Nginx/Proxy Configuration

```bash
# Verificar que proxy está redirigindo para backend correto
curl -v https://kronos.com/api/health 2>&1 | head -20

# Verificar headers seguros estão sendo adicionados
curl -i https://kronos.com/api/lgpd/processing-catalog 2>/dev/null | \
  grep -i "X-Content-Type-Options\|X-Frame-Options\|Content-Security-Policy"

# Esperado:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# Content-Security-Policy: default-src 'self'
```

**Critério de Sucesso:**
- [ ] Proxy está redirigindo corretamente
- [ ] X-Content-Type-Options: nosniff
- [ ] X-Frame-Options: DENY (ou SAMEORIGIN)
- [ ] Content-Security-Policy configurado

---

### 4.2 Trust Forwarded Headers

```bash
# No arquivo de configuração Spring (application-prod.yml):
server:
  tomcat:
    remoteip:
      remote-ip-header: "X-Forwarded-For"
      protocol-header: "X-Forwarded-Proto"
      port-header: "X-Forwarded-Port"
      protocol-header-value: "https"
```

**Critério de Sucesso:**
- [ ] server.tomcat.remoteip.remote-ip-header = X-Forwarded-For
- [ ] server.tomcat.remoteip.protocol-header = X-Forwarded-Proto
- [ ] server.tomcat.remoteip.protocol-header-value = https

---

## 5. Spring Boot Actuator

### 5.1 Verificar Endpoints de Actuator

```bash
# Verificar que /actuator está desabilitado ou protegido
curl -s http://localhost:8080/actuator 2>/dev/null | head -5

# Esperado em produção:
# 401 Unauthorized ou 403 Forbidden

# Verificar configuração
echo "management.endpoints.web.exposure.include: (verificar application-prod.yml)"
echo "Esperado: health,metrics (apenas essencial)"
```

**Critério de Sucesso:**
- [ ] /actuator exige autenticação
- [ ] Apenas endpoints essenciais expostos (health, metrics)
- [ ] Sem /actuator/env, /actuator/configprops expostos
- [ ] management.endpoints.web.exposure.exclude = *

---

### 5.2 Proteger Endpoints Sensíveis

```bash
# Configuração esperada em Spring Security:
# - /actuator/health: PUBLIC
# - /actuator/metrics: ADMIN_ONLY
# - Todos os outros /actuator: DENY

# Testar:
curl -H "Authorization: Bearer INVALID_TOKEN" \
  http://localhost:8080/actuator/metrics 2>/dev/null | head -5

# Esperado: 401 ou 403
```

**Critério de Sucesso:**
- [ ] /actuator/health é público (liveness probe)
- [ ] /actuator/metrics exige autenticação
- [ ] Nenhum endpoint de actuator expõe configuração/variáveis

---

## 6. Swagger/OpenAPI

### 6.1 Desabilitar Swagger em Produção

```bash
# Verificar que Swagger UI está desabilitado
curl -s http://localhost:8080/swagger-ui.html 2>/dev/null | grep -i "swagger" || echo "✓ Swagger desabilitado"

# Verificar configuração
echo "springdoc.swagger-ui.enabled: false (em application-prod.yml)"
echo "springdoc.api-docs.enabled: false (em application-prod.yml)"
```

**Critério de Sucesso:**
- [ ] springdoc.swagger-ui.enabled = false
- [ ] springdoc.api-docs.enabled = false
- [ ] /swagger-ui.html retorna 404 em produção
- [ ] /v3/api-docs retorna 404 em produção

---

## 7. Logging e PII

### 7.1 Verificar Logs Sem PII

```bash
# Verificar arquivo de configuração logback-spring.xml
cat src/main/resources/logback-spring.xml | grep -i "pattern\|conversionPattern"

# Esperado:
# Nenhuma referência a %X{user.email} ou %X{user.cpf}
# Apenas: %d, %p, %logger, %m

# Verificar logs em tempo real
tail -100 logs/kronos.log | grep -i "cpf\|email\|token\|senha" && \
  echo "✗ ERRO: PII encontrado em logs" || \
  echo "✓ Logs sem PII"
```

**Critério de Sucesso:**
- [ ] Logs não contêm CPF
- [ ] Logs não contêm email
- [ ] Logs não contêm tokens
- [ ] Logs não contêm senhas
- [ ] SensitiveDataMasker está ativo

---

### 7.2 Retenção de Logs

```bash
# Verificar política de retenção em logback-spring.xml
cat src/main/resources/logback-spring.xml | grep -A 5 "RollingFileAppender" | grep -E "maxHistory|maxFileSize"

# Esperado:
# <maxHistory>90</maxHistory>  <!-- 90 dias -->
# <maxFileSize>100MB</maxFileSize>
```

**Critério de Sucesso:**
- [ ] maxHistory configurado (recomendado: 90 dias)
- [ ] maxFileSize configurado (recomendado: 100MB)
- [ ] Logs não crescem indefinidamente

---

## 8. Database

### 8.1 Verificar Conexão Segura

```bash
# Verificar string de conexão (não expor senha)
echo "spring.datasource.url: (verificar application-prod.yml)"

# Esperado:
# spring.datasource.url=jdbc:postgresql://db-prod.internal:5432/kronos_db
# spring.datasource.username=${DB_USERNAME}
# spring.datasource.password=${DB_PASSWORD}

# Verificar que credenciais vêm de variáveis de ambiente
echo "DB_USERNAME: $([ -n "$DB_USERNAME" ] && echo "✓ Configurado" || echo "✗ NÃO")"
echo "DB_PASSWORD: $([ -n "$DB_PASSWORD" ] && echo "✓ Configurado" || echo "✗ NÃO")"
```

**Critério de Sucesso:**
- [ ] spring.datasource.url não contém senha
- [ ] DB_USERNAME vem de variável de ambiente
- [ ] DB_PASSWORD vem de variável de ambiente (não hardcoded)
- [ ] Conexão usa SSL (jdbc:postgresql://...?ssl=true)

---

### 8.2 Backup

```bash
# Verificar que backup está rodando
# (Verificar via AWS RDS Console ou comando específico do banco)

aws rds describe-db-instances \
  --db-instance-identifier kronos-prod-db \
  --query 'DBInstances[0].[BackupRetentionPeriod, PreferredBackupWindow]' \
  --output text

# Esperado:
# 30  (dias de retenção)
# 03:00-04:00  (janela de backup)
```

**Critério de Sucesso:**
- [ ] BackupRetentionPeriod >= 7 dias (recomendado: 30)
- [ ] Backup automático está ativado
- [ ] Teste de restore foi realizado

---

## 9. Rate Limiting

### 9.1 Verificar Rate Limiting

```bash
# Testar rate limiting (enviar múltiplas requisições)
for i in {1..10}; do
  curl -s -o /dev/null -w "%{http_code}\n" https://kronos.com/api/auth/login \
    -X POST \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"test"}'
done

# Esperado (após X requisições):
# 200
# 200
# 429 (Too Many Requests)
```

**Critério de Sucesso:**
- [ ] Rate limiting está ativo
- [ ] Limite é apropriado (não muito restritivo)
- [ ] Retorna 429 após limite excedido

---

## 10. Antivírus / EDR

### 10.1 Verificar Antivírus em Servidor

```bash
# Verificar se antivírus está rodando (Linux)
systemctl status clamav-daemon || echo "✗ ClamAV não instalado"

# Verificar definições são recentes
clamscan --version && freshclam -V || echo "✗ Antivírus não configurado"

# Verificar exclusões não incluem diretórios críticos
# (Revisar /etc/clamav/clamd.conf)
```

**Critério de Sucesso:**
- [ ] Antivírus está instalado e rodando
- [ ] Definições estão atualizadas
- [ ] Scans periódicos estão configurados
- [ ] Alertas estão ativados

---

## 11. Monitoramento e Alertas

### 11.1 Health Check Endpoint

```bash
# Verificar endpoint de health
curl -s http://localhost:8080/actuator/health | jq .

# Esperado:
# {
#   "status": "UP",
#   "components": {
#     "db": {"status": "UP"},
#     "diskSpace": {"status": "UP"},
#     "livenessState": {"status": "UP"},
#     "readinessState": {"status": "UP"}
#   }
# }
```

**Critério de Sucesso:**
- [ ] /actuator/health retorna UP
- [ ] Database connection está OK
- [ ] Disco tem espaço suficiente
- [ ] Liveness probe está UP

---

### 11.2 Métricas

```bash
# Verificar métricas disponíveis
curl -s http://localhost:8080/actuator/metrics | jq '.names[]' | head -20

# Esperado:
# jvm.memory.used
# process.cpu.usage
# http.server.requests
# etc.
```

**Critério de Sucesso:**
- [ ] Métricas JVM estão disponíveis
- [ ] Métricas HTTP estão disponíveis
- [ ] Sistema de monitoramento coleta as métricas

---

## 12. LGPD Específico

### 12.1 Endpoints LGPD Protegidos

```bash
# Verificar que endpoints LGPD exigem autenticação
curl -s http://localhost:8080/lgpd/processing-catalog \
  -H "Authorization: Bearer INVALID" 2>/dev/null | head -5

# Esperado: 401 Unauthorized ou 403 Forbidden

# Verificar que apenas CTO pode acessar /lgpd/admin/retention
curl -s http://localhost:8080/lgpd/admin/retention/dry-run \
  -H "Authorization: Bearer USER_TOKEN" 2>/dev/null | head -5

# Esperado: 403 Forbidden (sem role CTO)
```

**Critério de Sucesso:**
- [ ] /lgpd/processing-catalog exige autenticação
- [ ] /lgpd/admin/retention exige role CTO
- [ ] /lgpd/export exige autenticação próprio usuário
- [ ] Sem acesso anônimo a dados LGPD

---

### 12.2 Sanitização de Dados

```bash
# Verificar que responses não contêm PII
curl -s http://localhost:8080/lgpd/admin/retention/dry-run \
  -H "Authorization: Bearer CTO_TOKEN" 2>/dev/null | jq '.[].notes'

# Não deve conter:
# - Emails
# - CPF
# - Telefones
# - Endereços
```

**Critério de Sucesso:**
- [ ] Responses não contêm PII
- [ ] notes campo é sanitizado
- [ ] SensitiveDataMasker está funcionando

---

## 13. Checklist Pré-Deploy Final

| Item | Staging | Produção | Status |
|---|---|---|---|
| JWT_SECRET configurado | ✓ | ✓ | |
| CORS restritivo | ✓ | ✓ | |
| Cookies SECURE | ✓ | ✓ | |
| HTTPS/TLS ativado | ✓ | ✓ | |
| HSTS header | ✓ | ✓ | |
| S3 privado | ✓ | ✓ | |
| S3 encryption | ✓ | ✓ | |
| IAM Role ativado | ✓ | ✓ | |
| Actuator protegido | ✓ | ✓ | |
| Swagger desabilitado | ✓ | ✓ | |
| Logs sem PII | ✓ | ✓ | |
| Database backup | ✓ | ✓ | |
| Rate limiting | ✓ | ✓ | |
| Antivírus ativo | | ✓ | |
| Health check OK | ✓ | ✓ | |
| Métricas disponíveis | ✓ | ✓ | |
| LGPD endpoints protegidos | ✓ | ✓ | |
| Sanitização funcionando | ✓ | ✓ | |

---

## 14. Riscos Residuais

### 🟡 Medium Risk

1. **Certificado SSL expira** → Configurar renovação automática (Let's Encrypt)
2. **Credenciais em variáveis** → Usar Secret Manager (AWS Secrets Manager)
3. **Logs crescem rápido** → Aumentar retenção ou usar CloudWatch
4. **Backup falha silenciosamente** → Testar restore regularmente

### 🔴 High Risk

1. **CORS aberto em produção** → Bloqueador crítico
2. **JWT_SECRET padrão** → Bloqueador crítico
3. **Database sem encryption** → Bloqueador crítico
4. **Swagger expõe API em produção** → Bloqueador crítico

---

## 15. Próximos Passos

1. **P2-DEVOPS-002** — Roteiro de rollback
2. **P2-SEC-001** — Escopo de pentest LGPD
3. **P2-DOC-001** — Checklist jurídico pré-go-live
4. **P2-DOC-002** — Atualizar relatório técnico com status

---

**Documento preparado por:** DevOps  
**Validado por:** Security, Compliance  
**Última atualização:** 2026-05-25  
**Status:** Pronto para validação em staging
