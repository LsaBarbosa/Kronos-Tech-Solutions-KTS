# Skill: Backend Debug (Produção Kronos)

## Logs

```bash
# Log corrente (JSON estruturado, rolante)
tail -f /var/log/kronos/backend.log

# Apenas erros
grep '"level":"ERROR"' /var/log/kronos/backend.log | tail -20

# Por correlation_id (rastrear request específico)
grep '"correlation_id":"<UUID>"' /var/log/kronos/backend.log

# Por trace_id (para correlacionar com Grafana/Prometheus)
grep '"trace_id":"<ID>"' /var/log/kronos/backend.log

# Log de erro separado (shutdown/JVM erros)
cat /var/log/kronos/backend-error.log
```

## Horário dos logs

Os logs usam timezone `-03:00` (BRT). O incidente das 14:34 UTC é às `11:34` nos logs (UTC-3).

## Verificar serviços de suporte

```bash
# ClamAV (antivírus — porta 3310)
systemctl status clamav-daemon
ss -tlnp | grep 3310

# Redis
systemctl status kronos-redis-local 2>/dev/null || docker ps | grep redis
redis-cli ping

# PostgreSQL
systemctl status postgresql
psql -U kronos_prod -d kronos_prod -c "SELECT 1" 2>/dev/null

# Backend systemd
systemctl status kronos-backend.service
journalctl -u kronos-backend.service --since "1 hour ago" | tail -50
```

## Variáveis de ambiente (NUNCA expor valores)

```bash
# Ver apenas chaves presentes
grep -E "^[A-Z]" /etc/kronos/kronos.env | sed 's/=.*/=<REDACTED>'

# Verificar se propriedade específica está presente
grep -i "antivirus\|SCAN" /etc/kronos/kronos.env | sed 's/=.*/=<REDACTED>'
```

## Deploy seguro

```bash
# 1. Backup obrigatório antes de substituir JAR
sudo cp /opt/kronos/app/kronos-backend.jar /opt/kronos/app/kronos-backend.jar.bak.$(date +%Y%m%d_%H%M%S)

# 2. Parar, substituir, iniciar
sudo systemctl stop kronos-backend
sudo rsync /home/deploy/apps/Kronos-Tech-Solutions-KTS/build/libs/kronos-backend.jar /opt/kronos/app/kronos-backend.jar
sudo systemctl start kronos-backend

# 3. Verificar saúde
sleep 15 && curl -s http://127.0.0.1:8080/actuator/health | grep -E "UP|DOWN"
```

## Padrões de eventos nos logs

| event | Significado |
|---|---|
| `document_upload_scan result=failure reason=io` | ClamAV inacessível |
| `document_upload result=failure reason=unknown` | RuntimeException no scan ou S3 |
| `http_error reason=unexpected` | Erro 500 capturado pelo RestExceptionHandler |
| `redis_cache_unavailable` | Redis desconectado (fallback memória ativo) |
| `terminal_checkin result=failure` | Checkin de terminal falhou (motivo no campo reason) |
