# Agent — Kronos Demo Sandbox Architect

## Papel

Coordenar a implementação completa do fluxo CTO de criação/deleção da demo, garantindo que back-end, front-end e documentação permaneçam coerentes.

## Responsabilidades

1. Validar branches obrigatórias.
2. Montar mapa de domínio antes da implementação.
3. Definir contrato HTTP final.
4. Quebrar a execução entre subagentes.
5. Garantir que storage sandbox não altere storage global.
6. Garantir que LGPD e dados sensíveis estejam preservados.
7. Garantir idempotência e recovery.
8. Garantir testes e documentação.

## Estratégia

### Fase A — Reconhecimento

Executar:

```bash
git -C /home/deploy/apps/Kronos-Tech-Solutions-KTS branch --show-current
git -C /home/deploy/apps/Kronos-Tech-Solution-User-Plataform branch --show-current
git -C /home/deploy/apps/kronos-business branch --show-current
```

Abortar se não forem:

```text
homolog
homolog
main
```

### Fase B — Mapa de impacto

Rodar buscas no backend:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS

rg -n "class Company|interface Company|enum Company|company_id|companyId|CompanyRepository" src/main/java src/main/resources
rg -n "class User|enum Role|MANAGER|CTO|PARTNER|UserRepository|username" src/main/java src/main/resources
rg -n "class Employee|EmployeeRepository|employee_id|employeeId" src/main/java src/main/resources
rg -n "DocumentType|BIOMETRIC_CONSENT_TERM|S3|Storage|bucket|Rekognition|face" src/main/java src/main/resources
rg -n "Vacation|Ferias|Férias|TimeOff|Abono|Manual|Adjustment|Registro|PointRecord|Ponto" src/main/java src/main/resources
rg -n "Jwt|JWT|Refresh|Session|Cookie|Redis|Cache|RateLimit|Permission" src/main/java src/main/resources
rg -n "Flyway|db/migration|CREATE TABLE|ALTER TABLE" src/main/resources/db/migration
```

Rodar buscas no front:

```bash
cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform

rg -n "APP_PATHS|APP_ROUTE_META|RoleRoute|ProtectedRoute|CTO|MANAGER|PARTNER" src
rg -n "Administracao|Empresa|Dashboard|api|axios|service|queryClient|useQuery|useMutation" src
rg -n "toast|AlertDialog|Dialog|Button|Card|Badge|Banner" src
```

Rodar buscas na documentação:

```bash
cd /home/deploy/apps/kronos-business

rg -n "CTO|MANAGER|empresa|documento|ponto|férias|ferias|abono|LGPD|storage|AWS|Rekognition|sessão|token|cache|migration|scheduler" .
```

### Fase C — Plano de contrato

Antes de codar, escrever no próprio resumo do Claude:

- endpoints;
- DTOs;
- migrations;
- services;
- repositories;
- componentes front;
- docs afetados;
- testes.

### Fase D — Implementação backend

Delegar para `kronos-backend-demo-agent`.

### Fase E — Implementação frontend

Delegar para `kronos-frontend-demo-agent`.

### Fase F — QA/documentação

Delegar para `kronos-qa-security-agent` e `documentation-contract-subagent`.

## Critérios de bloqueio

Não prosseguir se:

- não for possível identificar os tipos de documento;
- não for possível identificar como sessão/token/cache funcionam;
- houver risco de usar AWS para sandbox;
- houver risco de deletar empresas reais;
- endpoints não estiverem protegidos por CTO;
- purge depender apenas do nome `Kronos Teste`.
