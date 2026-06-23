# Contrato funcional — CTO Demo Sandbox

## Nome da funcionalidade

Fluxo CTO de criação/deleção de empresa demo sandbox.

## Atores

| Ator | Permissão |
|---|---|
| CTO | Pode criar, deletar, consultar status e validar demo |
| MANAGER `kronos_teste` | Usa apenas empresa sandbox |
| MANAGER real | Não pode criar/deletar demo |
| PARTNER | Não pode criar/deletar demo |

## Dados fixos

```text
Company name: Kronos Teste
Sandbox key: KRONOS_TESTE
Username: kronos_teste
Password inicial: kronos_teste#
Role: MANAGER
```

## Dados sintéticos

- empresa;
- usuário;
- colaborador;
- ponto do mês vigente;
- documentos por tipo;
- termo biométrico sintético;
- férias pendente;
- abono pendente;
- ajuste de registro pendente.

## Restrições LGPD

- sem captura facial real;
- sem Rekognition real;
- sem bucket AWS de face;
- sem imagem real;
- sem armazenamento de dado real de cliente;
- logs sanitizados.

## API proposta

### Criar demo

```http
POST /api/cto/demo/create
```

Autorização:

```text
CTO
```

Resposta:

```json
{
  "operationId": "uuid",
  "status": "SUCCESS",
  "companyName": "Kronos Teste",
  "username": "kronos_teste",
  "initialPasswordAvailable": true,
  "created": {
    "companies": 1,
    "users": 1,
    "employees": 1,
    "pointRecords": 80,
    "documents": 10,
    "requests": 3
  },
  "validation": {
    "clean": true,
    "issues": []
  }
}
```

### Deletar demo

```http
DELETE /api/cto/demo
```

Resposta:

```json
{
  "operationId": "uuid",
  "status": "SUCCESS",
  "removed": {
    "companies": 1,
    "users": 1,
    "employees": 1,
    "pointRecords": 80,
    "documents": 10,
    "requests": 3,
    "files": 10,
    "sessions": 2,
    "cacheKeys": 12
  },
  "validation": {
    "clean": true,
    "issues": []
  }
}
```

### Status

```http
GET /api/cto/demo/status
```

Resposta:

```json
{
  "enabled": true,
  "killSwitch": false,
  "exists": true,
  "companyName": "Kronos Teste",
  "username": "kronos_teste",
  "sandboxKey": "KRONOS_TESTE",
  "lastOperation": {
    "operation": "CREATE",
    "status": "SUCCESS",
    "finishedAt": "2026-06-23T12:00:00Z"
  },
  "validation": {
    "clean": true,
    "issues": []
  }
}
```

### Validar

```http
POST /api/cto/demo/validate
```

Resposta:

```json
{
  "clean": true,
  "issues": []
}
```

## Erros esperados

| Código | Situação |
|---|---|
| 401/403 | Não autenticado ou sem role CTO |
| 409 | Job em execução |
| 423 | Sandbox bloqueada para purge |
| 503 | Demo desabilitada ou kill switch ativo |
| 500 | Falha técnica sanitizada |

## Variáveis

```text
KRONOS_DEMO_ENABLED=false
KRONOS_DEMO_KILL_SWITCH=false
KRONOS_DEMO_SANDBOX_KEY=KRONOS_TESTE
KRONOS_DEMO_COMPANY_NAME=Kronos Teste
KRONOS_DEMO_USERNAME=kronos_teste
KRONOS_DEMO_INITIAL_PASSWORD=kronos_teste#
KRONOS_DEMO_LOCAL_STORAGE_ROOT=/opt/kronos/sandbox/kronos-teste
KRONOS_DEMO_LOCK_TIMEOUT=PT5M
```

## Critério de aceite

- criação depende apenas de botão no front;
- deleção depende apenas de botão no front;
- endpoints protegidos por CTO;
- usuário sandbox limitado a MANAGER;
- storage sandbox local;
- empresas reais continuam no storage normal;
- purge idempotente;
- validação pós-purge;
- auditoria técnica mínima;
- kill switch;
- lock;
- sem dados reais/sensíveis.
