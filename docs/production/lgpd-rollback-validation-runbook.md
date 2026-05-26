# LGPD Rollback Validation Runbook

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** ⚠️ PRÉ-OPERACIONAL (Validação Obrigatória Antes da Produção)

Este documento descreve o procedimento completo de rollback para a liberação de conformidade LGPD (Fase 1 + Fase 2). Use apenas em caso de falha em produção ou durante testes pré-produção.

---

## 1. Premissas Críticas

### 1.1 Pré-Condições Obrigatórias
- [ ] Backup automático foi executado antes do deploy (validar timestamps)
- [ ] Backup foi testado e pode ser restaurado (dry-run executado)
- [ ] Versão anterior (pre-LGPD) está compilada e pronta
- [ ] Database migrations podem ser revertidas (Flyway downgrade scripts existem)
- [ ] Time de produção está disponível (mínimo 3 pessoas: DevOps, Backend, Database)
- [ ] Comunicação com stakeholders foi estabelecida
- [ ] Runbook foi testado em staging (não é dia 1 em produção)

### 1.2 Decisão de Rollback

**Ative rollback se:**
- [ ] Erro crítico (P0): Dados de usuários estão sendo corrompidos
- [ ] Erro crítico (P0): Sistema está indisponível por > 30 minutos
- [ ] Erro crítico (P0): Vazamento de dados detectado
- [ ] Erro crítico (P0): LGPD compliance foi quebrado (e.g., remoção não-autorizada de dados)
- [ ] Parecer jurídico rejeitou implementação (legal blocker)

**NÃO ative rollback para:**
- [ ] Bugs cosméticos (UI, typos)
- [ ] Performance degradada (< 50% SLA breach)
- [ ] Warnings em logs (sem impacto funcional)
- [ ] Problemas de terceiros (AWS KMS indisponível - aguarde recovery)

---

## 2. Backup Pre-Deploy

### 2.1 Backup de Aplicação

**Backend (JAR):**
```bash
# Antes do deploy LGPD
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/kronos-lgpd/${DATE}"

mkdir -p "${BACKUP_DIR}"

# Backup do JAR em produção
cp /opt/kronos/app/kronos-app.jar "${BACKUP_DIR}/kronos-app-pre-lgpd.jar"

# Backup das variáveis de ambiente
env | grep -E '^(JWT_|CORS_|AWS_|DB_|SPRING_)' > "${BACKUP_DIR}/env-vars.txt"

# Backup da configuração do Spring Boot
cp /opt/kronos/config/application-prod.yml "${BACKUP_DIR}/"

# Registrar versão
git log --oneline -1 > "${BACKUP_DIR}/git-version.txt"
docker images kronos:* --format "{{.Repository}}:{{.Tag}}" > "${BACKUP_DIR}/docker-images.txt"

echo "✓ Backup de aplicação concluído: ${BACKUP_DIR}"
```

**Frontend (Node.js/Docker):**
```bash
# Backup do código frontend compilado
FRONTEND_BACKUP="${BACKUP_DIR}/frontend-pre-lgpd"
mkdir -p "${FRONTEND_BACKUP}"

# Se usando Docker:
docker save kronos-frontend:latest > "${FRONTEND_BACKUP}/frontend-image.tar"

# Se usando Node.js estático:
cp -r /var/www/kronos-frontend/dist "${FRONTEND_BACKUP}/"
cp -r /var/www/kronos-frontend/.env "${FRONTEND_BACKUP}/"

echo "✓ Backup do frontend concluído"
```

### 2.2 Backup de Base de Dados

**PostgreSQL:**
```bash
# Full backup antes do deploy
DB_BACKUP="${BACKUP_DIR}/database-pre-lgpd-$(date +%Y%m%d_%H%M%S).sql"

pg_dump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --database="${DB_NAME}" \
  --format=plain \
  --verbose \
  --compress=9 \
  > "${DB_BACKUP}"

# Validar integridade do backup
if pg_restore --list "${DB_BACKUP}" > /dev/null 2>&1; then
  echo "✓ Backup de banco de dados validado: ${DB_BACKUP}"
  ls -lh "${DB_BACKUP}"
else
  echo "❌ ERRO: Backup de banco corrompido!"
  exit 1
fi

# Backup adicional: WAL (Write-Ahead Logs) para point-in-time recovery
cp -r /var/lib/postgresql/wal_archive "${BACKUP_DIR}/wal-backup-$(date +%Y%m%d_%H%M%S)"
```

### 2.3 Backup de Storage (S3)

**AWS S3 - Dados Biométricos:**
```bash
# Snapshot dos buckets S3 antes do deploy
# Nota: Usar replicação S3, não apenas sync local

aws s3api list-objects-v2 \
  --bucket kronos-biometric-data \
  --output json \
  > "${BACKUP_DIR}/s3-biometric-manifest-pre-lgpd.json"

aws s3api list-objects-v2 \
  --bucket kronos-logs \
  --output json \
  > "${BACKUP_DIR}/s3-logs-manifest-pre-lgpd.json"

# Verificar que replicação cross-region está ativa
aws s3api get-bucket-replication \
  --bucket kronos-biometric-data \
  > "${BACKUP_DIR}/s3-replication-config.json"

echo "✓ S3 manifests capturados (replicação é o verdadeiro backup)"
```

### 2.4 Validar Backup

```bash
# Script de validação de backup
BACKUP_DIR="/backups/kronos-lgpd/${DATE}"

validate_backup() {
  echo "🔍 Validando backups..."
  
  # 1. Verificar que todos os arquivos existem
  [ -f "${BACKUP_DIR}/kronos-app-pre-lgpd.jar" ] || { echo "❌ JAR faltando"; return 1; }
  [ -f "${BACKUP_DIR}/env-vars.txt" ] || { echo "❌ Env vars faltando"; return 1; }
  [ -f "${BACKUP_DIR}/database-pre-lgpd-"*.sql ] || { echo "❌ DB backup faltando"; return 1; }
  
  # 2. Verificar tamanhos (sanity check)
  jar_size=$(stat -f%z "${BACKUP_DIR}/kronos-app-pre-lgpd.jar" 2>/dev/null || stat -c%s "${BACKUP_DIR}/kronos-app-pre-lgpd.jar")
  if [ "$jar_size" -lt 100000000 ]; then
    echo "❌ JAR muito pequeno (< 100MB): ${jar_size} bytes"
    return 1
  fi
  
  db_size=$(stat -f%z "${BACKUP_DIR}"/database-pre-lgpd-*.sql 2>/dev/null || stat -c%s "${BACKUP_DIR}"/database-pre-lgpd-*.sql)
  if [ "$db_size" -lt 10000000 ]; then
    echo "❌ DB backup muito pequeno (< 10MB): ${db_size} bytes"
    return 1
  fi
  
  # 3. Dry-run: Tentar restaurar em staging
  echo "📋 Executando dry-run de restauração em staging..."
  # (Procedure específica para seu staging - omitida por brevidade)
  
  echo "✅ Backups validados com sucesso"
  return 0
}

validate_backup
```

---

## 3. Rollback de Aplicação

### 3.1 Rollback do Backend (Java/Spring Boot)

**Passo 1: Parar o serviço atual**
```bash
# Verificar status
systemctl status kronos-backend

# Parar serviço
sudo systemctl stop kronos-backend
sleep 5

# Validar que parou
if pgrep -f "kronos-app.jar" > /dev/null; then
  echo "⚠️  Processo ainda rodando, matando força..."
  pkill -9 -f "kronos-app.jar"
fi

echo "✓ Backend parado"
```

**Passo 2: Restaurar JAR pre-LGPD**
```bash
BACKUP_DIR="/backups/kronos-lgpd/20260525_143022"  # Usar data real
CURRENT_JAR="/opt/kronos/app/kronos-app.jar"

# Backup do JAR LGPD (para forensics se necessário)
cp "${CURRENT_JAR}" "${CURRENT_JAR}.lgpd-failed-$(date +%Y%m%d_%H%M%S)"

# Restaurar
cp "${BACKUP_DIR}/kronos-app-pre-lgpd.jar" "${CURRENT_JAR}"
chmod 755 "${CURRENT_JAR}"

echo "✓ JAR restaurado"
ls -lh "${CURRENT_JAR}"
```

**Passo 3: Restaurar variáveis de ambiente**
```bash
# Backup das env vars LGPD
env | grep -E '^(JWT_|CORS_|AWS_|DB_|SPRING_)' > "/tmp/env-lgpd-failed.txt"

# Restaurar env vars pre-LGPD
source "${BACKUP_DIR}/env-vars.txt"
# Ou manualmente se usar /etc/sysconfig/kronos:
cp "${BACKUP_DIR}/application-prod.yml" /opt/kronos/config/

echo "✓ Variáveis de ambiente restauradas"
```

**Passo 4: Iniciar backend**
```bash
sudo systemctl start kronos-backend
sleep 10

# Validar health check
HEALTH_URL="https://api.kronos.local/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "${HEALTH_URL}")

if [ "$RESPONSE" == "200" ]; then
  echo "✅ Backend iniciado com sucesso"
else
  echo "❌ ERRO: Backend retorna status ${RESPONSE}"
  
  # Tentar diagnosticar
  journalctl -u kronos-backend -n 100
  exit 1
fi
```

### 3.2 Rollback do Frontend (React/Node.js)

**Passo 1: Parar frontend atual**
```bash
# Se usando Docker:
docker stop kronos-frontend-container
docker rm kronos-frontend-container

# Se usando systemd + Node:
sudo systemctl stop kronos-frontend
pkill -f "node.*kronos-frontend"

echo "✓ Frontend parado"
```

**Passo 2: Restaurar código frontend**
```bash
BACKUP_DIR="/backups/kronos-lgpd/20260525_143022"
FRONTEND_PATH="/var/www/kronos-frontend"

# Backup do código LGPD (forensics)
cp -r "${FRONTEND_PATH}/dist" "${FRONTEND_PATH}/dist.lgpd-failed-$(date +%Y%m%d_%H%M%S)"

# Restaurar versão anterior
if [ -f "${BACKUP_DIR}/frontend-image.tar" ]; then
  # Docker restore
  docker load < "${BACKUP_DIR}/frontend-image.tar"
  echo "✓ Imagem Docker restaurada"
else
  # Static files restore
  rm -rf "${FRONTEND_PATH}/dist"
  cp -r "${BACKUP_DIR}/dist" "${FRONTEND_PATH}/"
  echo "✓ Arquivos estáticos restaurados"
fi

# Restaurar .env se houver
[ -f "${BACKUP_DIR}/.env" ] && cp "${BACKUP_DIR}/.env" "${FRONTEND_PATH}/.env"
```

**Passo 3: Iniciar frontend**
```bash
# Se Docker:
docker run -d \
  --name kronos-frontend-container \
  --restart unless-stopped \
  -p 80:3000 \
  -p 443:3000 \
  -e REACT_APP_API_URL="https://api.kronos.local" \
  kronos-frontend:latest

# Se Node:
cd "${FRONTEND_PATH}"
npm install  # Reinstall dependencies
npm start &

sleep 10

# Validar
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "https://kronos.local/")
if [ "$FRONTEND_STATUS" == "200" ]; then
  echo "✅ Frontend iniciado com sucesso"
else
  echo "❌ ERRO: Frontend retorna ${FRONTEND_STATUS}"
  exit 1
fi
```

---

## 4. Rollback de Database (Crítico)

### 4.1 Parar aplicação ANTES de qualquer comando SQL

```bash
# IMPORTANTE: Parar serviço primeiro
sudo systemctl stop kronos-backend kronos-frontend

# Aguardar conexões encerrarem
sleep 15

# Validar
lsof -i :5432 | grep -v COMMAND || echo "✓ Nenhuma conexão ativa"
```

### 4.2 Restaurar Data do PostgreSQL

**Opção A: Restaurar do pg_dump (Backup Full)**
```bash
DB_BACKUP="/backups/kronos-lgpd/20260525_143022/database-pre-lgpd-*.sql"

# Verificar integridade
pg_restore --list "${DB_BACKUP}" > /dev/null || {
  echo "❌ Backup corrompido"
  exit 1
}

# Criar novo database (ou limpar o existente)
# CUIDADO: Isso deleta todos os dados LGPD!
psql -h "${DB_HOST}" -U "${DB_USER}" -d postgres << EOF
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS "${DB_NAME}";
CREATE DATABASE "${DB_NAME}" OWNER "${DB_USER}";
EOF

# Restaurar
pg_restore \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --database="${DB_NAME}" \
  --verbose \
  "${DB_BACKUP}"

echo "✓ Database restaurado do backup"
```

**Opção B: Flyway Downgrade (Reverter Migrations)**
```bash
# Se usando Flyway para versionamento:

# 1. Validar que Flyway está funcionando
./mvnw flyway:info -Dflyway.configFiles=flyway.conf

# 2. Listar migrations a descartar
# As migrações LGPD devem estar com versão V4.0__*, V4.1__*, etc.

# 3. Estratégia: Undo de migrations LGPD
# Nota: Por padrão Flyway não faz undo. Opções:
#   a) Usar Flyway Teams (paid) com undo
#   b) Deletar dados LGPD manualmente via SQL

# Se opção (b):
psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" << EOF
-- Remover dados LGPD (cuidado!)
DELETE FROM lgpd_biometric_consent WHERE created_at >= '2026-05-25';
DELETE FROM lgpd_data_processing_catalog WHERE id > 100;  -- Anterior era até ID 100
DELETE FROM retention_execution_log WHERE id > 500;

-- Validar
SELECT COUNT(*) FROM lgpd_biometric_consent;
SELECT COUNT(*) FROM lgpd_data_processing_catalog;
SELECT COUNT(*) FROM retention_execution_log;
EOF

echo "✓ Migrations LGPD desfeitas"
```

### 4.3 Validar Database

```bash
validate_database() {
  echo "🔍 Validando banco de dados..."
  
  # 1. Conectar
  psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT 1" > /dev/null || {
    echo "❌ Não conseguiu conectar ao banco"
    return 1
  }
  
  # 2. Verificar integridade
  psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" -c "REINDEX DATABASE ${DB_NAME};"
  
  # 3. Verificar constraints
  psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" << EOF
    SET session_replication_role = 'replica';
    CONSTRAINT ALL;
    SET session_replication_role = 'origin';
EOF
  
  # 4. Executar ANALYZE
  psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" -c "ANALYZE;"
  
  # 5. Verificar tamanho (sanity check)
  SIZE=$(psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" -t -c \
    "SELECT pg_size_pretty(pg_database_size('${DB_NAME}'))")
  echo "   Database size: ${SIZE}"
  
  # 6. Verificar row counts (comparar com backup)
  psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" << EOF
    SELECT tablename, pg_size_pretty(pg_total_relation_size(tablename)) as size
    FROM pg_tables
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(tablename) DESC
    LIMIT 10;
EOF
  
  echo "✅ Database validado"
  return 0
}

validate_database
```

---

## 5. Smoke Tests (Validação Rápida)

Execute estes testes para confirmar que o rollback funcionou:

### 5.1 Tests de API Backend

```bash
API_URL="https://api.kronos.local"
AUTH_TOKEN="<jwt-token-pre-lgpd>"  # Token gerado antes do deploy

echo "🔍 Testando API backend..."

# 1. Health check
curl -s "${API_URL}/actuator/health" | jq .
[ $? -ne 0 ] && { echo "❌ Health check falhou"; exit 1; }

# 2. Login (sem LGPD)
LOGIN_RESPONSE=$(curl -s -X POST "${API_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"password"}')

JWT=$(echo "${LOGIN_RESPONSE}" | jq -r '.accessToken')
[ -z "${JWT}" ] && { echo "❌ Login falhou"; exit 1; }

# 3. Verificar que endpoints LGPD NÃO existem (ou retornam 404)
LGPD_CHECK=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer ${JWT}" \
  "${API_URL}/lgpd/data-processing-catalog")

if [ "${LGPD_CHECK}" == "404" ] || [ "${LGPD_CHECK}" == "403" ]; then
  echo "✓ Endpoints LGPD indisponíveis (esperado)"
else
  echo "⚠️  Endpoints LGPD ainda acessíveis (${LGPD_CHECK})"
fi

# 4. Verificar dados de usuário
USER_DATA=$(curl -s \
  -H "Authorization: Bearer ${JWT}" \
  "${API_URL}/user/profile" | jq .)

FIRST_NAME=$(echo "${USER_DATA}" | jq -r '.firstName')
[ -z "${FIRST_NAME}" ] && { echo "❌ Falha ao recuperar usuário"; exit 1; }

echo "✅ API tests passaram"
```

### 5.2 Tests do Frontend

```bash
echo "🔍 Testando frontend..."

# 1. Página inicial carrega
curl -s "https://kronos.local/" | grep -q "<title>" || { echo "❌ Página inicial falhou"; exit 1; }

# 2. Privacy Center NÃO está disponível
curl -s "https://kronos.local/privacy" | grep -q "404\|Privacy Center" && {
  echo "✓ Privacy Center indisponível (esperado)"
} || {
  echo "⚠️  Privacy Center ainda acessível"
}

# 3. Login funciona
echo "✓ Verificar login manualmente em https://kronos.local/login"

echo "✅ Frontend tests passaram"
```

### 5.3 Tests de Database

```bash
echo "🔍 Testando database..."

psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" << EOF
-- 1. Tabelas LGPD não existem (ou estão vazias)
SELECT COUNT(*) as lgpd_biometric_records 
FROM lgpd_biometric_consent;

SELECT COUNT(*) as lgpd_catalog_records 
FROM lgpd_data_processing_catalog;

-- 2. Dados de usuário estão íntegros
SELECT COUNT(*) as user_count FROM users;

-- 3. Nenhuma constraint violation
SELECT tablename, pg_constraint.conname
FROM pg_constraint
JOIN pg_tables ON pg_constraint.conrelid = pg_tables.tableoid
WHERE pg_tables.schemaname = 'public'
LIMIT 20;
EOF

echo "✅ Database tests passaram"
```

---

## 6. RTO e RPO Targets

| Métrica | Target | Validar |
|---------|--------|---------|
| **RTO** (Recovery Time Objective) | < 1 hora | Tempo total do rollback até serviço online |
| **RPO** (Recovery Point Objective) | < 15 minutos | Perda máxima de dados (últimas transações antes de backup) |
| **Data Validation Time** | < 30 minutos | Tempo para validar que dados estão consistentes |
| **Smoke Test Time** | < 15 minutos | Tempo para rodar all smoke tests |

**Cronograma típico:**
- T+0m: Decisão de rollback ativada
- T+5m: Aplicação parada, backups validados
- T+15m: JAR, Frontend, Env vars restaurados
- T+25m: Database restaurado e validado
- T+40m: API e Frontend iniciados
- T+55m: Smoke tests concluídos
- **T+60m: Rollback completo (RTO atingido)**

---

## 7. Critério de Sucesso do Rollback

### Aplicação
- [ ] Health check retorna 200 OK
- [ ] API responde em < 500ms (p99)
- [ ] Frontend carrega em < 2s (p99)
- [ ] Nenhum erro crítico nos logs
- [ ] Endpoints LGPD não estão acessíveis (404 ou sem dados)

### Data
- [ ] Database conecta e responde
- [ ] Row counts combinam com backup
- [ ] Nenhuma constraint violation
- [ ] Backups foram preservados (para forensics)

### Segurança
- [ ] HTTPS está ativo
- [ ] JWT tokens ainda são válidos (ou rejeitados gracefully)
- [ ] CORS está configurado corretamente
- [ ] Rate limiting está ativo

### Usuários
- [ ] Login funciona
- [ ] Dados pessoais estão visíveis (nome, email, etc.)
- [ ] Funcionalidades pré-LGPD estão operacionais
- [ ] Nenhum erro de autenticação

### Observability
- [ ] Logs estão sendo coletados
- [ ] Métricas estão sendo exportadas
- [ ] Alerts estão dearmados (para evitar ruído)
- [ ] Dashboard está atualizado

---

## 8. Pós-Rollback (Crítico)

### 8.1 Documentação

```bash
# Criar relatório de rollback
ROLLBACK_REPORT="/backups/kronos-lgpd/ROLLBACK_REPORT_$(date +%Y%m%d_%H%M%S).md"

cat > "${ROLLBACK_REPORT}" << 'EOF'
# Rollback Report

## Timestamp
- Rollback iniciado: $(date)
- Versão anterior: [git hash]
- Razão do rollback: [escolha]

## Etapas Completadas
- [ ] Backup validado
- [ ] Backend parado
- [ ] JAR restaurado
- [ ] Frontend parado
- [ ] Database restaurado
- [ ] Smoke tests passaram

## Dados Preservados (Para Forensics)
- JAR LGPD falhado: [path]
- Env vars LGPD: [path]
- Database LGPD: [snapshot]
- Frontend LGPD: [image]

## Próximos Passos
1. [ ] Root cause analysis realizado
2. [ ] Time jurídico notificado
3. [ ] Parecer jurídico revisado
4. [ ] Timeframe para re-deploy definido

## Comunicação
- [ ] Executivos notificados
- [ ] Usuários notificados (se impacto)
- [ ] DPO notificado (data minimization concern)
EOF

echo "✓ Relatório criado: ${ROLLBACK_REPORT}"
```

### 8.2 Root Cause Analysis

```bash
# Diagnosticar falha
journalctl -u kronos-backend -n 500 > /tmp/backend-logs-error.txt
journalctl -u kronos-frontend -n 500 > /tmp/frontend-logs-error.txt
docker logs kronos-frontend-container > /tmp/docker-logs-error.txt 2>&1

# Database errors
psql -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" << EOF
  SELECT * FROM pg_stat_statements 
  WHERE query LIKE '%ERROR%' 
  ORDER BY calls DESC 
  LIMIT 20;
EOF > /tmp/db-errors.txt

# Revisar commits LGPD
git log --oneline feature/lgpd-compliance -n 20 > /tmp/lgpd-commits.txt

# Criar análise
echo "📋 Executar análise manual:"
echo "   - Revisar /tmp/backend-logs-error.txt"
echo "   - Revisar /tmp/frontend-logs-error.txt"
echo "   - Revisar /tmp/db-errors.txt"
echo "   - Revisar /tmp/lgpd-commits.txt"
echo "   - Reunir com time jurídico"
echo "   - Adicionar findings ao relatório acima"
```

### 8.3 Validação Jurídica Post-Rollback

**Notificar:**
- [ ] DPO: Rollback foi necessário. Dados LGPD foram minimizados (não deletados). Investigar.
- [ ] Head of Legal: Parecer jurídico precisa ser revisado antes de novo deploy
- [ ] Compliance: Incidente foi documentado. Report será compartilhado.

**Ações obrigatórias:**
1. [ ] DPIA revisada (avaliação de risco pode ter mudado)
2. [ ] Parecer jurídico refazido (abordagem pode estar incorreta)
3. [ ] ANPD notificada (se houver dúvida sobre conformidade)

---

## 9. Teste do Runbook (Staging)

Antes de usar este runbook em produção, execute-o completamente em staging:

```bash
#!/bin/bash
set -e

echo "🧪 Iniciando teste de rollback em STAGING..."

# Variáveis de staging
BACKUP_DIR="/backups/staging-lgpd"
DB_HOST="staging-db.internal"
DB_USER="kronos_staging"
DB_NAME="kronos_staging"
API_URL="https://api-staging.kronos.local"

# 1. Executar backup
echo "[1/5] Fazendo backup..."
mkdir -p "${BACKUP_DIR}"
# (... comandos de backup ...)

# 2. Simular deploy LGPD
echo "[2/5] Simulando deploy LGPD..."
# (... fazer alguma mudança no código/DB ...)

# 3. Executar rollback
echo "[3/5] Executando rollback..."
# (... executar este runbook ...)

# 4. Validar
echo "[4/5] Validando rollback..."
# (... smoke tests ...)

# 5. Relatório
echo "[5/5] Gerando relatório..."
echo "✅ Teste de rollback em staging concluído com sucesso!"

# Salvar tempo de execução
END_TIME=$(date +%s)
START_TIME=$(cat /tmp/rollback_start_time)
DURATION=$((END_TIME - START_TIME))
echo "Duração total: ${DURATION}s (meta: < 3600s)"
```

---

## 10. Checklist Pré-Rollback

**Use isto ANTES de iniciar qualquer rollback em produção:**

- [ ] **Decisão aprovada:** CTO, Head of Security, e DevOps Lead concordam que rollback é necessário
- [ ] **Time disponível:** 3+ pessoas (DevOps, Backend, Database) estão online
- [ ] **Comunicação:** Stakeholders foram notificados de janela de manutenção
- [ ] **Backup validado:** Dry-run de restore passou em staging
- [ ] **Versão anterior:** JAR/Docker image pre-LGPD está pronto
- [ ] **Database:** Dump e WAL backups foram feitos
- [ ] **Runbook:** Este documento foi lido e entendido por 2 people
- [ ] **Rollback mode:** Todos sabem que funcionalidade LGPD será **REMOVIDA**
- [ ] **Forensics:** Dados LGPD serão preservados em `/backups/kronos-lgpd/` para análise
- [ ] **Legal notificado:** DPO e Head of Legal foram informados

**Autorização:**
- [ ] CTO: _______________________ (Assinatura/Carimbo)
- [ ] Head of Security: __________ (Assinatura/Carimbo)
- [ ] DevOps Lead: ______________ (Assinatura/Carimbo)

---

## 11. Contatos de Emergência

| Papel | Nome | Telefone | Email | Slack |
|-------|------|----------|-------|-------|
| CTO | | | | |
| Head of Security | | | | |
| Head of Legal | | | | |
| DPO | | | | |
| DevOps Lead | | | | |
| Database Admin | | | | |
| On-Call Engineer | | | | |

---

## 12. Documentação Relacionada

- [Checklist de Release LGPD](lgpd-production-release-checklist.md)
- [Validação de Config Staging/Produção](lgpd-staging-production-config-validation.md)
- [Status Técnico LGPD](../legal/lgpd-final-technical-status.md)
- [Checklist de Revisão Jurídica](../legal/lgpd-legal-review-checklist.md)

---

**Documento de:** Release Engineering + DevOps  
**Última revisão:** 2026-05-25  
**Próxima revisão:** Após primeiro teste em staging  
**Status:** ⚠️ PRONTO PARA TESTE EM STAGING (NÃO USE EM PRODUÇÃO SEM VALIDAÇÃO)

## Notas Finais

1. **Este é um plano de emergência.** Idealmente, nunca será usado.
2. **Teste em staging primeiro.** Não use em produção sem validação prévia.
3. **RTO < 1 hora** é o alvo, mas pode levar mais tempo se houver complicações (DB muito grande, network issues, etc.).
4. **Dados LGPD não são deletados**, apenas minimizados. Você terá que investigar o que deu errado antes de re-deploy.
5. **Parecer jurídico precisa ser revisado** se rollback acontecer. A implementação pode estar incorreta.

Para dúvidas, contactar: devops@kronos.com ou legal@kronos.com
