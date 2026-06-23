# Regras permanentes — Kronos Demo Sandbox CTO

## Escopo

Implementar o fluxo operacional de sandbox/demo acionado exclusivamente por usuário com role `CTO`:

- `CRIAR DEMO`
- `DELETAR DEMO`
- validação pós-purge
- recuperação idempotente de sandbox suja em execuções posteriores

## Regras absolutas

1. Não alterar o provider global de storage para `local`.
2. Não enviar arquivos da empresa sandbox para AWS.
3. Não acionar Rekognition real.
4. Não capturar, armazenar ou simular imagem facial real.
5. Não usar dados reais de cliente, funcionário, empresa ou biometria.
6. Não permitir que `kronos_teste` acesse empresa real.
7. Não permitir que empresa real use storage local da sandbox.
8. Não permitir alteração de username ou senha do usuário `kronos_teste`.
9. Não permitir criação de integrações externas pela empresa sandbox.
10. Não permitir alteração de configurações globais pela empresa sandbox.
11. Não quebrar fluxos atuais de empresa real, documentos, ponto, férias, abono, ajuste de registro, LGPD, autenticação, login facial ou administração.
12. Não colocar regra de negócio em controller.
13. Não remover ou reduzir validações existentes.
14. Não logar CPF, CNPJ, e-mail, senha, token, imagem, base64, path sensível completo ou payload sensível.
15. Não criar seed automático na inicialização da aplicação.
16. Não deixar o fluxo de demo disponível sem kill switch.
17. Não permitir execução concorrente de criação/purge da demo.

## Branches obrigatórias

- Back-end: `homolog`
- Front-end: `homolog`
- Documentação: `main`

Abortar a implementação se a branch local não corresponder.

## Entidade sandbox

A empresa demo deve ser identificada por metadado técnico imutável, não apenas pelo nome.

Preferência:

```text
company_type = SANDBOX
sandbox_key = KRONOS_TESTE
```

Alternativa aceitável se o modelo atual impedir enum:

```text
is_sandbox = true
sandbox_key = KRONOS_TESTE
```

O nome visível deve ser:

```text
Kronos Teste
```

## Usuário demo

```text
username: kronos_teste
password: kronos_teste#
role: MANAGER
```

A senha deve ser armazenada somente com o mecanismo de hash já usado pelo sistema. A senha em texto puro só pode aparecer em documentação operacional e resposta controlada de criação, nunca em log.

## Dados criados

O fluxo de criação deve criar, no mínimo:

1. `COMPANY` sandbox `Kronos Teste`.
2. `USER` `kronos_teste` com role `MANAGER`.
3. `EMPLOYEE` vinculado ao `USER` e à empresa sandbox.
4. Registros de ponto sintéticos do mês vigente.
5. Um documento sintético para cada tipo de documento suportado.
6. Um `BIOMETRIC_CONSENT_TERM` sintético, sem face real.
7. Uma solicitação pendente de férias.
8. Uma solicitação pendente de abono.
9. Uma solicitação pendente de ajuste de registro.
10. Auditoria técnica mínima da execução.

## Storage sandbox

A empresa sandbox deve usar storage local dedicado na VPS.

Caminho base recomendado:

```text
/opt/kronos/sandbox/kronos-teste
```

Caminho por empresa:

```text
/opt/kronos/sandbox/kronos-teste/company/{companyId}
```

Caminho por documento:

```text
/opt/kronos/sandbox/kronos-teste/company/{companyId}/documents/{documentType}/{documentId}.pdf
```

O roteamento de storage deve ser por empresa/tenant. Empresas reais continuam usando o storage normal do sistema.

## Deleção

O purge deve remover tudo relacionado à sandbox, incluindo dados criados inicialmente e dados criados posteriormente por usuários vinculados à empresa sandbox.

O purge deve:

1. adquirir lock;
2. checar kill switch;
3. bloquear novas operações da sandbox;
4. invalidar sessão/token/cache/rate-limit;
5. identificar resíduos;
6. remover dados dependentes em ordem segura;
7. remover arquivos locais da sandbox;
8. validar ausência de resíduos;
9. registrar auditoria técnica mínima;
10. liberar lock.

## Validação pós-purge obrigatória

Após deletar, validar:

- existe `USER` `kronos_teste` sem empresa?
- existe `COMPANY` sandbox sem employee?
- existe pasta sandbox antiga?
- existe documento órfão?
- existe registro de ponto órfão?
- existe solicitação trabalhista órfã?
- existe refresh token/sessão/cache para usuário/empresa sandbox?
- existe rate-limit/cache de permissão/cache de company/user?
- existe qualquer arquivo físico sob `/opt/kronos/sandbox/kronos-teste`?

Se houver resíduo, tentar limpar de forma idempotente e registrar falha/sucesso na auditoria.

## Auditoria

Manter log técnico do job, sem dados sensíveis.

Campos mínimos:

- `jobId`
- `operation`: `CREATE`, `PURGE`, `VALIDATE`
- `status`: `STARTED`, `SUCCESS`, `FAILED`, `PARTIAL`
- `startedAt`
- `finishedAt`
- `durationMs`
- `actorUserId`
- `actorRole`
- `sandboxKey`
- contadores por tipo de dado criado/removido
- erro técnico sanitizado
- versão da aplicação, se disponível

A auditoria do job pode permanecer após o purge. Ela não deve conter dados pessoais ou documentos.

## API

Endpoints sugeridos:

```http
POST   /api/cto/demo/create
DELETE /api/cto/demo
GET    /api/cto/demo/status
POST   /api/cto/demo/validate
```

Todos os endpoints devem exigir role `CTO`.

## Front-end

Adicionar botões em área acessível somente ao `CTO`:

- `CRIAR DEMO`
- `DELETAR DEMO`

O front deve:

- consumir endpoints reais;
- mostrar estado de loading;
- confirmar deleção com diálogo;
- exibir resultado do purge/validação;
- exibir banner claro quando a empresa ativa for sandbox;
- bloquear visualmente ações proibidas para a sandbox;
- nunca armazenar JWT em `localStorage` ou `sessionStorage`;
- nunca duplicar autorização sensível que pertence ao back-end.

## Testes obrigatórios

Back-end:

- unitários de service;
- integração de repository/purge quando possível;
- segurança dos endpoints `CTO`;
- idempotência de `create`;
- idempotência de `purge`;
- recovery de sandbox parcialmente suja;
- storage local apenas para sandbox;
- empresas reais continuam com storage normal.

Front-end:

- renderização dos botões somente para `CTO`;
- bloqueio para `MANAGER`/`PARTNER`;
- loading, erro e sucesso;
- confirmação antes do purge;
- banner de sandbox.

## Critério de aceite final

A tarefa só termina quando:

```bash
# backend
./gradlew clean test

# frontend
npm run lint
npm run test
npm run build
```

forem executados ou, se falharem por motivo pré-existente, a falha for isolada e documentada.
