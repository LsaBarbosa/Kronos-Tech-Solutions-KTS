# JIRA Backlog Técnico — Saúde e Performance SQL (Contexto Employee)

## EPIC 1 — Otimização de Queries Críticas no Domínio Employee

**Tipo:** Epic  
**Prioridade:** Highest  
**Objetivo:** reduzir latência, eliminar gargalos de banco, evitar degradação em rotinas periódicas e reduzir risco de indisponibilidade por consultas não indexadas ou padrão N+1.

**Contexto técnico resumido**
- O serviço usa PostgreSQL com Spring Data JPA.
- O domínio `Employee` aciona consultas em `tb_employee`, `tb_time_records`, `tb_user`, `tb_message`, `tb_document` e `tb_time_record_approval`.
- Há consultas de alto impacto em listagens por empresa, histórico de ponto e rotinas agendadas.

---

## Prioridade 1 — TASKS de impacto direto em estabilidade e throughput

### TASK 1 — Indexação composta para consultas por empresa/ativo em colaboradores
**Tipo:** Task  
**Prioridade:** P1

**Erro / melhoria a corrigir**
- Ausência de índice composto para filtros frequentes:
  - `findByCompanyId(companyId)`
  - `findByCompanyIdAndActive(companyId, active)`
  - `countByCompanyIdAndActive(companyId, active)`

**Problema | risco atual**
- Sem índice adequado, o banco pode fazer table scan em `tb_employee` para operações usadas por API e scheduler.
- Risco de aumento de latência em horários de pico.
- Risco de crescimento linear do tempo de resposta conforme base de colaboradores aumenta.

**Ação técnica proposta**
- Criar índices:
  - `idx_employee_company_id` em `(company_id)`
  - `idx_employee_company_active` em `(company_id, is_active)`

**Critério de aceite**
- `EXPLAIN` das três consultas indicando uso de índice.
- Queda de latência p95 nas listagens e contagens por empresa.

---

### TASK 2 — Otimizar `findLatestByEmployeeId` em `tb_time_records`
**Tipo:** Task  
**Prioridade:** P1

**Erro / melhoria a corrigir**
- Query com `ORDER BY start_work DESC LIMIT 1` sem garantia de índice apropriado.

**Problema | risco atual**
- Pode gerar sort caro e varredura de muitos registros por colaborador.
- Risco de lentidão progressiva em colaboradores com histórico grande.

**Ação técnica proposta**
- Criar índice composto: `idx_time_records_emp_start_desc (employee_id, start_work DESC)`.
- Validar se a consulta precisa de todas as colunas (`SELECT *`) ou se pode usar projeção.

**Critério de aceite**
- `EXPLAIN ANALYZE` mostrando Index Scan/Index Only Scan com baixo custo.
- Redução da latência do endpoint/fluxo que consulta último ponto.

---

### TASK 3 — Reduzir N+1 no scheduler diário de folga/ausência
**Tipo:** Task  
**Prioridade:** P1

**Erro / melhoria a corrigir**
- No `ensureDayOffRecords`, a validação `existsByEmployeeIdAndDate` é executada por colaborador (loop), gerando múltiplas consultas.

**Problema | risco atual**
- Padrão N+1 no job diário.
- Risco de janela de execução longa, lock contention e atraso em outras rotinas.
- Risco de não cumprir SLA do processamento de fechamento diário.

**Ação técnica proposta**
- Reescrever estratégia para busca em lote de registros do dia por empresa.
- Trocar lógica baseada em `COUNT` por semântica mais eficiente com `EXISTS` quando aplicável.
- Adicionar/garantir índice `(employee_id, start_work)`.

**Critério de aceite**
- Redução do número total de queries executadas no scheduler.
- Tempo do job diário reduzido com mesma consistência funcional.

---

### TASK 4 — Eliminar leitura massiva de pontos em memória para férias/solicitações
**Tipo:** Task  
**Prioridade:** P1

**Erro / melhoria a corrigir**
- Fluxo atual busca colaboradores da empresa e faz `findByEmployeeId` para cada um, consolidando em memória.

**Problema | risco atual**
- Alto consumo de memória da aplicação.
- Risco de timeout em empresas com grande volume histórico.
- Risco de GC pressure e degradação global da API.

**Ação técnica proposta**
- Criar query única paginada no banco por empresa + status + período.
- Evitar `flatMap` com múltiplas queries por empregado.
- Ajustar índices com foco em filtros: `(employee_id, status_record, start_work)` e/ou estratégia por join com empresa.

**Critério de aceite**
- Endpoint de férias com paginação real no banco.
- Redução de memória e tempo de resposta em cenários de carga.

---

## Prioridade 2 — TASKS de eficiência estrutural (impacto alto em escala)

### TASK 5 — Reescrever cálculo de NSR máximo por empresa
**Tipo:** Task  
**Prioridade:** P2

**Erro / melhoria a corrigir**
- Query usa `IN (subquery)` + `GREATEST(COALESCE(...))` em campo de ponto.

**Problema | risco atual**
- Plano de execução com custo elevado em bases grandes.
- Risco de CPU alta no banco ao calcular agregação com função por linha.

**Ação técnica proposta**
- Reescrever com `JOIN` explícito entre `tb_time_records` e `tb_employee`.
- Avaliar coluna derivada de NSR máximo por registro para reduzir função em tempo de consulta.

**Critério de aceite**
- Custo total da query reduzido em `EXPLAIN ANALYZE`.
- Tempo de resposta estável mesmo com aumento de histórico.

---

### TASK 6 — Indexar `tb_user.employee_id` para validações de vínculo
**Tipo:** Task  
**Prioridade:** P2

**Erro / melhoria a corrigir**
- Consulta `findByEmployeeId` sem evidência de índice dedicado.

**Problema | risco atual**
- Risco de scan em `tb_user` durante criação/validação de colaboradores.
- Aumento de latência no onboarding.

**Ação técnica proposta**
- Criar índice `idx_user_employee_id`.
- Se relação for 1:1, considerar constraint/índice único.

**Critério de aceite**
- Busca por `employee_id` com uso de índice.
- Fluxo de criação de colaborador sem regressão de performance.

---

### TASK 7 — Otimizar consulta de registro aberto (`end_work IS NULL`)
**Tipo:** Task  
**Prioridade:** P2

**Erro / melhoria a corrigir**
- Query por ponto aberto combina filtro `IS NULL` com ordenação por data.

**Problema | risco atual**
- Em alto volume, pode haver custo elevado para encontrar “registro aberto mais recente”.

**Ação técnica proposta**
- Criar índice parcial (Postgres):
  - `(employee_id, start_work DESC) WHERE end_work IS NULL`

**Critério de aceite**
- Plano de execução usando índice parcial.
- Redução de latência no check de ponto aberto.

---

## Prioridade 3 — TASKS de busca textual, paginação e qualidade operacional

### TASK 8 — Melhorar busca de mensagens visíveis por empregado
**Tipo:** Task  
**Prioridade:** P3

**Erro / melhoria a corrigir**
- Query com `OR` entre `employee_id` e `recipient_employee_id` + `ORDER BY created_at DESC`.

**Problema | risco atual**
- `OR` reduz eficiência de uso de índice.
- Risco de latência crescente conforme `tb_message` aumenta.

**Ação técnica proposta**
- Reescrever para `UNION ALL` (quando aplicável) em duas queries indexáveis.
- Adicionar índices:
  - `(company_id, employee_id, created_at DESC)`
  - `(company_id, recipient_employee_id, created_at DESC)`
- Aplicar paginação na leitura.

**Critério de aceite**
- Query paginada com plano previsível.
- Redução de tempo de listagem de mensagens.

---

### TASK 9 — Otimizar busca por nome em aprovações de ajuste de ponto
**Tipo:** Task  
**Prioridade:** P3

**Erro / melhoria a corrigir**
- Uso de `ILIKE %nome%`/`LOWER(name) LIKE %...%` com wildcard à esquerda.

**Problema | risco atual**
- Busca textual tende a full scan em `tb_employee` sem índice especializado.
- Risco de degradação acentuada com aumento da base.

**Ação técnica proposta**
- Habilitar `pg_trgm` e índice trigram em `full_name`.
- Padronizar uma única estratégia textual entre queries de aprovação.

**Critério de aceite**
- Busca por nome parcial com custo reduzido e resposta estável.

---

### TASK 10 — Indexar consultas de documentos por colaborador/tipo/data/visibilidade
**Tipo:** Task  
**Prioridade:** P3

**Erro / melhoria a corrigir**
- Filtros recorrentes em `tb_document` sem índice composto explícito:
  - `employee_id`, `document_type`, `uploaded_at`, flags de exclusão lógica.

**Problema | risco atual**
- Risco de varreduras frequentes no histórico de documentos.
- Latência inconsistente em listagens por período.

**Ação técnica proposta**
- Criar índice composto `(employee_id, document_type, uploaded_at)`.
- Avaliar índices parciais para `deleted_by_manager=false` e `deleted_by_employee=false`.
- Revisar consistência temporal (`Instant` x `LocalDateTime`) para evitar conversões custosas/erros sutis.

**Critério de aceite**
- Consultas por período usando índice.
- Tempo de listagem estável com crescimento de volume.

---

## Sequência de execução recomendada (roadmap)
1. TASK 1
2. TASK 2
3. TASK 3
4. TASK 4
5. TASK 5
6. TASK 6
7. TASK 7
8. TASK 8
9. TASK 9
10. TASK 10

---

## Métricas de sucesso do Epic
- Reduzir p95/p99 dos endpoints de Employee e TimeRecord.
- Reduzir tempo total dos schedulers diários/semanais.
- Reduzir número médio de queries por requisição em fluxos críticos.
- Garantir planos de execução estáveis com crescimento de dados.
