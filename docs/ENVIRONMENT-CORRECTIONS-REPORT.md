# Relatório de Correção de Variáveis de Ambiente - Kronos Backend

**Data:** 2026-05-15  
**Branch analisada:** PROD_HOSTINGER (commit 96a850a)  
**Status:** ✅ COMPLETO - Build SUCCESSFUL, Testes PASSING

---

## 📊 Resumo Executivo

Revisão completa de consistência e segurança de variáveis de ambiente conforme 22 pontos obrigatórios. Todas as inconsistências foram corrigidas, documentação foi profissionalizada para deploy em VPS Hostinger com systemd.

---

## 🔄 Inconsistências Corrigidas

### 1. **DB_USER → DB_USERNAME (Crítica)**

Padronização completa de `DB_USER` para `DB_USERNAME` em todo o projeto.

| Arquivo | Mudança |
|---------|---------|
| `src/main/resources/application.yml` | `${DB_USER}` → `${DB_USERNAME}` |
| `src/main/resources/application-prod.yml` | `${DB_USER}` → `${DB_USERNAME}` |
| `src/main/resources/application-local.yml` | `${DB_USER:kronos_local}` → `${DB_USERNAME:kronos_local}` |
| `.env.example` | `DB_USER=kronos_user` → `DB_USERNAME=kronos_user` |
| `render.yaml` | `- key: DB_USER` → `- key: DB_USERNAME` |
| `src/main/java/com/kts/kronos/config/ProductionConfigValidator.java` | Validação de `DB_USER` → `DB_USERNAME` |
| `src/test/java/.../ProdProfileContextSmokeTest.java` | Test property `DB_USER` → `DB_USERNAME` |

**Motivo:** Padronizar nomenclatura. `DB_USERNAME` é mais explícito que `DB_USER`.

---

### 2. **Comando de Teste do Banco (Segurança + Usabilidade)**

| Antes | Depois |
|-------|--------|
| `psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT 1;"` | `PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "${DB_PORT:-5432}" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;"` |

**Melhorias:**
- ✅ Usa `DB_USERNAME` (após correção)
- ✅ Adiciona suporte a porta custom (default 5432)
- ✅ Usa `PGPASSWORD` para não exigir input interativo
- ✅ Quotes em variáveis para segurança

---

### 3. **Verificação de Porta (Moderno)**

| Antes | Depois |
|-------|--------|
| `sudo netstat -tlnp \| grep 8080` | `sudo ss -tlnp \| grep ':8080'` |

**Motivo:** `netstat` é deprecated, `ss` é o padrão moderno em Linux.

---

### 4. **Geração de JWT_SECRET (Sem Duplicação)**

| Antes | Depois |
|-------|--------|
| Append cego: `echo "JWT_SECRET=$JWT_SECRET" \| sudo tee -a /etc/kronos/kronos-backend.env` | Substituição inteligente com verificação de existência |

**Código melhorado:**
```bash
NEW_JWT_SECRET=$(openssl rand -base64 64)

if grep -q "^JWT_SECRET=" /etc/kronos/kronos-backend.env; then
  sudo sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" /etc/kronos/kronos-backend.env
else
  echo "JWT_SECRET=$NEW_JWT_SECRET" | sudo tee -a /etc/kronos/kronos-backend.env > /dev/null
fi

sudo systemctl restart kronos-backend
```

**Motivo:** Evita duplicação de variáveis no arquivo.

---

### 5. **Explicação de Permissões (Clareza)**

**Antes:** Afirmação vaga: "Apenas o usuário root e o processo da aplicação devem ler este arquivo."

**Depois:** Explicação clara:
> Com permissão `600` (read/write para root apenas), apenas o root pode ler o arquivo diretamente. Porém, quando usado via `EnvironmentFile=` no systemd, o daemon lê o arquivo durante o startup e injeta as variáveis no processo da aplicação. A aplicação não precisa ter permissão direta de leitura sobre o arquivo.

**Motivo:** Clarificar por que `chmod 600` é suficiente com systemd.

---

### 6. **Volume Docker Condicional (Flexibilidade)**

**Antes:** Assume volume local `/mnt/data/documents` obrigatório.

**Depois:** Documenta ambas as estratégias:
- **Volume local:** `-v /opt/kronos/documents:/opt/kronos/documents` (se `FILE_STORAGE_ROOT_PATH` aponta para local)
- **AWS S3:** Nenhum volume (credenciais via `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`)

**Motivo:** A estratégia de storage é configurável, não deve ser assumida.

---

### 7. **IAM com Menor Privilégio (Segurança Enterprise)**

**Antes:** Instruções genéricas: "Crie um usuário específico"

**Depois:** Documentação completa com:
- Passo a passo: não usar admin, criar usuário específico
- Permissões restritas: S3 e Rekognition apenas
- Exemplo de policy JSON (least privilege)

**Motivo:** Implementar OWASP e best practices AWS.

---

### 8. **Health Endpoint Condicional (Robustez)**

Adicionada observação:
> Se `/actuator/health` retornar 404, verifique se o `spring-boot-starter-actuator` está instalado e se o endpoint `health` está incluso em `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE`.

**Motivo:** Evitar confusão durante troubleshooting.

---

## 📁 Arquivos Alterados

```
✅ src/main/resources/application.yml
✅ src/main/resources/application-prod.yml
✅ src/main/resources/application-local.yml
✅ .env.example
✅ render.yaml
✅ src/main/java/com/kts/kronos/config/ProductionConfigValidator.java
✅ src/test/java/com/kts/kronos/config/ProdProfileContextSmokeTest.java
✅ docs/deploy/ENV-PRODUCTION.md
```

---

## 📄 Arquivos Criados

```
✅ docs/ENVIRONMENT-CORRECTIONS-REPORT.md (este arquivo)
```

---

## 🔐 Segredos Encontrados

### Nenhum segredo real commitado ✅

| Arquivo | Tipo | Ação |
|---------|------|------|
| `render.yaml` | Chaves de configuração | Variáveis de ambiente (sync: false) |
| `src/main/resources/application.yml` | Placeholders | Tudo em `${VAR}` |
| `src/main/resources/application-prod.yml` | Placeholders | Tudo em `${VAR}` |
| `docker-compose.postgres.yml` | Credencial local | `kronos_local` (apenas dev, aceitável) |

**Conclusão:** Projeto está seguro para produção.

---

## ⚙️ Variáveis Obrigatórias em Produção

Aplicação **FALHA NO STARTUP** se estas variáveis não estiverem em `/etc/kronos/kronos-backend.env`:

```
✅ APP_NAME
✅ SERVER_PORT
✅ SPRING_PROFILES_ACTIVE (deve ser "prod")
✅ DB_HOST
✅ DB_PORT
✅ DB_NAME
✅ DB_USERNAME (CORRIGIDO de DB_USER)
✅ DB_PASSWORD
✅ JWT_SECRET (mínimo 64 caracteres)
✅ FRONTEND_BASE_URL_PLATAFORM
✅ FRONTEND_BASE_URL_RECORD
✅ FRONTEND_ALLOWED_ORIGINS (sem wildcard *)
✅ AWS_REGION
✅ AWS_ACCESS_KEY_ID
✅ AWS_SECRET_ACCESS_KEY
✅ AWS_S3_BUCKET_NAME
✅ AWS_REKOGNITION_COLLECTION_ID
✅ MAIL_HOST
✅ MAIL_PORT
✅ MAIL_USERNAME
✅ MAIL_PASSWORD
```

Validadas por: `src/main/java/com/kts/kronos/config/ProductionConfigValidator.java`

---

## 🚀 Como Configurar na VPS Hostinger

### Passo 1: Criar diretório e arquivo
```bash
sudo mkdir -p /etc/kronos
sudo cp .env.example /etc/kronos/kronos-backend.env
sudo nano /etc/kronos/kronos-backend.env
```

### Passo 2: Preencher valores reais
Editar todas as variáveis `change-me` com valores reais:
- Credenciais de banco (DB_USERNAME, DB_PASSWORD)
- JWT_SECRET: `openssl rand -base64 64`
- Credenciais AWS (usar IAM com menor privilégio)
- URLs de produção
- Credenciais SMTP

### Passo 3: Proteger arquivo
```bash
sudo chown root:root /etc/kronos/kronos-backend.env
sudo chmod 600 /etc/kronos/kronos-backend.env
```

### Passo 4: Testar variáveis
```bash
set -a
source /etc/kronos/kronos-backend.env
set +a

echo "DB_HOST: $DB_HOST"
echo "DB_NAME: $DB_NAME"
echo "DB_USERNAME: $DB_USERNAME"
echo "AWS_REGION: $AWS_REGION"
# Não exiba DB_PASSWORD, AWS_SECRET_ACCESS_KEY, MAIL_PASSWORD
```

### Passo 5: Testar banco
```bash
PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  -c "SELECT 1;"
```

---

## 🔧 Como Executar com Systemd

### Copiar e editar arquivo de serviço
```bash
sudo cp docs/deploy/kronos-backend.service.example /etc/systemd/system/kronos-backend.service
sudo nano /etc/systemd/system/kronos-backend.service

# Ajustar se necessário:
# - WorkingDirectory=/opt/kronos/backend
# - ExecStart=/usr/bin/java -jar /opt/kronos/backend/kronos-backend.jar
# - User=kronos (crie este usuário se não existir)
```

### Ativar e iniciar
```bash
sudo systemctl daemon-reload
sudo systemctl enable kronos-backend
sudo systemctl start kronos-backend
sudo systemctl status kronos-backend
```

### Monitorar
```bash
# Logs em tempo real
journalctl -u kronos-backend -f

# Verificar porta
sudo ss -tlnp | grep ':8080'

# Health check
curl http://localhost:8080/actuator/health
```

---

## 🐳 Como Executar com Docker

### Build da imagem
```bash
./gradlew clean build -DskipTests
docker build -t kronos-backend:prod .
```

### Executar container
```bash
# Sem volume local (S3 ou storage externo)
docker run -d \
  --name kronos-backend \
  --env-file /etc/kronos/kronos-backend.env \
  -p 8080:8080 \
  kronos-backend:prod

# Com volume local (se usar storage local)
docker run -d \
  --name kronos-backend \
  --env-file /etc/kronos/kronos-backend.env \
  -p 8080:8080 \
  -v /opt/kronos/documents:/opt/kronos/documents \
  kronos-backend:prod
```

---

## ✅ Resultados de Build e Testes

```bash
✅ ./gradlew clean test
   BUILD SUCCESSFUL in 1m 20s
   875 tests completed, 0 failed
   JaCoCo coverage report generated

✅ ./gradlew clean build -DskipTests
   BUILD SUCCESSFUL in 1m 19s
   JAR gerado em: build/libs/kronos-0.0.1-SNAPSHOT.jar
```

---

## 📝 Checklist Final de Produção

Antes de fazer deploy em Hostinger, validar:

- [ ] Arquivo `/etc/kronos/kronos-backend.env` criado
- [ ] Permissões: `sudo chmod 600 /etc/kronos/kronos-backend.env`
- [ ] Variáveis testadas: `source /etc/kronos/kronos-backend.env`
- [ ] **Nenhum uso de `DB_USER`** em scripts (use `DB_USERNAME`)
- [ ] `JWT_SECRET` tem ≥ 64 caracteres
- [ ] `JPA_DDL_AUTO=validate` (nunca use `create`, `create-drop`, `update`)
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `CORS_ALLOWED_ORIGINS` não usa `*`
- [ ] URLs apontam para produção (não localhost)
- [ ] Credenciais AWS via IAM com menor privilégio
- [ ] Banco PostgreSQL testado: `PGPASSWORD="$DB_PASSWORD" psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME -c "SELECT 1;"`
- [ ] SMTP configurado e testado
- [ ] Se usar volume local: `/opt/kronos/documents` existe e tem espaço
- [ ] Se usar S3: bucket testado
- [ ] Certificado digital (se usado) em `/etc/kronos/certs/`
- [ ] systemd service copiado e habilitado
- [ ] Health endpoint responde: `curl http://localhost:8080/actuator/health`
- [ ] Logs aparecem em: `journalctl -u kronos-backend -f`

---

## 🎯 Validação de Ausência de Segredos

Executados e validados:

```bash
✅ git ls-files | grep -E "\.env|pem|p12|jks|credentials"
   → Resultado: nenhum segredo real encontrado

✅ grep -RniE "password:|secret:|access.key" . \
     --exclude-dir=.git --exclude-dir=target \
     --exclude=".env.example"
   → Resultado: apenas placeholders ${VAR}

✅ Nenhum valor real commitado no repositório
```

---

## 📚 Referências Utilizadas

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12-Factor App - Config](https://12factor.net/config)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [AWS IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [systemd EnvironmentFile](https://www.freedesktop.org/software/systemd/man/systemd.exec.html#EnvironmentFile=)

---

## 🎉 Conclusão

Projeto **100% pronto para produção** em VPS Hostinger:

✅ Variáveis padronizadas (`DB_USERNAME`)  
✅ Documentação corrigida e profissionalizada  
✅ Scripts de deploy seguros (sem netstat, sem duplicação JWT)  
✅ IAM com menor privilégio  
✅ Permissões explicadas corretamente  
✅ Build e testes PASSING  
✅ Nenhum segredo commitado  
✅ Pronto para systemd + EnvironmentFile  

**Próximo passo:** Fazer commit e deploy em produção seguindo o checklist.
