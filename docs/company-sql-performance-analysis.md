# Análise aprofundada de performance SQL — Contexto `Company`

## 1) Escopo e contexto técnico

Esta análise cobre as queries SQL (derivadas e customizadas) que participam diretamente do contexto de **Company** no serviço, incluindo:
- operações da entidade `Company`;
- consultas de agregação dependentes de `companyId` usadas em `CompanyService`;
- queries company-scoped em fluxos correlatos (NSR, AFD, aprovações e mensagens).

> Banco alvo: PostgreSQL (`spring.datasource.url` com driver PostgreSQL).

---

## 2) Mapa das queries no contexto de Company

## Query C1 — `CompanyRepository.findByCnpj(String cnpj)`
**Local:** repositório de empresa.  
**SQL aproximado:**
```sql
SELECT *
FROM tb_company
WHERE company_cnpj = :cnpj
LIMIT 1;
```

**Onde impacta o serviço**
- Validação de unicidade no `createCompany`.
- Busca de empresa no `getCompany` e `updateCompany`.

**É performática?**
- **Depende do índice em `company_cnpj`**.
- No mapeamento JPA, `company_cnpj` **não está marcado como `unique=true`** e não há definição explícita de índice na entidade.

**Desvantagem/Risco atual**
- Sem índice: full scan em `tb_company` para operações críticas de CRUD.
- Em concorrência, validar antes de inserir sem constraint no banco pode permitir duplicidade lógica de CNPJ.

**Recomendação técnica**
- Criar índice único em `tb_company(company_cnpj)`.
- Tratar exceção de unicidade no create (defesa contra corrida).

---

## Query C2 — `CompanyRepository.findByActiveTrue()`
**SQL aproximado:**
```sql
SELECT * FROM tb_company WHERE is_active = true;
```

## Query C3 — `CompanyRepository.findByActiveFalse()`
**SQL aproximado:**
```sql
SELECT * FROM tb_company WHERE is_active = false;
```

**Onde impacta o serviço**
- `CompanyService.listCompanies(active)` quando há filtro por status.
- `DayOffScheduler` usa `companyProvider.findByActive(true)` para iterar empresas ativas.

**É performática?**
- **Regular** em bases pequenas.
- Em escala, filtro booleano puro pode ter seletividade baixa e custo crescente sem estratégia de índice adequada.

**Desvantagem/Risco atual**
- Pode varrer grande parte da tabela e retornar listas extensas em memória.
- Risco de alongar jobs agendados dependentes de empresas ativas.

**Recomendação técnica**
- Avaliar índice parcial para ativos (`WHERE is_active = true`) se esse filtro for dominante.
- Adotar paginação para listagens administrativas quando necessário.

---

## Query C4 — `CompanyRepository.findAll()` (herdada de `JpaRepository`)
**SQL aproximado:**
```sql
SELECT * FROM tb_company;
```

**Onde impacta o serviço**
- `CompanyService.listCompanies(null)`.

**É performática?**
- **Não** para crescimento de base (varredura e materialização total).

**Desvantagem/Risco atual**
- Em `listCompanies`, cada empresa dispara contagens adicionais de funcionários (N+1 lógico de agregação).
- Risco de latência e consumo de memória na aplicação.

**Recomendação técnica**
- Paginar `listCompanies`.
- Reescrever para trazer contagens agregadas por empresa em query única (JOIN + GROUP BY).

---

## Query C5 — `CompanyRepository.deleteByCnpj(String cnpj)`
**SQL aproximado:**
```sql
DELETE FROM tb_company WHERE company_cnpj = :cnpj;
```

**Onde impacta o serviço**
- `CompanyService.deleteByCnpj`.

**É performática?**
- **Boa com índice em `company_cnpj`**, ruim sem ele.

**Desvantagem/Risco atual**
- Sem índice, delete custa scan.
- Dependendo de constraints relacionais, pode falhar ou causar bloqueios maiores.

**Recomendação técnica**
- Índice/constraint única por CNPJ.
- Revisar política de integridade referencial (cascade/restrict) explicitamente.

---

## Query C6 — `EmployeeRepository.countByCompanyIdAndActive(companyId, active)`
**SQL aproximado:**
```sql
SELECT COUNT(*)
FROM tb_employee
WHERE company_id = :companyId
  AND is_active = :active;
```

**Onde impacta o serviço**
- `CompanyService.getCompany` (2 contagens: ativos/inativos).
- `CompanyService.listCompanies` (2 contagens por empresa dentro de stream).

**É performática?**
- **Boa com índice composto `(company_id, is_active)`**.
- **Fraca sem índice**, principalmente no padrão repetitivo de `listCompanies`.

**Desvantagem/Risco atual**
- Em `listCompanies`, gera múltiplas contagens por empresa (N+1 de agregação).
- Escala de forma linear com nº de empresas × 2 contagens.

**Recomendação técnica**
- Índice composto em `tb_employee(company_id, is_active)`.
- Consolidar contagem por empresa em consulta única agregada.

---

## Query C7 — `EmployeeRepository.findByCompanyId(companyId)`
**SQL aproximado:**
```sql
SELECT *
FROM tb_employee
WHERE company_id = :companyId;
```

**Onde impacta o serviço**
- `CompanyService.toggleActivate` para propagar status da empresa aos usuários.

**É performática?**
- **Regular** com índice simples em `company_id`; ruim sem ele.

**Desvantagem/Risco atual**
- Carrega todos os colaboradores na memória.
- Em seguida executa busca de usuário por colaborador (`findByEmployeeId`) em loop (N+1 de usuários).

**Recomendação técnica**
- Indexar `tb_employee(company_id)`.
- Reescrever toggle para update em lote (SQL set-based), evitando loops por registro.

---

## Query C8 — `UserRepository.findByEmployeeId(employeeId)`
**SQL aproximado:**
```sql
SELECT *
FROM tb_user
WHERE employee_id = :employeeId
LIMIT 1;
```

**Onde impacta o serviço**
- `CompanyService.toggleActivate`, dentro de loop de colaboradores.

**É performática?**
- **Boa se houver índice em `tb_user(employee_id)`**.

**Desvantagem/Risco atual**
- Sem índice: cada lookup vira scan.
- Com loop por funcionário, custo total pode explodir em empresas grandes.

**Recomendação técnica**
- Índice (ou único) em `employee_id`.
- Substituir N lookups por operação em lote via join/update.

---

## Query C9 — `TimeRecordRepository.findMaxNsrByCompanyId(companyId)`
**Query atual (JPQL):**
```jpql
SELECT MAX(GREATEST(COALESCE(tr.nsrCheckin, 0), COALESCE(tr.nsrCheckout, 0)))
FROM TimeRecordEntity tr
WHERE tr.employeeId IN (
  SELECT e.employeeId FROM EmployeeEntity e WHERE e.companyId = :companyId
)
```

**Onde impacta o serviço**
- Fluxos de geração legal/AFD que dependem de sequência NSR por empresa.

**É performática?**
- **Regular a fraca** em volume alto.

**Desvantagem/Risco atual**
- Subquery com `IN` + função por linha (`GREATEST/COALESCE`) aumenta custo.
- Pode elevar CPU no banco quando histórico de ponto cresce.

**Recomendação técnica**
- Reescrever com `JOIN` explícito.
- Avaliar estratégia de persistir NSR “máximo efetivo” por registro para reduzir função em runtime.

---

## Query C10 — `CompanyNsrRepository.incrementAndGetNsr(companyId)`
**Query atual (native):** upsert atômico com `RETURNING`.

**Onde impacta o serviço**
- Geração de NSR sequencial por empresa.

**É performática?**
- **Sim, geralmente performática e correta para concorrência**.

**Desvantagem/Risco atual**
- Hot row por empresa sob altíssima concorrência (contention localizado).
- Em cargas extremas, pode virar ponto de serialização.

**Recomendação técnica**
- Manter abordagem atômica.
- Monitorar lock wait e throughput por `company_id`.

---

## Query C11 — `AfdEntryRepository.findLastHashByCompanyId(companyId)`
**Query atual (JPQL):**
```jpql
SELECT a.currentHash
FROM AfdEntryEntity a
WHERE a.companyId = :companyId
ORDER BY a.nsr DESC
LIMIT 1
```

**Onde impacta o serviço**
- Encadeamento hash da geração de AFD.

**É performática?**
- **Boa se houver índice `(company_id, nsr DESC)`**.
- Sem índice, custo de ordenação cresce rapidamente.

**Desvantagem/Risco atual**
- Pode degradar em empresas com alto volume de AFD entries.

**Recomendação técnica**
- Índice composto por empresa + NSR ordenável.

---

## Query C12 — `AfdEntryRepository.streamAllByCompanyIdOrderByNsrAsc(companyId)`
**Query atual (JPQL) + hint de fetch size:** stream ordenado por NSR.

**Onde impacta o serviço**
- Exportação/leitura completa de entradas AFD por empresa.

**É performática?**
- **Boa para memória** (streaming + fetch size) quando bem indexada.

**Desvantagem/Risco atual**
- Sem índice por `(company_id, nsr)`, banco faz sort caro.
- Streaming prolonga transação/conexão; risco de pressionar pool em execuções longas.

**Recomendação técnica**
- Garantir índice composto `(company_id, nsr)`.
- Isolar execução em janela e observar tempo máximo por export.

---

## Query C13 — `TimeRecordApprovalRepository.findAllByCompanyId(...)`
**Query atual (JPQL):** filtra aprovações por subquery de funcionários da empresa e busca opcional por nome.

**Onde impacta o serviço**
- Listagem paginada de solicitações de ajuste de ponto no escopo da empresa.

**É performática?**
- **Regular**, podendo ficar **fraca** com filtro textual `%nome%`.

**Desvantagem/Risco atual**
- `LOWER(name) LIKE '%...%'` (ou equivalente) tende a full scan sem índice textual.
- Subquery pode escalar pior que join em alguns cenários.

**Recomendação técnica**
- Avaliar `JOIN` direto.
- Para busca parcial por nome, usar `pg_trgm` + índice trigram.

---

## Query C14 — `MessageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(...)`
**Query atual (JPQL):**
```jpql
SELECT m FROM MessageEntity m
WHERE m.companyId = :companyId
AND (m.employeeId = :employeeId OR m.recipientEmployeeId = :employeeId)
ORDER BY m.createdAt DESC
```

**Onde impacta o serviço**
- Inbox/visão de mensagens por colaborador dentro da empresa.

**É performática?**
- **Regular a fraca** conforme volume cresce.

**Desvantagem/Risco atual**
- `OR` prejudica seletividade e pode dificultar uso ótimo de índices.
- `ORDER BY` sem índice alinhado gera custo extra.

**Recomendação técnica**
- Considerar reescrita com `UNION ALL` (duas trilhas indexáveis).
- Índices: `(company_id, employee_id, created_at DESC)` e `(company_id, recipient_employee_id, created_at DESC)`.

---

## 3) Diagnóstico consolidado do `CompanyService`

### 3.1 Risco de N+1 em `listCompanies`
Fluxo atual:
1. busca empresas (`findAll` ou `findByActive`).
2. para cada empresa, executa **duas contagens** em `tb_employee`.

**Impacto:** latência e carga no banco crescem com número de empresas.

### 3.2 Risco de N+1 em `toggleActivate`
Fluxo atual:
1. busca todos funcionários da empresa.
2. para cada funcionário, busca usuário por `employeeId`.
3. aplica toggle via camada de caso de uso.

**Impacto:** elevado número de round-trips ao banco e custo de transação em empresas grandes.

---

## 4) Priorização de correção (Company)

### P1 (imediato)
1. Índice único em `tb_company(company_cnpj)`.
2. Índices em `tb_employee(company_id)` e `(company_id, is_active)`.
3. Refatorar `listCompanies` para agregação única por empresa (evitar N+1 de contagens).
4. Refatorar `toggleActivate` para operação em lote (evitar N+1 de usuários).

### P2 (curto prazo)
5. Otimizar `findMaxNsrByCompanyId` (join e redução de funções por linha).
6. Garantir índices de AFD/NSR por empresa (`company_id, nsr`).
7. Índice em `tb_user(employee_id)` (idealmente único se regra de negócio permitir).

### P3 (evolutivo)
8. Otimização de busca textual em aprovações (trigram).
9. Otimização de mensagens com OR (estratégia com UNION + paginação).

---

## 5) Conclusão técnica

No contexto de `Company`, as queries isoladas são simples e corretas, porém a **composição no serviço** cria gargalos de escala (principalmente N+1 e ausência potencial de índices críticos). O maior risco atual não é uma única query “ruim”, mas o **padrão de execução repetitiva por empresa/funcionário** em operações administrativas e agendadas.

Com os ajustes propostos, o serviço tende a ganhar:
- menor latência p95/p99;
- menor tempo de jobs que percorrem empresas ativas;
- menor pressão de CPU no banco e memória na aplicação.
