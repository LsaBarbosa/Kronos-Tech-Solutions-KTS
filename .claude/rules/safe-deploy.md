# Rules: Deploy Seguro — Kronos (VPS Hostinger)

## Scripts disponíveis

| Script | Uso |
|---|---|
| `/home/deploy/kronos-full-deploy.sh` | Deploy completo (backend + frontend). Requer `sudo`. |
| `scripts/deploy.sh` | Deploy seletivo: `--backend`, `--frontend`, `--all`, `--skip-build` |

## Checklist de deploy seguro

### Pré-deploy

- [ ] `git status` — confirmar que está na branch correta (`feature/flag`)
- [ ] `git pull` — branch atualizada
- [ ] Backup do JAR atual obrigatório:
  ```bash
  sudo cp /opt/kronos/app/kronos-backend.jar \
    /opt/kronos/app/kronos-backend.jar.bak.$(date +%Y%m%d_%H%M%S)
  ls -lh /opt/kronos/releases/ 2>/dev/null || ls -lh /opt/kronos/app/*.bak.*
  ```
- [ ] Variáveis de ambiente validadas (sem alterar valores):
  ```bash
  grep -E "^AWS_|^DB_|^REDIS_|^SPRING_" /etc/kronos/kronos.env | sed 's/=.*/=[REDACTED]/'
  ```

### Build

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS

# Build com testes (preferido)
./gradlew bootJar

# Build sem testes (emergência — documentar motivo)
./gradlew bootJar -x test

# Verificar JAR gerado
ls -lh build/libs/*.jar
```

**Se o build falhar**: NÃO fazer deploy. Investigar e corrigir antes.

### Deploy via script oficial

```bash
# Backend apenas
sudo bash /home/deploy/apps/Kronos-Tech-Solutions-KTS/scripts/deploy.sh --backend

# Full (backend + frontend platform)
sudo bash /home/deploy/kronos-full-deploy.sh
```

O script `deploy.sh` faz automaticamente:
1. Backup do JAR atual
2. `systemctl stop kronos-backend`
3. `rsync` do novo JAR
4. `systemctl start kronos-backend`
5. Health check em `http://127.0.0.1:8081/actuator/health` com timeout de 120s

### Deploy manual (se script não disponível)

```bash
sudo systemctl stop kronos-backend
sudo rsync build/libs/kronos-backend.jar /opt/kronos/app/kronos-backend.jar
sudo systemctl start kronos-backend
```

### Validação pós-deploy

```bash
# 1. Aguardar Spring Boot subir (30-60s)
sleep 30

# 2. Health check
curl -s http://127.0.0.1:8080/actuator/health | grep -E '"status":"(UP|DOWN)"'

# 3. Sem erros de startup
grep "Started KronosApplication" /var/log/kronos/backend.log | tail -3
grep '"level":"ERROR"' /var/log/kronos/backend.log | tail -5

# 4. Smoke test S3 (após rotação de credenciais)
aws sts get-caller-identity 2>&1 | grep -v "Account\|UserId\|Arn"

# 5. Validar ClamAV
printf 'zPING\0' | nc -q1 127.0.0.1 3310 2>&1  # deve retornar PONG
```

### Rollback

```bash
# Listar backups
ls -lht /opt/kronos/app/*.bak.* 2>/dev/null || ls -lht /opt/kronos/releases/*.jar 2>/dev/null

# Restaurar backup
sudo systemctl stop kronos-backend
sudo cp /opt/kronos/app/kronos-backend.jar.bak.<TIMESTAMP> /opt/kronos/app/kronos-backend.jar
sudo systemctl start kronos-backend
sleep 30 && curl -s http://127.0.0.1:8080/actuator/health
```

## Regras invioláveis

1. **NUNCA** commitar sem autorização do usuário
2. **NUNCA** fazer `git push --force` em `feature/flag`
3. **SEMPRE** backup do JAR antes de substituir
4. **NUNCA** editar `/etc/kronos/kronos.env` sem backup imediato antes
5. **NUNCA** expor valores de variáveis de ambiente em logs ou output
6. **NUNCA** reiniciar sem o build ter passado
7. Reload Nginx sempre via `sudo nginx -t -q && sudo systemctl reload nginx`
