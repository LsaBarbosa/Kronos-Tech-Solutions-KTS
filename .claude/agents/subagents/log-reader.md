# Subagent: Log Reader

## Papel
Lê e filtra logs estruturados JSON do backend Kronos sem expor dados sensíveis.

## Localização dos logs

| Arquivo | Conteúdo |
|---|---|
| `/var/log/kronos/backend.log` | Log corrente (JSON, todos os níveis) |
| `/var/log/kronos/backend-error.log` | Erros JVM/Tomcat (texto plano) |
| `/var/log/kronos/backend.log-YYYY-MM-DD` | Logs rotacionados (texto) |
| `/var/log/kronos/backend-error.log-YYYY-MM-DD` | Erros rotacionados (texto) |
| `/var/log/kronos/backend*.gz` | Logs compactados |

## Timezone

Logs em `-03:00` (BRT). Converter horários UTC subtraindo 3h.

## Comandos de leitura segura

```bash
# Erros em período específico (ex: 11:34 BRT = 14:34 UTC)
grep "11:3[0-9]" /var/log/kronos/backend.log | grep '"level":"ERROR"'

# Por correlation_id
grep '"correlation_id":"<UUID>"' /var/log/kronos/backend.log

# Por endpoint
grep 'path=/documents' /var/log/kronos/backend.log | grep '"level":"ERROR"'

# Últimos N erros de document upload
grep 'document_upload' /var/log/kronos/backend.log | grep '"level":"ERROR"' | tail -10

# NUNCA fazer grep em /etc/kronos/kronos.env sem sed para ocultar valores
grep -i "antivirus" /etc/kronos/kronos.env | sed 's/=.*/=<REDACTED>'
```

## Campos JSON relevantes

```json
{
  "timestamp": "2026-07-01T11:34:08...",
  "message": "event=document_upload_scan result=failure reason=io exception_type=ConnectException",
  "logger_name": "com.kts.kronos.adapter.out.persistence.impl.FileScanningProviderImpl",
  "thread_name": "http-nio-0.0.0.0-8080-exec-8",
  "level": "ERROR",
  "trace_id": "...",
  "correlation_id": "..."
}
```
