# Skill: Backend Debug — Produção Kronos (VPS Hostinger)

## Caminhos de log

| Arquivo | Conteúdo |
|---|---|
| `/var/log/kronos/backend.log` | Log corrente (JSON estruturado, todos os níveis) |
| `/var/log/kronos/backend-error.log` | Erros JVM/Tomcat em texto plano |
| `/var/log/kronos/backend.log-YYYY-MM-DD` | Logs rotacionados diariamente |
| `/var/log/kronos/backend-error.log-YYYY-MM-DD` | Erros rotacionados |
| `/var/log/kronos/backend*.gz` | Logs compactados (anteriores a 2 dias) |

**Timezone dos logs**: `-03:00` (BRT). Converter UTC: `UTC-3h = BRT`.  
Exemplos: `14:34 UTC = 11:34 BRT` | `16:35 UTC = 13:35 BRT`

## Comandos de leitura de log

```bash
# Erros no horário específico (ex: 11:34 BRT)
grep '"level":"ERROR"' /var/log/kronos/backend.log | grep "11:3[3-5]"

# Por correlation_id (rastrear request específico end-to-end)
grep '"correlation_id":"<UUID>"' /var/log/kronos/backend.log

# Por trace_id (correlacionar com Grafana)
grep '"trace_id":"<ID>"' /var/log/kronos/backend.log

# Por endpoint
grep 'path=/documents' /var/log/kronos/backend.log | grep '"level":"ERROR"'

# Stack traces (campo stack_trace no JSON)
grep '"stack_trace"' /var/log/kronos/backend.log | grep "11:3[3-5]" | python3 -c "
import sys, json
for line in sys.stdin:
    try:
        obj = json.loads(line)
        if 'stack_trace' in obj:
            print(obj.get('timestamp',''))
            print(obj.get('message',''))
            print(obj['stack_trace'][:2000])
            print('---')
    except: pass
"

# Erros de documentos com stack_trace
grep -E '"level":"ERROR".*document|document.*"level":"ERROR"' /var/log/kronos/backend.log | tail -20
```

## Correlacionar request frontend → backend

O request do frontend inclui um `X-Correlation-ID` gerado pelo interceptor Axios.
O backend ecoa esse ID em todos os logs do mesmo request via MDC.

```bash
# Pegar o correlation_id de um erro específico
grep "11:34:08" /var/log/kronos/backend.log | python3 -c "
import sys,json
for l in sys.stdin:
    try:
        o = json.loads(l)
        print(o.get('correlation_id'), o.get('message','')[:100])
    except: pass
"

# Ver todos os eventos do mesmo request
grep '"correlation_id":"<UUID_ENCONTRADO>"' /var/log/kronos/backend.log
```

## Actuator (health e info)

O backend expõe actuator na porta **8080** (não 8081) — confirmar no `application.yml`:

```bash
# Health check
curl -s http://127.0.0.1:8080/actuator/health | python3 -m json.tool

# Info (versão, build)
curl -s http://127.0.0.1:8080/actuator/info | python3 -m json.tool

# Métricas Prometheus
curl -s http://127.0.0.1:8080/actuator/prometheus | grep kronos | head -20

# Verificar se serviço responde
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/actuator/health
```

## Procedimento seguro de restart

```bash
# 1. BACKUP OBRIGATÓRIO do JAR atual
sudo cp /opt/kronos/app/kronos-backend.jar \
  /opt/kronos/app/kronos-backend.jar.bak.$(date +%Y%m%d_%H%M%S)
ls -lh /opt/kronos/app/

# 2. Build (do diretório do backend)
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
./gradlew bootJar -x test
ls -lh build/libs/*.jar

# 3. Stop → Deploy → Start
sudo systemctl stop kronos-backend
sudo rsync build/libs/kronos-backend.jar /opt/kronos/app/kronos-backend.jar
sudo systemctl start kronos-backend

# 4. Aguardar e validar
sleep 20
systemctl is-active kronos-backend
curl -s http://127.0.0.1:8080/actuator/health | grep -E "UP|DOWN"

# 5. Verificar logs de startup
grep "Started KronosApplication" /var/log/kronos/backend.log | tail -3
```

## Rollback

```bash
# Listar backups disponíveis
ls -lht /opt/kronos/app/*.bak.*

# Restaurar backup específico
sudo systemctl stop kronos-backend
sudo cp /opt/kronos/app/kronos-backend.jar.bak.<TIMESTAMP> /opt/kronos/app/kronos-backend.jar
sudo systemctl start kronos-backend
sleep 20 && curl -s http://127.0.0.1:8080/actuator/health | grep -E "UP|DOWN"
```

## Status dos serviços de suporte

```bash
systemctl is-active kronos-backend clamav-daemon && \
  docker ps | grep redis && \
  pg_isready -h 127.0.0.1 -p 5432 2>/dev/null | head -1
```
