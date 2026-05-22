# Secrets Management Guide

Este documento descreve como gerenciar segredos (credentials, chaves, senhas) com segurança no projeto Kronos.

## Princípios

1. **Nunca comitar segredos no repositório** — nem em `.env`, nem em código fonte
2. **Usar variáveis de ambiente** em produção
3. **Documentar sem revelar** — `.env.example` com placeholders
4. **Validar na startup** — evitar inicializações silenciosas com defaults inseguros

## Variáveis Obrigatórias

### Back-end

#### Database
- `DB_HOST` — hostname do PostgreSQL
- `DB_PORT` — porta (default: 5432)
- `DB_NAME` — nome do banco
- `DB_USERNAME` — usuário
- `DB_PASSWORD` — senha **[OBRIGATÓRIO, sem default]**

#### JWT & Auth
- `JWT_SECRET` — chave para assinar JWTs **[OBRIGATÓRIO, mín. 64 caracteres, sem default em prod]**
- `SECRET_TERM` — salt para termos biométricos **[OBRIGATÓRIO, mín. 32 caracteres, sem default em prod]**
- `AUTH_COOKIE_SECURE` — usar HTTPS cookies (true em prod)

#### AWS
- `AWS_ACCESS_KEY_ID` — chave de acesso **[OBRIGATÓRIO, sem default]**
- `AWS_SECRET_ACCESS_KEY` — chave secreta **[OBRIGATÓRIO, sem default]**
- `AWS_S3_BUCKET_NAME` — bucket principal **[OBRIGATÓRIO, sem default]**
- `AWS_REGION` — região AWS (default: us-east-1)

#### Email
- `MAIL_HOST` — servidor SMTP
- `MAIL_USERNAME` — usuário SMTP
- `MAIL_PASSWORD` — senha SMTP **[OBRIGATÓRIO, sem default]**

#### Digital Certificate (opcional)
- `DIGITAL_CERTIFICATE_PATH` — caminho para arquivo `.p12`
- `DIGITAL_CERTIFICATE_PASSWORD` — senha do certificado

### Observability Stack (infra/observability)

#### Grafana
- `GRAFANA_ADMIN_USER` — usuário admin (default: admin)
- `GRAFANA_ADMIN_PASSWORD` — senha (sem default em prod)

## Validação na Startup

A classe `ProductionConfigValidator.java` valida na startup:

- ✅ `JWT_SECRET` tem pelo menos 64 caracteres
- ✅ `SECRET_TERM` tem pelo menos 32 caracteres e não é placeholder
- ✅ `AUTH_COOKIE_SECURE=true` em production profile
- ✅ Nenhum default silencioso para credenciais críticas

Se validações falharem, a aplicação não inicializa.

## Padrão: Arquivo .env.example

Use `.env.example` com **placeholders**, não valores reais:

```bash
# ✅ Correto
DB_PASSWORD=change-me-strong-password
JWT_SECRET=change-me-use-base64-encoded-secret-at-least-64-chars

# ❌ Errado
DB_PASSWORD=prod_password_123
JWT_SECRET=real-production-jwt-secret-from-aws
```

Quando um desenvolvedor clona o repo:

```bash
cp .env.example .env
# Editar .env com valores locais/de teste
```

## Produção: Integração com AWS Secrets Manager

### Recomendado

Use AWS Secrets Manager para armazenar segredos:

```bash
# Armazenar no AWS Secrets Manager
aws secretsmanager create-secret \
  --name kronos/prod/db-password \
  --secret-string "password123"

# Na variável de ambiente, passar o ARN ou usar lambda environment override
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id kronos/prod/db-password \
  --query SecretString --output text)
```

### Alternativo: Variáveis de Ambiente Seguras

Se não tiver AWS Secrets Manager, use:

1. **Sistema de configuração da sua infraestrutura** (Kubernetes Secrets, Docker Compose env_file, etc.)
2. **Nunca em código, nunca em logs**
3. **Rotação periódica**

### CI/CD: GitHub Actions Secrets

No GitHub, use GitHub Secrets para armazenar valores sensíveis:

```yaml
env:
  DB_PASSWORD: ${{ secrets.PROD_DB_PASSWORD }}
  JWT_SECRET: ${{ secrets.PROD_JWT_SECRET }}
```

**Nunca** use `secrets.PROD_PASSWORD` em logs ou outputs.

## Checklist para Deploy

- [ ] Todas as variáveis obrigatórias estão definidas
- [ ] Nenhum `.env` real foi commitado
- [ ] ProductionConfigValidator passou (logs mostram "Production configuration validated")
- [ ] `application-prod.yml` usa apenas env vars, sem defaults de produção
- [ ] `.gitignore` inclui `**/.env`
- [ ] Segredos foram rotacionados há menos de 90 dias
- [ ] Logs não contêm senhas, tokens ou credenciais (logs sanitizados via `SensitiveDataMasker`)

## Rotação de Segredos

### Recomendação
- JWT_SECRET: a cada 6 meses ou após suspeita de exposição
- Database Password: a cada 3 meses
- AWS Keys: a cada 6 meses (ativar e desativar em paralelo)
- Grafana Admin Password: a cada 6 meses

### Procedure
1. Gere novo valor
2. Atualize em AWS Secrets Manager ou CI/CD
3. Implante em staging
4. Teste completamente
5. Implante em produção (com zero downtime se possível)

## Resposta a Exposição

Se um segredo foi exposto:

1. **Imediatamente:**
   - Revogar credenciais antigas (AWS keys, passwords)
   - Rotacionar segredo (JWT, database password)

2. **Nos próximos 15 minutos:**
   - Buscar logs de acesso com credencial exposta
   - Verificar se foram feitas ações não autorizadas

3. **Na próxima hora:**
   - Escrever relatório de incidente
   - Notificar time de segurança
   - Se dados foram acessados/modificados, considerar notificar clientes/reguladores

4. **Em 24 horas:**
   - Post-mortem
   - Melhorias (ex: adicionar audit, melhorar detecção)

## Referências

- [OWASP: Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [AWS Secrets Manager Best Practices](https://docs.aws.amazon.com/secretsmanager/latest/userguide/best-practices.html)
- [12 Factor App: Config](https://12factor.net/config)
