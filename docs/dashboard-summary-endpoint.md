# Dashboard Summary Endpoint

## Informações Gerais

- **Endpoint**: `GET /dashboard/summary`
- **Autenticação**: Requerida (JWT via Cookie HTTP-only)
- **Autorização**: `hasAnyRole('MANAGER', 'PARTNER', 'CTO')`
- **Content-Type**: `application/json`
- **Status HTTP**: `200 OK`, `401 Unauthorized`, `403 Forbidden`

## Descrição

Endpoint agregador que retorna métricas de dashboard personalizadas baseadas na role do usuário autenticado. Cada role recebe dados específicos de sua responsabilidade.

## Autenticação e Segurança

- Usa `JwtAuthenticatedUser` para obter informações do usuário autenticado
- Requer usuário autenticado com role válida (CTO, MANAGER, PARTNER)
- Dados são restringidos por empresa (tenant) para MANAGER e PARTNER
- CTO recebe visão global da plataforma
- Nenhum dado sensível (salário, CPF, e-mail completo) é exposto

## Contrato de Response

### Response por Role

#### Role: CTO

```json
{
  "role": "CTO",
  "generatedAt": "2026-06-04T23:30:00-03:00",
  "company": null,
  "cto": {
    "companies": {
      "total": 15,
      "active": 12,
      "inactive": 3
    },
    "lgpd": {
      "pendingRequests": 4,
      "overdueRequests": 0,
      "completedRequests": 20
    },
    "legal": {
      "documentsGeneratedThisMonth": 0,
      "afdGeneratedThisMonth": 0,
      "aejGeneratedThisMonth": 0,
      "mirrorGeneratedThisMonth": 0
    },
    "platform": {
      "activeUsers": 120,
      "activeEmployees": 95
    }
  },
  "manager": null,
  "partner": null,
  "fallbacks": [
    {
      "key": "legal.documentsGeneratedThisMonth",
      "reason": "Não existe tabela/evento persistido para contabilizar documentos legais gerados."
    }
  ]
}
```

#### Role: MANAGER

```json
{
  "role": "MANAGER",
  "generatedAt": "2026-06-04T23:30:00-03:00",
  "company": {
    "id": "uuid-empresa",
    "name": "Empresa Exemplo"
  },
  "cto": null,
  "manager": {
    "employees": {
      "total": 25,
      "active": 22,
      "inactive": 3
    },
    "pendingApprovals": {
      "total": 8,
      "timeRecords": 3,
      "vacations": 2,
      "timeOff": 3
    },
    "documents": {
      "recentTotal": 5,
      "pendingReview": 0
    },
    "warnings": {
      "total": 12,
      "recentTotal": 2,
      "unreadTotal": 2
    }
  },
  "partner": null,
  "fallbacks": []
}
```

#### Role: PARTNER

```json
{
  "role": "PARTNER",
  "generatedAt": "2026-06-04T23:30:00-03:00",
  "company": {
    "id": "uuid-empresa",
    "name": "Empresa Exemplo"
  },
  "cto": null,
  "manager": null,
  "partner": {
    "requests": {
      "pending": 2,
      "approvedThisMonth": 1,
      "rejectedThisMonth": 0
    },
    "documents": {
      "total": 8,
      "recentTotal": 1
    },
    "warnings": {
      "unreadTotal": 3,
      "recentTotal": 2
    },
    "privacy": {
      "biometricTermPending": false
    }
  },
  "fallbacks": []
}
```

## Métricas por Role

### CTO

#### companies
- **total**: Contagem de todas as empresas cadastradas
- **active**: Contagem de empresas com `active = true`
- **inactive**: Contagem de empresas com `active = false`
- **Fonte**: `CompanyProvider.findAll()`

#### lgpd
- **pendingRequests**: Contagem de solicitações LGPD com status OPEN, IN_ANALYSIS, WAITING_CONTROLLER, WAITING_LEGAL_REVIEW, WAITING_DATA_SUBJECT
- **overdueRequests**: Solicitações com vencimento passado (sempre 0 nesta versão, sem campo dueAt verificado)
- **completedRequests**: Contagem de solicitações com status COMPLETED
- **Fonte**: `LgpdRequestProvider.findAll()`

#### legal
- **documentsGeneratedThisMonth**: Sempre 0 (fallback - métrica indisponível)
- **afdGeneratedThisMonth**: Sempre 0 (fallback - métrica indisponível)
- **aejGeneratedThisMonth**: Sempre 0 (fallback - métrica indisponível)
- **mirrorGeneratedThisMonth**: Sempre 0 (fallback - métrica indisponível)
- **Razão**: Não existe tabela/evento persistido para contabilizar documentos legais gerados

#### platform
- **activeUsers**: Contagem de todos os colaboradores
- **activeEmployees**: Contagem de colaboradores com `active = true`
- **Fonte**: `EmployeeProvider.findAll()`

### MANAGER

#### employees
- **total**: Total de colaboradores da empresa
- **active**: Colaboradores da empresa com `active = true`
- **inactive**: Colaboradores da empresa com `active = false`
- **Fonte**: `EmployeeProvider.findByCompanyId()` + `countByCompanyIdAndActive()`

#### pendingApprovals
- **total**: Soma de timeRecords + vacations + timeOff
- **timeRecords**: Contagem de registros de ponto com status PENDING_APPROVAL de todos os colaboradores
- **vacations**: Contagem de solicitações de férias (REQUEST_VACATION) de todos os colaboradores
- **timeOff**: Contagem de abonos/folgas (TIME_OFF_REQUEST) de todos os colaboradores
- **Fonte**: `TimeRecordProvider.findByEmployeeIdsAndStatuses()` com filtro por statuses

#### documents
- **recentTotal**: Documentos enviados nos últimos 7 dias de todos os colaboradores (não deletados)
- **pendingReview**: Sempre 0 (métrica não implementada nesta versão)
- **Fonte**: `DocumentProvider.findAllByEmployeeId()` com filtro por `uploadeAt`

#### warnings
- **total**: Total de mensagens visíveis para o manager
- **recentTotal**: Mensagens dos últimos 7 dias
- **unreadTotal**: Igual a recentTotal nesta versão
- **Fonte**: `MessageProvider.findVisibleMessagesByCompanyIdAndEmployeeId()` com filtro por `createdAt`

### PARTNER

#### requests
- **pending**: Contagem de registros pessoais com status PENDING_APPROVAL, REQUEST_VACATION, TIME_OFF_REQUEST
- **approvedThisMonth**: Registros aprovados/concluídos no mês atual (CREATED, UPDATED, VACATION, TIME_OFF)
- **rejectedThisMonth**: Registros rejeitados no mês atual (UPDATE_REJECTED, VACATION_REJECTED, TIME_OFF_REJECTED, WORK_TIME_REJECTED)
- **Fonte**: `TimeRecordProvider.findByEmployeeId()` com filtro por status e datas

#### documents
- **total**: Total de documentos do usuário não deletados
- **recentTotal**: Documentos dos últimos 7 dias
- **Fonte**: `DocumentProvider.findAllByEmployeeId()` com filtro por `uploadeAt` e `deletedByEmployee`

#### warnings
- **unreadTotal**: Mensagens visíveis recentes (últimos 7 dias)
- **recentTotal**: Igual a unreadTotal nesta versão
- **Fonte**: `MessageProvider.findVisibleMessagesByCompanyIdAndEmployeeId()` com filtro por `createdAt`

#### privacy
- **biometricTermPending**: Sempre false nesta versão (termo biométrico sempre aceito para usuários autenticados)

## Fallbacks

Quando uma métrica não pode ser calculada com confiabilidade, é adicionada ao array `fallbacks` com:
- **key**: Caminho da métrica (ex: `legal.documentsGeneratedThisMonth`)
- **reason**: Motivo técnico da indisponibilidade

### Fallbacks Conhecidos (Versão Atual)

#### CTO
```json
{
  "key": "legal.documentsGeneratedThisMonth",
  "reason": "Não existe tabela/evento persistido para contabilizar documentos legais gerados."
}
```

## Regras de Isolamento de Dados

1. **CTO**: Acesso total aos dados agregados de todas as empresas
2. **MANAGER**: Acesso apenas aos dados da sua empresa e seus colaboradores
3. **PARTNER**: Acesso apenas aos seus próprios dados pessoais

## Performance e Índices

As queries são otimizadas para:
- Usar métodos `count*` quando disponíveis em vez de carregar listas completas
- Filtrar por status na camada de aplicação em casos onde índices diretos não existem
- Limitar busca de documentos/mensagens "recentes" aos últimos 7 dias

## Tabelas Consultadas

| Tabela | Campo | Operação | Role(s) |
|--------|-------|----------|---------|
| company | active | findAll, countByActive | CTO |
| employee | companyId, active | findByCompanyId, countByCompanyIdAndActive | CTO, MANAGER, PARTNER |
| lgpd_request | status | findAll | CTO |
| time_record | employeeId, statusRecord | findByEmployeeIdsAndStatuses | MANAGER, PARTNER |
| document | employeeId, uploadeAt, deletedBy* | findAllByEmployeeId | MANAGER, PARTNER |
| message | companyId, employeeId, createdAt | findVisibleMessagesByCompanyIdAndEmployeeId | MANAGER, PARTNER |

## Exemplos de Uso (cURL)

```bash
# Autenticado como CTO
curl -X GET http://localhost:8080/dashboard/summary \
  -H "Cookie: authToken=<jwt-token>" \
  -H "Content-Type: application/json"

# Autenticado como MANAGER
curl -X GET http://localhost:8080/dashboard/summary \
  -H "Cookie: authToken=<jwt-token>" \
  -H "Content-Type: application/json"

# Autenticado como PARTNER
curl -X GET http://localhost:8080/dashboard/summary \
  -H "Cookie: authToken=<jwt-token>" \
  -H "Content-Type: application/json"
```

## Códigos de Resposta

- **200 OK**: Métricas retornadas com sucesso
- **401 Unauthorized**: Usuário não autenticado
- **403 Forbidden**: Usuário autenticado mas sem role válida

## Implementação

- **Controller**: `DashboardController`
- **Service**: `DashboardService`
- **DTOs**: 
  - `DashboardSummaryResponse`
  - `DashboardCtoSummary`
  - `DashboardManagerSummary`
  - `DashboardPartnerSummary`
  - `DashboardCompanyInfo`
  - `DashboardFallbackItem`

## Próximas Melhorias

1. Implementar cálculo real de documentos legais gerados (integrar com `LegalService`)
2. Implementar cálculo de solicitações LGPD atrasadas usando campo `dueAt`
3. Implementar estado de "pendingReview" para documentos
4. Implementar verificação de termo biométrico pendente
5. Adicionar cache com TTL (ex: 5 minutos) para reduzir carga do banco
