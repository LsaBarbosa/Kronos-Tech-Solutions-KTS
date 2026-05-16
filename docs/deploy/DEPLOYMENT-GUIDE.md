# Guia de Deploy - Kronos Backend em Hostinger VPS

**Versão:** 2.0  
**Atualizado:** 2026-05-16  
**Validação:** ⚠️ Pendente de execução na VPS real

---

## 📌 Status de Prontidão

### ✅ Prontos

- Código-fonte está compilável e testado (875 testes passando)
- Variáveis de ambiente externalizadas corretamente
- Nenhum segredo real versionado no repositório
- Validações de configuração produção implementadas
- Documentação operacional completa
- Suporte a Hostinger VPS (não Render)

### ⚠️ Pendentes (Requerem Validação na VPS Real)

- Conectividade com PostgreSQL real
- SMTP com credenciais reais
- AWS S3/Rekognition com IAM real
- CORS com domínio real do frontend
- Storage de documentos (decisão S3 vs local)
- systemd service startup
- Health checks
- Logs via journalctl

**Conclusão:** O deploy está **tecnicamente pronto**. A prontidão de **produção real** depende da validação operacional na VPS.

---

## 🚀 Preparação da VPS Hostinger

### 1. Acesso SSH

```bash
ssh -i sua-chave.pem usuario@seu-vps.com
```

### 2. Atualizar Sistema

```bash
sudo apt update
sudo apt upgrade -y
```

### 3. Instalar Pré-requisitos

**Java 17+**

```bash
sudo apt install -y openjdk-17-jre-headless
java -version
```

**PostgreSQL (se não instalado)**

```bash
sudo apt install -y postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**systemd (já vem por padrão)**

```bash
systemctl --version
```

### 4. Criar Usuário `kronos`

```bash
sudo useradd -m -s /bin/bash kronos
sudo passwd kronos  # Defina uma senha
id kronos
```

### 5. Criar Diretórios

```bash
sudo mkdir -p /opt/kronos/backend
sudo mkdir -p /opt/kronos/documents  # Se usar storage local
sudo mkdir -p /etc/kronos
sudo mkdir -p /etc/kronos/certs      # Se usar certificado digital

sudo chown -R kronos:kronos /opt/kronos
sudo chmod 750 /opt/kronos/backend
sudo chmod 750 /opt/kronos/documents
```

---

## 🔐 Configuração de Ambiente

### 1. Copiar Template

```bash
# Do seu computador local ou clone do Git:
cp .env.example /tmp/kronos-backend.env
sudo mv /tmp/kronos-backend.env /etc/kronos/kronos-backend.env
```

### 2. Editar Arquivo de Configuração

```bash
sudo nano /etc/kronos/kronos-backend.env
```

Substituir todos os `change-me` pelos valores reais:

**Banco de dados:**
```bash
DB_HOST=localhost  # ou IP do servidor PostgreSQL
DB_PORT=5432
DB_NAME=kronos_prod
DB_USERNAME=kronos_user
DB_PASSWORD=<senha-segura-do-postgres>
```

**JWT (gerar com):**
```bash
openssl rand -base64 64
```

**SECRET_TERM (gerar com):**
```bash
openssl rand -base64 48
```

**Frontend:**
```bash
FRONTEND_BASE_URL_PLATAFORM=https://seu-dominio.com.br
FRONTEND_BASE_URL_RECORD=https://seu-dominio.com.br
FRONTEND_ALLOWED_ORIGINS=https://seu-dominio.com.br,https://www.seu-dominio.com.br
```

**AWS (usar IAM com menor privilégio):**
```bash
AWS_REGION=sa-east-1
AWS_ACCESS_KEY_ID=<sua-chave>
AWS_SECRET_ACCESS_KEY=<sua-chave-secreta>
AWS_S3_BUCKET_NAME=seu-bucket
AWS_REKOGNITION_COLLECTION_ID=kronos_prod
```

**Email:**
```bash
MAIL_HOST=smtp.seu-provedor.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@seu-dominio.com
MAIL_PASSWORD=<sua-senha-smtp>
```

**Storage:**
```bash
# Opção 1: AWS S3 (recomendado para produção)
# FILE_STORAGE_ROOT_PATH=/opt/kronos/documents  # deixe vazio, usa S3

# Opção 2: Storage Local
FILE_STORAGE_ROOT_PATH=/opt/kronos/documents
```

### 3. Proteger Arquivo

```bash
sudo chown root:root /etc/kronos/kronos-backend.env
sudo chmod 600 /etc/kronos/kronos-backend.env
```

### 4. Validar Variáveis

```bash
set -a
source /etc/kronos/kronos-backend.env
set +a

echo "DB_HOST: $DB_HOST"
echo "JWT_SECRET (primeiros 10): ${JWT_SECRET:0:10}..."
echo "SECRET_TERM (primeiros 10): ${SECRET_TERM:0:10}..."
echo "AWS_REGION: $AWS_REGION"
echo "FRONTEND_ALLOWED_ORIGINS: $FRONTEND_ALLOWED_ORIGINS"
```

### 5. Testar Conexão com PostgreSQL

```bash
PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  -c "SELECT 1;"
```

Se funcionar, verá: `SELECT 1; (1 row)`

---

## 📦 Deploy da Aplicação

### 1. Copiar JAR

```bash
# Do seu computador local ou pipeline CI/CD:
scp -i sua-chave.pem kronos-0.0.1-SNAPSHOT.jar usuario@seu-vps.com:/tmp/

# Na VPS:
sudo cp /tmp/kronos-0.0.1-SNAPSHOT.jar /opt/kronos/backend/kronos-backend.jar
sudo chown kronos:kronos /opt/kronos/backend/kronos-backend.jar
sudo chmod 755 /opt/kronos/backend/kronos-backend.jar
```

### 2. Configurar Systemd Service

```bash
# Copiar template
sudo cp docs/deploy/kronos-backend.service.example /etc/systemd/system/kronos-backend.service

# Revisar e ajustar se necessário
sudo nano /etc/systemd/system/kronos-backend.service
```

Verificar:
```ini
[Service]
User=kronos
Group=kronos
WorkingDirectory=/opt/kronos/backend
EnvironmentFile=/etc/kronos/kronos-backend.env
ExecStart=/usr/bin/java -jar /opt/kronos/backend/kronos-backend.jar
```

### 3. Ativar e Iniciar Service

```bash
sudo systemctl daemon-reload
sudo systemctl enable kronos-backend
sudo systemctl start kronos-backend
sudo systemctl status kronos-backend
```

Esperado:
```
● kronos-backend.service - Kronos Backend Service
     Loaded: loaded (/etc/systemd/system/kronos-backend.service; enabled; vendor preset: enabled)
     Active: active (running) since ...
```

### 4. Monitorar Logs

```bash
journalctl -u kronos-backend -f
```

Procure por:
- `Started KronosApplication` ✅
- Nenhum erro de variáveis faltantes
- Nenhum erro de conexão com banco

---

## ✅ Validações Pós-Deploy

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Esperado:
```json
{"status":"UP"}
```

### Porta Escutando

```bash
sudo ss -tlnp | grep ':8080'
```

Esperado:
```
LISTEN ... java ... :8080
```

### Teste de Conectividade (do seu cliente)

```bash
curl https://seu-dominio.com.br/actuator/health
```

(Substitua `seu-dominio.com.br` pelo seu domínio real)

---

## 🔄 Operações Comuns

### Reiniciar Service

```bash
sudo systemctl restart kronos-backend
```

### Ver Logs Recentes

```bash
journalctl -u kronos-backend -n 100
```

### Parar Service

```bash
sudo systemctl stop kronos-backend
```

### Iniciar Service

```bash
sudo systemctl start kronos-backend
```

### Atualizar Aplicação

```bash
# 1. Para o service
sudo systemctl stop kronos-backend

# 2. Faz backup do JAR antigo
sudo cp /opt/kronos/backend/kronos-backend.jar /opt/kronos/backend/kronos-backend.jar.bak

# 3. Copia novo JAR
sudo cp /tmp/novo-kronos.jar /opt/kronos/backend/kronos-backend.jar
sudo chown kronos:kronos /opt/kronos/backend/kronos-backend.jar
sudo chmod 755 /opt/kronos/backend/kronos-backend.jar

# 4. Inicia o service
sudo systemctl start kronos-backend

# 5. Monitora logs
journalctl -u kronos-backend -f
```

---

## 🚨 Troubleshooting

### Erro: "Missing required production environment variable"

**Solução:**
1. Verificar qual variável falta nos logs
2. Editar `/etc/kronos/kronos-backend.env`
3. Adicionar a variável
4. Restart: `sudo systemctl restart kronos-backend`

### Erro: "Conexão com banco recusada"

**Solução:**
```bash
# Testar conexão manualmente
PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  -c "SELECT 1;"
```

Se falhar, verificar:
- PostgreSQL está rodando: `sudo systemctl status postgresql`
- Credenciais corretas
- Host acessível
- Firewall liberado

### Erro: "Port 8080 already in use"

**Solução:**
```bash
sudo lsof -i :8080  # Ver processo usando porta
sudo kill -9 <PID>   # Matar processo se necessário
sudo systemctl restart kronos-backend
```

### Erro: "CORS error"

**Solução:**
1. Verificar `FRONTEND_ALLOWED_ORIGINS` em `/etc/kronos/kronos-backend.env`
2. Usar HTTPS se aplicável
3. Não usar `*` em produção
4. Restart: `sudo systemctl restart kronos-backend`

---

## 📊 Monitoramento Recomendado

### Verificar Saúde Periodicamente

```bash
# Cron job: verificar a cada 5 minutos
*/5 * * * * /usr/bin/curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1 || /usr/bin/systemctl restart kronos-backend
```

### Alertas de Log

```bash
# Procurar por erros críticos
journalctl -u kronos-backend | grep ERROR
```

---

## 🔐 Segurança

### Nunca

- ❌ Commitar `.env` com segredos reais no Git
- ❌ Usar `DB_USER` (use `DB_USERNAME`)
- ❌ Deixar CORS aberto com `*`
- ❌ Usar senhas simples
- ❌ Rodar service como `root`
- ❌ Deixar arquivo de configuração com permissão > 600

### Sempre

- ✅ Usar variáveis de ambiente externalizadas
- ✅ Proteger `/etc/kronos/kronos-backend.env` com `chmod 600`
- ✅ Usar IAM com menor privilégio na AWS
- ✅ Validar FRONTEND_ALLOWED_ORIGINS com domínios reais
- ✅ Rotar credenciais regularmente
- ✅ Fazer backup de configurações críticas

---

## 📚 Referências

- [Spring Boot Environment Variables](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [systemd Documentation](https://www.freedesktop.org/software/systemd/man/)
- [PostgreSQL Connection](https://www.postgresql.org/docs/current/app-psql.html)
- [AWS IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)

---

## 📋 Checklist Rápido

Para cada deploy:

1. [ ] Variáveis testadas: `source /etc/kronos/kronos-backend.env`
2. [ ] Banco acessível: `psql ... -c "SELECT 1;"`
3. [ ] JAR copiado: `ls -lh /opt/kronos/backend/kronos-backend.jar`
4. [ ] Service reiniciado: `sudo systemctl restart kronos-backend`
5. [ ] Logs monitorados: `journalctl -u kronos-backend -f`
6. [ ] Health check: `curl http://localhost:8080/actuator/health`
7. [ ] Port escutando: `ss -tlnp | grep 8080`

---

**Última atualização:** 2026-05-16  
**Versão:** 2.0  
**Próxima validação:** Na VPS real conforme checklist HOSTINGER-DEPLOY-CHECKLIST.md
