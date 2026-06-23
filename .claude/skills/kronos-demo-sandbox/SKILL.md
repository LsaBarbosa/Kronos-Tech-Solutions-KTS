---
name: kronos-demo-sandbox
description: Implementa fluxo CTO para criar, deletar, validar e recuperar empresa sandbox demo Kronos Teste com dados sintéticos, storage local isolado, purge idempotente, lock, kill switch, auditoria e UI controlada no front-end.
---

# Skill — Kronos Demo Sandbox CTO

## Quando usar

Use esta skill quando a tarefa envolver:

- criação automática da empresa demo `Kronos Teste`;
- usuário demo `kronos_teste`;
- dados sintéticos para demonstração;
- botões `CRIAR DEMO` e `DELETAR DEMO` para `CTO`;
- purge completo e idempotente;
- isolamento entre empresa sandbox e empresas reais;
- storage local na VPS apenas para sandbox;
- bloqueio de integrações externas;
- validação pós-purge.

## Objetivo técnico

Criar uma funcionalidade produtiva e segura que permita ao `CTO` resetar uma empresa demo sem afetar clientes reais.

O fluxo deve permitir:

1. Remover qualquer resíduo de sandbox anterior.
2. Criar dados sintéticos consistentes.
3. Permitir uso da credencial demo por clientes/testadores.
4. Remover depois todos os dados gerados inicialmente e posteriormente.
5. Validar que nada ficou no banco, storage, cache ou sessão.
6. Rodar novamente com segurança se uma execução anterior falhar.

## Ordem de execução

### 1. Mapear antes de alterar

Ler e mapear:

- entidades `Company`, `User`, `Employee`;
- entidades de ponto/registro;
- entidades de férias;
- entidades de abono;
- entidades de ajuste de registro/manual registration;
- entidades e serviços de documentos;
- tipos de documento;
- consentimento biométrico;
- autenticação/JWT/cookies/refresh token;
- caches Redis/local;
- rate-limit;
- services de storage;
- AWS S3;
- Rekognition;
- guards/roles;
- migrations Flyway.

Não implementar nada sem esse mapa.

### 2. Backend primeiro

Implementar primeiro no back-end:

- configuração `kronos.demo.*`;
- metadado sandbox na empresa;
- lock de execução;
- service de seed;
- service de purge;
- roteamento de storage por empresa;
- endpoints CTO;
- validação pós-purge;
- testes.

### 3. Front-end depois

Implementar somente após contrato estável:

- service HTTP para endpoints demo;
- UI de administração CTO;
- botões e confirmação;
- feedback de execução;
- banner de sandbox;
- testes.

### 4. Documentação por último

Atualizar documentação:

- contratos API;
- fluxos;
- atores/permissões;
- segurança/LGPD;
- ambientes/variáveis;
- troubleshooting;
- ADR se houver decisão arquitetural nova.

## Design recomendado

### Configuração

Adicionar propriedades equivalentes a:

```yaml
kronos:
  demo:
    enabled: ${KRONOS_DEMO_ENABLED:false}
    kill-switch: ${KRONOS_DEMO_KILL_SWITCH:false}
    sandbox-key: ${KRONOS_DEMO_SANDBOX_KEY:KRONOS_TESTE}
    company-name: ${KRONOS_DEMO_COMPANY_NAME:Kronos Teste}
    username: ${KRONOS_DEMO_USERNAME:kronos_teste}
    initial-password: ${KRONOS_DEMO_INITIAL_PASSWORD:kronos_teste#}
    local-storage-root: ${KRONOS_DEMO_LOCAL_STORAGE_ROOT:/opt/kronos/sandbox/kronos-teste}
    lock-timeout: ${KRONOS_DEMO_LOCK_TIMEOUT:PT5M}
```

Nunca ativar por default.

### Domínio

Preferir novos componentes isolados:

```text
demo/
  config/
  controller/
  dto/
  service/
  repository/
  audit/
  storage/
  validation/
```

Não misturar regra de sandbox nos services principais além de pontos de integração mínimos.

### API

DTOs sugeridos:

```text
DemoStatusResponse
DemoCreateResponse
DemoPurgeResponse
DemoValidationResponse
DemoAuditSummary
DemoValidationIssue
DemoOperationCounters
```

### Idempotência

`create` deve:

1. adquirir lock;
2. validar kill switch;
3. executar `purge` interno se houver resíduo;
4. criar sandbox limpa;
5. validar;
6. retornar status.

`purge` deve:

1. adquirir lock;
2. validar kill switch;
3. buscar por `sandboxKey`, `companyName`, `username` e storage local;
4. limpar resíduos mesmo se parte das entidades não existir;
5. validar;
6. retornar status.

### Storage

Não alterar provider global.

Criar roteador/delegador:

```text
CompanyStorageRouter
SandboxLocalStorageService
RealCompanyStorageDelegate
```

Fluxo:

```text
if company.isSandbox():
    usar SandboxLocalStorageService
else:
    usar provider existente
```

### Integrações externas

Para empresa sandbox:

- e-mail real: bloqueado ou redirecionado para sink técnico;
- WhatsApp/webhooks: bloqueados;
- AWS S3: bloqueado;
- Rekognition: bloqueado;
- geocoding externo: bloquear se não for necessário ao fluxo;
- qualquer integração futura: negar por política `SandboxExternalIntegrationGuard`.

### Sessões e cache

Implementar/usar serviço central:

```text
DemoSessionInvalidationService
```

Ele deve invalidar, conforme existir no projeto:

- refresh tokens;
- tabela de sessões;
- cookies via resposta HTTP quando aplicável;
- cache Redis por usuário;
- cache Redis por company;
- cache de permissões;
- rate-limit por username;
- contexto em memória;
- token version/session version se existir.

Se JWT for totalmente stateless e não houver blacklist, registrar limitação técnica e implementar a melhor mitigação disponível no projeto.

## Checklist de conclusão

- [ ] Branches corretas.
- [ ] Mapa das entidades concluído.
- [ ] Propriedades configuradas.
- [ ] Migration criada.
- [ ] Seed idempotente implementado.
- [ ] Purge idempotente implementado.
- [ ] Lock implementado.
- [ ] Kill switch implementado.
- [ ] Auditoria implementada.
- [ ] Storage sandbox local isolado.
- [ ] AWS/Rekognition bloqueados para sandbox.
- [ ] Endpoints restritos a CTO.
- [ ] Front com botões restritos a CTO.
- [ ] Banner sandbox.
- [ ] Testes backend.
- [ ] Testes frontend.
- [ ] Documentação atualizada.
