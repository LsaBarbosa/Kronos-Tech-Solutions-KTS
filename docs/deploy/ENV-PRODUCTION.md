# Configuração de Variáveis de Ambiente em Produção - Kronos Backend

## 🔐 Política de Segredos

**NUNCA commitar segredos no Git.** Todos os valores sensíveis devem ser configurados exclusivamente no servidor de produção.

## 📁 Local Recomendado para Variáveis

```bash
/etc/kronos/kronos-backend.env
```

## 🚀 Configuração na VPS Hostinger

### 1. Criar diretório de configuração

```bash
sudo mkdir -p /etc/kronos
```

### 2. Criar arquivo de variáveis de ambiente

```bash
sudo nano /etc/kronos/kronos-backend.env
```

### 3. Copiar template e ajustar valores reais

Use o arquivo `.env.example` como referência:

```bash
cp .env.example /tmp/kronos-backend.env
sudo mv /tmp/kronos-backend.env /etc/kronos/kronos-backend.env
sudo nano /etc/kronos/kronos-backend.env
```

### 4. Definir permissões seguras

```bash
sudo chown root:root /etc/kronos/kronos-backend.env
sudo chmod 600 /etc/kronos/kronos-backend.env
```

**Importante:** Com permissão `600` (read/write para root apenas), apenas o root pode ler o arquivo diretamente. Porém, quando usado via `EnvironmentFile=` no systemd, o daemon lê o arquivo durante o startup e injeta as variáveis no processo da aplicação. A aplicação não precisa ter permissão direta de leitura sobre o arquivo.

## 🔑 Gerando Valores Críticos

### JWT Secret

```bash
openssl rand -base64 64
```

Copie a saída e use como valor de `JWT_SECRET`. Deve ter no mínimo 64 caracteres.

### AWS Credentials

**Princípio de Menor Privilégio:** Crie um usuário ou role específico da aplicação com permissões mínimas.

**Procedimento:**
1. Acesse AWS IAM Console
2. Crie um usuário específico (ex: `kronos-backend-prod`)
3. **Não** use usuário administrador
4. Crie ou anexe uma policy com **apenas** as permissões necessárias:
   - S3: `GetObject`, `PutObject`, `DeleteObject` apenas no bucket usado
   - Rekognition: `CreateCollection`, `IndexFaces`, `DeleteFaces`, `SearchFacesByImage` apenas na collection usada
5. Gere access keys
6. Copie `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` para `/etc/kronos/kronos-backend.env`

**Nunca compartilhe estas credenciais por Slack, email ou chat.**
**Exemplo de policy restritiva:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::kronos-prod-documents/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "rekognition:CreateCollection",
        "rekognition:IndexFaces",
        "rekognition:DeleteFaces",
        "rekognition:SearchFacesByImage"
      ],
      "Resource": "arn:aws:rekognition:sa-east-1:ACCOUNT_ID:collection/kronos_prod"
    }
  ]
}
```

### Certificado Digital (se necessário)

Se a aplicação usar certificado digital para assinatura:

```bash
# Copiar para local seguro na VPS
sudo mkdir -p /etc/kronos/certs
sudo cp seu-certificado.p12 /etc/kronos/certs/
sudo chown root:root /etc/kronos/certs/seu-certificado.p12
sudo chmod 400 /etc/kronos/certs/seu-certificado.p12
```

## 🧪 Testar Variáveis Localmente (Antes de Deploy)

Antes de fazer deploy, teste se as variáveis estão sendo lidas corretamente:

```bash
set -a
source /etc/kronos/kronos-backend.env
set +a

# Verificar algumas variáveis
echo "DB_HOST: $DB_HOST"
echo "JWT_SECRET: ${JWT_SECRET:0:10}..." # Mostra apenas os primeiros 10 chars
echo "AWS_REGION: $AWS_REGION"
```

## 🐳 Iniciar com Docker

Se estiver rodando em container Docker, passe o arquivo de ambiente:

```bash
docker run -d \
  --name kronos-backend \
  --env-file /etc/kronos/kronos-backend.env \
  -p 8080:8080 \
  seu-registry/kronos-backend:latest
```

**Nota sobre armazenamento de documentos:**
- Se a aplicação usar **volume local** para documentos, adicione: `-v /opt/kronos/documents:/opt/kronos/documents`
- Se a aplicação usar **AWS S3**, nenhum volume adicional é necessário (credenciais vêm de `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`)

Verifique qual estratégia está configurada em `FILE_STORAGE_ROOT_PATH` antes de adicionar volumes.

## 🔧 Iniciar com Systemd (Recomendado)

Veja o arquivo `kronos-backend.service.example` para configuração do systemd.

Depois de copiar e ajustar:

```bash
sudo cp kronos-backend.service.example /etc/systemd/system/kronos-backend.service
sudo systemctl daemon-reload
sudo systemctl enable kronos-backend
sudo systemctl start kronos-backend
sudo systemctl status kronos-backend
```

## 📋 Checklist Pré-Deployment

- [ ] Arquivo `/etc/kronos/kronos-backend.env` criado
- [ ] Permissões definidas como `600`
- [ ] Variáveis testadas localmente com `source`
- [ ] `JWT_SECRET` tem no mínimo 64 caracteres
- [ ] `JPA_DDL_AUTO=validate` (nunca use `create`, `create-drop` ou `update` em produção)
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] Nenhum uso restante de `DB_USER` (use `DB_USERNAME` em todos os scripts)
- [ ] `CORS_ALLOWED_ORIGINS` aponta apenas para domínios reais (não use `*`)
- [ ] Todas as URLs apontam para produção (não localhost)
- [ ] Credenciais AWS usam IAM com menor privilégio
- [ ] Email/SMTP configurado e testado
- [ ] Certificado digital (se necessário) em `/etc/kronos/certs/`
- [ ] PostgreSQL acessível com credenciais testadas
- [ ] Se usar volume local: `/opt/kronos/documents` existe com espaço livre
- [ ] Se usar S3: bucket existe e credenciais têm acesso
- [ ] Build do Docker foi bem-sucedido
- [ ] Systemd service está copiado e habilitado

## 🔍 Monitorar Após Deployment

Após iniciar a aplicação:

```bash
# Ver logs da aplicação
journalctl -u kronos-backend -f

# Verificar se está escutando na porta 8080
sudo ss -tlnp | grep ':8080'

# Testar endpoint de health
curl http://localhost:8080/actuator/health

# Se receiver 404, verifique se o actuator está configurado em application-prod.yml
# e se o endpoint 'health' está incluso em MANAGEMENT_ENDPOINTS_WEB_EXPOSURE
```

## 🚨 Em Caso de Erro

### Erro: "Missing required production environment variable"

**Causa:** Uma variável obrigatória não foi definida.

**Solução:**
1. Editar `/etc/kronos/kronos-backend.env`
2. Adicionar a variável faltante
3. Recarregar: `sudo systemctl restart kronos-backend`

### Erro: "JWT_SECRET must have at least 64 characters"

**Causa:** JWT_SECRET é muito curto.

**Solução (sem duplicação):**
```bash
NEW_JWT_SECRET=$(openssl rand -base64 64)

if grep -q "^JWT_SECRET=" /etc/kronos/kronos-backend.env; then
  # Se a variável já existe, substitua
  sudo sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" /etc/kronos/kronos-backend.env
else
  # Se não existe, adicione
  echo "JWT_SECRET=$NEW_JWT_SECRET" | sudo tee -a /etc/kronos/kronos-backend.env > /dev/null
fi

sudo systemctl restart kronos-backend
```

### Erro: Conexão com banco de dados recusada

**Causa:** PostgreSQL não está acessível ou credenciais estão erradas.

**Solução:**
```bash
set -a
source /etc/kronos/kronos-backend.env
set +a

# Testar conexão ao banco (use PGPASSWORD para evitar prompt)
PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  -c "SELECT 1;"

# Se o comando falhar, editar as credenciais em /etc/kronos/kronos-backend.env
# Verifique especialmente DB_HOST, DB_PORT, DB_USERNAME e DB_PASSWORD
```

## 📚 Referências

- [Spring Boot Environment Variables](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
