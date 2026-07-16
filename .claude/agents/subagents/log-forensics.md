# Subagent: Log Forensics

## Propósito
Extrair stack traces, correlacionar eventos por timestamp/thread e identificar classe+linha Java do erro.

## Localização e formato dos logs

```
/var/log/kronos/backend.log          ← corrente, JSON estruturado (ndjson)
/var/log/kronos/backend.log-YYYY-MM-DD  ← rotacionado
/var/log/kronos/backend-error.log    ← erros JVM/Tomcat, texto plano
```

Campos JSON relevantes: `timestamp`, `level`, `message`, `logger_name`, `thread_name`,
`trace_id`, `correlation_id`, `stack_trace`.

**Timezone**: logs em `-03:00` (BRT). `14:34 UTC = 11:34 BRT` | `16:35 UTC = 13:35 BRT`

## Extrair stack trace por timestamp

```bash
# INCIDENTE 1: upload às 11:34 BRT (14:34 UTC)
grep "11:3[3-5]" /var/log/kronos/backend.log | \
  python3 -c "
import sys, json
for line in sys.stdin:
    try:
        o = json.loads(line)
        if o.get('level') in ('ERROR','WARN') and 'stack_trace' in o:
            print('=== TIMESTAMP:', o.get('timestamp'))
            print('MSG:', o.get('message'))
            print('THREAD:', o.get('thread_name'))
            print('CORRELATION:', o.get('correlation_id'))
            print('STACK:')
            print(o['stack_trace'][:3000])
            print()
    except: pass
"

# INCIDENTE 2: download às 13:35 BRT (16:35 UTC)
grep "13:3[4-6]" /var/log/kronos/backend.log | \
  python3 -c "
import sys, json
for line in sys.stdin:
    try:
        o = json.loads(line)
        if 'stack_trace' in o or o.get('level') == 'ERROR':
            print('TS:', o.get('timestamp'), '|', o.get('message','')[:200])
            if 'stack_trace' in o:
                print(o['stack_trace'][:2000])
    except: pass
" | head -80
```

## Correlacionar request completo por correlation_id

```bash
# 1. Encontrar o correlation_id do erro
CORR=$(grep "13:35:36" /var/log/kronos/backend.log | \
  python3 -c "import sys,json; [print(json.loads(l).get('correlation_id','')) for l in sys.stdin if json.loads(l).get('level')=='ERROR']" | head -1)

# 2. Ver todos os eventos do mesmo request
grep "\"correlation_id\":\"$CORR\"" /var/log/kronos/backend.log | \
  python3 -c "import sys,json; [print(json.loads(l).get('timestamp'),'|',json.loads(l).get('level'),'|',json.loads(l).get('message','')[:120]) for l in sys.stdin]"
```

## Extrair linha de código do erro

```bash
# No stack_trace, procurar padrões "(ArquivoJava.java:NNN)"
grep "13:35:36" /var/log/kronos/backend.log | python3 -c "
import sys, json, re
for line in sys.stdin:
    try:
        o = json.loads(line)
        if 'stack_trace' in o:
            for match in re.findall(r'at (com\.kts\.kronos\.[^\s]+)\(([^)]+)\)', o['stack_trace']):
                print(match)
    except: pass
" | head -20
```

## Stack trace real do incidente 2 (download 13:35 BRT)

```
S3Exception: User: arn:aws:iam::943598056698:user/kronos_render is not authorized to perform:
s3:GetObject on resource: "arn:aws:s3:::kronos-docs-payslip-prod/company/d2d06fe5-.../Paulo.pdf"
with an explicit deny in an identity-based policy: arn:aws:iam::aws:policy/AWSCompromisedKeyQuarantineV3
(Service: S3, Status Code: 403, Request ID: NM0S425JDCAGFGE9)
```

**Classe/linha**: `S3BucketStorageProviderImpl.downloadFile()` → `SdkException` catch → RuntimeException
→ `DocumentService.downloadDocument()` linha 138 → `BadRequestException(ERROR_GET_FILE)`
