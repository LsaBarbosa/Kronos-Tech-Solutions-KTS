# LGPD Production Environment Checklist

## Overview

Este documento fornece um checklist completo de configurações obrigatórias e recomendadas para ambiente de produção, garantindo conformidade com LGPD e boas práticas de segurança.

**Nota Importante:** Liveness (biometric verification) permanece configurado como `false` por decisão financeira. Este não é um requisito obrigatório para produção.

---

## 1. Autenticação e Cookies

### Obrigatório (Production)

| Variável | Valor | Descrição |
|----------|-------|-----------|
| `kronos.security.auth-cookie.secure` | `true` | Cookies transmitidos apenas via HTTPS |
| `kronos.security.auth-cookie.http-only` | `true` | Protege contra XSS |
| `kronos.security.auth-cookie.same-site` | `Strict` ou `Lax` | Proteção contra CSRF |
| `kronos.security.auth-cookie.domain` | `.kronostechsolutions.com` | Domínio de validade do cookie |

### Ambiente Específico

| Ambiente | Configuração | Motivo |
|----------|--------------|--------|
| `local` | `secure=false` | Suporta HTTP para desenvolvimento |
| `dev` | `secure=false` | Ambiente sem HTTPS obrigatório |
| `prod` | `secure=true` | HTTPS obrigatório |

---

## 2. CSRF (Cross-Site Request Forgery)

### Obrigatório (Production)

```yaml
spring:
  security:
    csrf:
      enabled: true
      cookie:
        secure: true
        http-only: true
```

### Checklist

- [ ] CSRF token validation habilitado
- [ ] Cookies com flag Secure
- [ ] Cookies com flag HttpOnly
- [ ] SameSite policy configurada

---

## 3. CORS (Cross-Origin Resource Sharing)

### Obrigatório (Production)

```env
FRONTEND_ALLOWED_ORIGINS=https://kronostechsolutions.com,https://www.kronostechsolutions.com,https://app.kronostechsolutions.com
```

**Ou em application.yml:**

```yaml
frontend:
  allowed-origins: "https://kronostechsolutions.com,https://www.kronostechsolutions.com,https://app.kronostechsolutions.com"
```

### ❌ NUNCA em Produção

```env
# Proibido: wildcard
FRONTEND_ALLOWED_ORIGINS=*

# Proibido: HTTP (apenas HTTPS)
FRONTEND_ALLOWED_ORIGINS=http://kronostechsolutions.com

# Proibido: com path
FRONTEND_ALLOWED_ORIGINS=https://kronostechsolutions.com/path

# Proibido: com query
FRONTEND_ALLOWED_ORIGINS=https://kronostechsolutions.com?x=1

# Proibido: com fragment
FRONTEND_ALLOWED_ORIGINS=https://kronostechsolutions.com#section

# Proibido: wildcard parcial
FRONTEND_ALLOWED_ORIGINS=*.kronostechsolutions.com
```

### Regras de Validação em Produção

A aplicação valida automaticamente cada CORS origin:

- ✅ **Obrigatório HTTPS** - Nenhuma origem HTTP permitida em produção
- ✅ **Sem wildcards** - `*` e `*.domain.com` são rejeitados
- ✅ **Sem path/query/fragment** - Origins devem ser apenas `scheme://host[:port]`
- ✅ **Formato válido** - URLs malformadas são rejeitadas
- ✅ **Sem espaços** - Espaços internos causam rejeição
- ✅ **Lista não vazia** - Configuração vazia falha na inicialização

### Checklist

- [ ] CORS usa apenas HTTPS
- [ ] Origins específicas definidas (comma-separated)
- [ ] Nenhum wildcard ou wildcard parcial
- [ ] Nenhum path, query ou fragment nas origins
- [ ] Lista validada na inicialização

---

## 4. JWT (JSON Web Tokens)

### Obrigatório (Production)

| Variável | Requisito | Exemplo |
|----------|-----------|---------|
| `jwt.secret` | Min. 32 caracteres | `your-secure-secret-key-min-32-chars` |
| `jwt.expiration` | Definido em segundos | `3600` (1 hora) |
| `jwt.refresh-expiration` | Maior que expiration | `604800` (7 dias) |

### Checklist

- [ ] JWT secret configurado com min. 32 caracteres
- [ ] JWT secret não armazenado em código
- [ ] Expiração configurada
- [ ] Refresh token separado do access token

---

## 5. AWS S3

### Obrigatório (Production)

```env
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_S3_BUCKET=kronos-documents-prod
```

### Recomendado

- [ ] Usar IAM Role em EC2/ECS (não keys hardcoded)
- [ ] Encryption at rest: `AES256`
- [ ] Versioning habilitado
- [ ] Logging habilitado
- [ ] Lifecycle policy para arquivos antigos
- [ ] MFA Delete habilitado

### Checklist

- [ ] AWS credentials configuradas via variáveis de ambiente
- [ ] Bucket name configurado
- [ ] Region configurado
- [ ] Permissions restringidas (não public read)

---

## 6. AWS Rekognition (Biometric)

### Obrigatório (Production)

```env
AWS_REKOGNITION_REGION=us-east-1
BIOMETRIC_LIVENESS_REQUIRED=false
```

### ⚠️ Importante

**Liveness permanece `false` por decisão de custo.** Não altere para `true` sem aprovação legal/financeira.

### Checklist

- [ ] Region configurado
- [ ] IAM permissions para Rekognition
- [ ] Liveness explicitamente desabilitado (`false`)
- [ ] Nenhuma tentativa de ativar liveness

---

## 7. LGPD Retention

### Obrigatório (Production)

```yaml
kronos:
  lgpd:
    retention:
      allow-apply: false          # Desabilitar aplicação automática
      scheduler:
        enabled: false            # Scheduler desabilitado por padrão
        cron: "0 15 4 * * ?"      # Executar 04:15 UTC diariamente
```

### Recomendado

- [ ] Dry-run executado antes de APPLY
- [ ] APPLY habilitado apenas após testes
- [ ] Scheduler desabilitado até aprovação jurídica
- [ ] Logs auditados regularmente

### Checklist

- [ ] `allow-apply` desabilitado por padrão
- [ ] Scheduler desabilitado por padrão
- [ ] Políticas de retenção configuradas
- [ ] Backup realizado antes de APPLY

---

## 8. Antivírus de Upload

### Obrigatório (Production)

```env
UPLOAD_ANTIVIRUS_ENABLED=true
UPLOAD_MAX_FILE_SIZE=52428800  # 50 MB
UPLOAD_ALLOWED_TYPES=pdf,docx,xlsx,jpg,png
```

### Recomendado

- [ ] Varrição de arquivo antes de salvar
- [ ] Quarentena de arquivos suspeitos
- [ ] Logging de detecções
- [ ] Alerta para admin

### Checklist

- [ ] Antivírus habilitado em produção
- [ ] Tipos de arquivo restringidos
- [ ] Tamanho máximo configurado
- [ ] Scanning de upload ativo

---

## 9. Actuator (Management Endpoints)

### Obrigatório (Production)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
        exclude: env,heapdump,configprops
  endpoint:
    health:
      show-details: when-authorized
```

### ❌ NUNCA Expor em Produção

- `env` - Expõe variáveis de ambiente
- `heapdump` - Permite dump de memória
- `configprops` - Mostra todas as configurações
- `shutdown` - Permite desligar aplicação

### Checklist

- [ ] Apenas health e metrics expostos
- [ ] Details restringidos
- [ ] Endpoints sensíveis desabilitados
- [ ] RBAC em management endpoints

---

## 10. Swagger / OpenAPI

### Obrigatório (Production)

```yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

### Recomendado

- [ ] Documentação mantida interna
- [ ] OpenAPI em endpoint autenticado se necessário
- [ ] Senhas e chaves não expostas em examples

### Checklist

- [ ] Swagger desabilitado (`enabled: false`)
- [ ] OpenAPI desabilitado (`enabled: false`)
- [ ] Documentação disponível apenas internamente

---

## 11. Logging e Observabilidade

### Obrigatório (Production)

```yaml
logging:
  level:
    root: INFO
    com.kts.kronos: INFO
  pattern:
    console: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/kronos/application.log
    max-size: 100MB
    max-history: 30
```

### Recomendado

- [ ] Log levels apropriados por módulo
- [ ] Logs centralizados (ELK, CloudWatch)
- [ ] Rotação de logs configurada
- [ ] Alertas para erros críticos
- [ ] Auditoria de acesso em logs

### Checklist

- [ ] Log level configurado (INFO, não DEBUG)
- [ ] Logs enviados para storage seguro
- [ ] Rotação automática habilitada
- [ ] Retenção configurada conforme LGPD

---

## 12. Auditoria LGPD

### Obrigatório (Production)

```yaml
kronos:
  audit:
    enabled: true
    log-details: true
    sensitive-data-masking: true
```

### Checklist

- [ ] Auditoria habilitada
- [ ] Sanitização de dados pessoais ativa
- [ ] Rastreamento de acessos
- [ ] Logs de consentimento
- [ ] Logs de exercício de direitos

---

## Validação Automática

A aplicação validará automaticamente a configuração de produção no startup:

```
✓ Cookie security validated
✓ CORS configuration validated
✓ Swagger disabled in production
✓ JWT secret configured
✓ Actuator endpoints validation completed
```

Se alguma validação falhar em produção, a aplicação **não iniciará**.

### Ambiente Diferente de Produção

Em `local`, `dev` e outros perfis, validações são **warnings**, não erros.

---

## Checklist Final de Deploy

### Antes do Deploy

- [ ] Todas as variáveis de ambiente configuradas
- [ ] Certificado SSL/TLS válido
- [ ] Backups configurados
- [ ] Plano de rollback documentado

### Na Inicialização

- [ ] Verificar logs de validação de segurança
- [ ] Confirmar que todas as validações passaram
- [ ] Testar endpoints críticos
- [ ] Verificar conectividade AWS

### Pós-Deploy

- [ ] Monitorar logs de erro
- [ ] Confirmar métricas de health
- [ ] Testar fluxo LGPD completo
- [ ] Verificar auditoria registrada

---

## Suporte e Referências

- **LGPD Compliance**: `docs/legal/lgpd-compliance.md`
- **Retention Policies**: `docs/legal/retention-policies.md`
- **API Security**: `docs/security/api-security.md`
- **AWS Setup**: `docs/infrastructure/aws-setup.md`

---

**Última Atualização**: 2026-05-24  
**Responsável**: DevOps / Security Team
