# Checklist de Deploy - Hostinger VPS - Kronos Backend

**Versão:** 1.0  
**Data:** 2026-05-16  
**Status:** ⚠️ Para ser executado na VPS real

---

## 📋 Pré-requisitos

- [ ] Acesso SSH à VPS Hostinger
- [ ] Linux (Debian/Ubuntu recomendado)
- [ ] Java 17+ instalado: `java -version`
- [ ] PostgreSQL instalado e acessível
- [ ] systemd disponível: `systemctl --version`

---

## 1️⃣ Configuração de Usuário e Diretórios

### Usuário `kronos`

- [ ] Usuário `kronos` criado: `id kronos`
- [ ] Grupo `kronos` criado: `groups kronos`
- [ ] Shell do usuário: `/bin/bash` (para poder testar variáveis)

### Diretórios

- [ ] `/opt/kronos/backend` criado: `sudo mkdir -p /opt/kronos/backend`
- [ ] `/opt/kronos/documents` criado (se usar storage local): `sudo mkdir -p /opt/kronos/documents`
- [ ] `/etc/kronos` criado: `sudo mkdir -p /etc/kronos`
- [ ] Proprietário correto: `sudo chown -R kronos:kronos /opt/kronos`
- [ ] Permissões corretas: `sudo chmod 750 /opt/kronos/backend /opt/kronos/documents`

---

## 2️⃣ Arquivo de Configuração de Ambiente

### Criação

- [ ] Arquivo `/etc/kronos/kronos-backend.env` copiado de `.env.example`
- [ ] Permissão: `sudo chmod 600 /etc/kronos/kronos-backend.env`
- [ ] Proprietário: `sudo chown root:root /etc/kronos/kronos-backend.env`

### Variáveis Obrigatórias

**Spring & Server:**
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `SERVER_PORT=8080`
- [ ] `APP_NAME=kronos-backend`
- [ ] `APP_ENV=prod`

**Database (PostgreSQL):**
- [ ] `DB_HOST=<seu-host>`
- [ ] `DB_PORT=5432`
- [ ] `DB_NAME=<seu-banco>`
- [ ] `DB_USERNAME=<seu-usuario>` (NOT `DB_USER`)
- [ ] `DB_PASSWORD=<sua-senha>`

**JWT & Security:**
- [ ] `JWT_SECRET=<gerado-com-openssl-rand-base64-64>` (≥64 caracteres)
- [ ] `JWT_EXPIRATION=900000`
- [ ] `SECRET_TERM=<gerado-com-openssl-rand-base64-48>` (≥32 caracteres, não placeholder)

**Frontend URLs:**
- [ ] `FRONTEND_BASE_URL_PLATAFORM=<seu-dominio>`
- [ ] `FRONTEND_BASE_URL_RECORD=<seu-dominio>`
- [ ] `FRONTEND_ALLOWED_ORIGINS=<apenas-dominios-reais>` (NÃO use `*`)

**AWS (Obrigatório):**
- [ ] `AWS_REGION=sa-east-1`
- [ ] `AWS_ACCESS_KEY_ID=<sua-chave>` (IAM com menor privilégio)
- [ ] `AWS_SECRET_ACCESS_KEY=<sua-chave-secreta>`
- [ ] `AWS_S3_BUCKET_NAME=<seu-bucket>`
- [ ] `AWS_REKOGNITION_COLLECTION_ID=<sua-collection>`

**Email (SMTP):**
- [ ] `MAIL_HOST=<seu-smtp>`
- [ ] `MAIL_PORT=587`
- [ ] `MAIL_USERNAME=<seu-usuario>`
- [ ] `MAIL_PASSWORD=<sua-senha>`

**File Storage (Escolha um):**
- [ ] Se usar S3: deixe `FILE_STORAGE_ROOT_PATH` vazio ou omita
- [ ] Se usar storage local: `FILE_STORAGE_ROOT_PATH=/opt/kronos/documents`

---

## 3️⃣ Validações Locais (na VPS)

### Carregar Variáveis

```bash
set -a
source /etc/kronos/kronos-backend.env
set +a
```

- [ ] Comando executado sem erros

### Verificar Variáveis Críticas

```bash
echo "DB_HOST: $DB_HOST"
echo "DB_NAME: $DB_NAME"
echo "DB_USERNAME: $DB_USERNAME"
echo "JWT_SECRET (primeiros 10): ${JWT_SECRET:0:10}..."
echo "SECRET_TERM (primeiros 10): ${SECRET_TERM:0:10}..."
echo "AWS_REGION: $AWS_REGION"
echo "FRONTEND_ALLOWED_ORIGINS: $FRONTEND_ALLOWED_ORIGINS"
# NÃO exiba: DB_PASSWORD, AWS_SECRET_ACCESS_KEY, MAIL_PASSWORD
```

- [ ] Todas as variáveis exibidas corretamente

### PostgreSQL Acessível

```bash
PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  -c "SELECT 1;"
```

Esperado: `SELECT 1` com resultado `1`

- [ ] Conexão bem-sucedida

---

## 4️⃣ JAR e Configuração Systemd

### JAR Copiado

- [ ] JAR copiado para `/opt/kronos/backend/kronos-backend.jar`
- [ ] Proprietário: `sudo chown kronos:kronos /opt/kronos/backend/kronos-backend.jar`
- [ ] Permissão: `sudo chmod 755 /opt/kronos/backend/kronos-backend.jar`

### Systemd Service

- [ ] Arquivo copiado: `sudo cp docs/deploy/kronos-backend.service.example /etc/systemd/system/kronos-backend.service`
- [ ] Ajustes realizados (se necessário):
  - `WorkingDirectory=/opt/kronos/backend`
  - `User=kronos`
  - `Group=kronos`
  - `ExecStart=/usr/bin/java -jar /opt/kronos/backend/kronos-backend.jar`
  - `EnvironmentFile=/etc/kronos/kronos-backend.env`
- [ ] Service habilitado: `sudo systemctl daemon-reload && sudo systemctl enable kronos-backend`
- [ ] Service iniciado: `sudo systemctl start kronos-backend`

---

## 5️⃣ Validações Após Startup

### Status do Service

```bash
sudo systemctl status kronos-backend
```

Esperado: `Active: active (running)`

- [ ] Service rodando

### Logs

```bash
journalctl -u kronos-backend -f
```

Esperado: Logs de startup, sem erros críticos

- [ ] Logs visíveis
- [ ] Sem erros de variáveis faltantes
- [ ] Sem erros de conexão com banco

### Porta Escutando

```bash
sudo ss -tlnp | grep ':8080'
```

Esperado: `LISTEN` na porta 8080

- [ ] Porta 8080 escutando

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Esperado: `{"status":"UP"}`

- [ ] Health endpoint respondendo

---

## 6️⃣ Validações de Funcionalidades

### PostgreSQL

- [ ] Aplicação consegue conectar (veja logs)
- [ ] Migrações Flyway executadas (veja logs)
- [ ] Nenhum erro de schema mismatch

### SMTP

Se enviada alguma notificação:

- [ ] Email chega na caixa de entrada real
- [ ] Sem erros de autenticação nos logs

### AWS S3

Se usando S3 (recomendado):

```bash
# Tente fazer upload de um documento via API
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <seu-token>" \
  -F "file=@/tmp/test.pdf"
```

- [ ] Upload bem-sucedido
- [ ] Arquivo visível no S3 bucket

### AWS Rekognition

Se usando biometria:

```bash
# Tente indexar uma face via API
curl -X POST http://localhost:8080/api/biometry/enroll \
  -H "Authorization: Bearer <seu-token>" \
  -F "image=@/tmp/face.jpg"
```

- [ ] Indexação bem-sucedida
- [ ] Nenhum erro de Rekognition nos logs

### CORS

Se usando navegador:

```javascript
// No console do navegador, no seu domínio frontend:
fetch('http://localhost:8080/actuator/health', {
  method: 'GET',
  credentials: 'include'
})
.then(r => r.json())
.then(console.log)
.catch(console.error)
```

Esperado: Resposta com `{"status":"UP"}` (sem erro de CORS)

- [ ] CORS funcionando com domínio real

---

## 7️⃣ Storage de Documentos

### Se usar S3 (Recomendado)

- [ ] IAM User criado com menor privilégio
- [ ] Policy permite apenas `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` no bucket usado
- [ ] Policy permite apenas `rekognition:*` na collection usada
- [ ] Nenhuma permissão de admin

### Se usar Storage Local

- [ ] Diretório `/opt/kronos/documents` criado
- [ ] Espaço em disco verificado: `df -h /opt/kronos/documents`
- [ ] Proprietário: `kronos:kronos`
- [ ] Permissão: `750`
- [ ] Systemd service tem `ReadWritePaths=/opt/kronos/documents` descomentado

---

## 8️⃣ Certificado Digital (se necessário)

Se a aplicação assina documentos:

- [ ] Certificado `.p12` copiado para `/etc/kronos/certs/`
- [ ] Proprietário: `sudo chown root:root /etc/kronos/certs/*.p12`
- [ ] Permissão: `sudo chmod 400 /etc/kronos/certs/*.p12`
- [ ] Senha do certificado em `/etc/kronos/kronos-backend.env`: `DIGITAL_CERTIFICATE_PASSWORD`

---

## 9️⃣ Monitoramento Contínuo

### Logs em Tempo Real

```bash
journalctl -u kronos-backend -f
```

- [ ] Monitorado durante uso normal
- [ ] Sem erros repetidos

### Health Check Periódico

```bash
watch -n 5 'curl -s http://localhost:8080/actuator/health | jq .'
```

- [ ] Status sempre `UP`

### Restart Automático

Teste killando o processo:

```bash
sudo systemctl kill kronos-backend
sleep 5
sudo systemctl status kronos-backend
```

Esperado: Service reinicia automaticamente

- [ ] Service reiniciou

---

## 🔟 Troubleshooting

### Erro: "Missing required production environment variable"

**Causa:** Variável obrigatória não definida

**Solução:**
1. Editar `/etc/kronos/kronos-backend.env`
2. Adicionar a variável faltante
3. Restart: `sudo systemctl restart kronos-backend`

- [ ] Corrigido

### Erro: "Conexão com banco recusada"

**Causa:** PostgreSQL não acessível ou credenciais erradas

**Solução:**
```bash
set -a
source /etc/kronos/kronos-backend.env
set +a
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "${DB_PORT:-5432}" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;"
```

Se falhar, revisar DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD

- [ ] Corrigido

### Erro: "JWT_SECRET must have at least 64 characters"

**Causa:** JWT_SECRET muito curto

**Solução:**
```bash
NEW_JWT_SECRET=$(openssl rand -base64 64)

if grep -q "^JWT_SECRET=" /etc/kronos/kronos-backend.env; then
  sudo sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" /etc/kronos/kronos-backend.env
else
  echo "JWT_SECRET=$NEW_JWT_SECRET" | sudo tee -a /etc/kronos/kronos-backend.env > /dev/null
fi

sudo systemctl restart kronos-backend
```

- [ ] Corrigido

### Erro: "SECRET_TERM must be at least 32 characters"

**Causa:** SECRET_TERM muito curto ou placeholder

**Solução:**
```bash
NEW_SECRET_TERM=$(openssl rand -base64 48)

if grep -q "^SECRET_TERM=" /etc/kronos/kronos-backend.env; then
  sudo sed -i "s|^SECRET_TERM=.*|SECRET_TERM=$NEW_SECRET_TERM|" /etc/kronos/kronos-backend.env
else
  echo "SECRET_TERM=$NEW_SECRET_TERM" | sudo tee -a /etc/kronos/kronos-backend.env > /dev/null
fi

sudo systemctl restart kronos-backend
```

- [ ] Corrigido

### Erro: "CORS não permite origin"

**Causa:** FRONTEND_ALLOWED_ORIGINS não contém seu domínio frontend

**Solução:**
1. Editar `/etc/kronos/kronos-backend.env`
2. Adicionar seu domínio: `FRONTEND_ALLOWED_ORIGINS=https://seu-dominio.com.br`
3. Restart: `sudo systemctl restart kronos-backend`

- [ ] Corrigido

---

## 7️⃣ Configuração do Nginx (API Gateway)

### Instalação

- [ ] Nginx instalado: `sudo apt-get install nginx`
- [ ] Nginx habilitado: `sudo systemctl enable nginx`
- [ ] Nginx iniciado: `sudo systemctl start nginx`

### Configuração de API Gateway (LGPD)

**Path Standardization Pattern:**
- External (Client): `/api/lgpd/**`
- Internal (Spring): `/lgpd/**`
- Nginx rewrites `/api/lgpd/**` → `/lgpd/**`

**Configuração:**
- [ ] Arquivo `deploy/hostinger-nginx.conf` copiado para `/etc/nginx/sites-available/api.seu-dominio.com`
- [ ] Symlink criado: `sudo ln -s /etc/nginx/sites-available/api.seu-dominio.com /etc/nginx/sites-enabled/`
- [ ] Location rule para LGPD paths presente:
  ```nginx
  location ~ ^/api/lgpd/(.*)$ {
    rewrite ^/api/lgpd/(.*)$ /lgpd/$1 break;
    proxy_pass http://127.0.0.1:8080;
    ...
  }
  ```
- [ ] Headers X-Forwarded-* configurados:
  ```nginx
  proxy_set_header X-Forwarded-Host $host;
  proxy_set_header X-Forwarded-Proto https;
  proxy_set_header X-Original-URI $request_uri;
  ```

### Certificados SSL/TLS

- [ ] Certificados gerados/renovados (Let's Encrypt recomendado):
  ```bash
  sudo certbot certonly -d api.seu-dominio.com
  ```
- [ ] Paths atualizados em `nginx.conf`:
  ```nginx
  ssl_certificate /etc/letsencrypt/live/api.seu-dominio.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/api.seu-dominio.com/privkey.pem;
  ```

### Validação Nginx

- [ ] Sintaxe validada: `sudo nginx -t`
- [ ] Config recarregada: `sudo systemctl reload nginx`
- [ ] Path rewrite testado:
  ```bash
  curl -v https://api.seu-dominio.com/api/lgpd/inventory
  # Deve retornar 200 com dados de inventory (ou 401 se não autenticado)
  ```
- [ ] Headers verificados:
  ```bash
  curl -v https://api.seu-dominio.com/api/lgpd/inventory 2>&1 | grep "X-Forwarded"
  # Backend deve receber X-Forwarded-Host, X-Forwarded-Proto, etc
  ```

---

## ✅ Conclusão

Quando todos os itens estiverem marcados com ✅, o deploy está completo e validado na VPS Hostinger.

**Próximas ações:**
- Monitorar health check continuamente
- Revisar logs regularmente
- Fazer backup do banco de dados
- Documentar qualquer configuração custom para recuperação de desastres

---

**Documento criado:** 2026-05-16  
**Última atualização:** 2026-05-16  
**Validação:** Pendente de execução na VPS real
