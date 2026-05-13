# Kronos Front-end — Backlog Operacional para Codex CLI

**Projeto front-end:** `Kronos-Tech-Solution-User-Plataform`  
**Branch alvo:** `v4/fase4/limpeza`  
**Back-end de referência:** `Kronos-Tech-Solutions-KTS`  
**Branch back-end:** `flag/redis`  
**Objetivo:** fechar aderência funcional do front-end ao back-end e evoluir a base para um padrão enterprise.

---

## 0. Escopo e premissas

Este backlog foi escrito para execução com **Codex CLI**, usando tarefas pequenas, revisáveis e orientadas a branch/commit.

### 0.1 Fora de escopo neste backlog

Por decisão técnica, este backlog **não deve implementar nem alterar**:

- registro de ponto;
- check-in/check-out;
- fluxo de `POST /records/checkin`;
- captura facial para marcação de ponto;
- captura de geolocalização para marcação de ponto;
- aceite de termo biométrico;
- revogação de termo biométrico;
- status de termo biométrico;
- qualquer fluxo relacionado a `/terms/status`;
- qualquer fluxo relacionado a `/terms/accept-biometric`;
- qualquer fluxo relacionado a `/terms/revoke-biometric`;
- renovação de token;
- armazenamento de token;
- refresh token;
- política de sessão;
- alteração de `AuthContext` para lidar com novo token;
- logout multiaba baseado em storage/token;
- revisão de estratégia JWT.

### 0.2 Motivo da exclusão

Os fluxos abaixo são responsabilidade de outro front-end:

- aceite de termo biométrico;
- registro de ponto.

Logo, este front-end deve focar nos fluxos administrativos, documentos, relatórios, férias, abonos, auditoria fiscal, contrato de API, testes, observabilidade e padronização enterprise.

### 0.3 Padrões obrigatórios no front-end

Todas as tarefas devem respeitar os padrões existentes na branch:

- usar `api` de `src/config/api.ts`;
- usar `API_ROUTES` e `buildRoute` de `src/config/api-routes.ts`;
- evitar `fetch` direto;
- usar helpers de erro existentes;
- usar helpers de normalização existentes quando aplicável;
- evitar duplicação de services;
- manter tipagem TypeScript;
- preservar strict mode;
- preferir React Query para dados remotos com cache/mutação;
- não criar endpoints que não existam no back-end;
- não implementar nada relacionado aos itens fora de escopo.

### 0.4 Comandos-base de validação

Executar ao final de cada tarefa relevante:

```bash
npm install
npm run lint
npm run build
npm run test
```

Quando a sprint incluir E2E:

```bash
npm run test:e2e
```

> Caso algum script ainda não exista, a tarefa deve criar o script antes de usá-lo.

---

# 1. Sprint 1 — Fechar aderência funcional administrativa ao back-end

## Objetivo da sprint

Fechar lacunas funcionais diretas entre o front-end e o back-end `flag/redis`, **sem incluir registro de ponto, termos biométricos ou token**.

## Resultado esperado

Ao final da sprint:

- datas sensíveis devem ser enviadas no formato esperado pelos DTOs;
- menu e permissões visuais devem estar coerentes com rotas;
- endpoints administrativos do back-end devem estar mapeados;
- documentos, férias, abonos, relatórios, auditoria e cadastros devem ficar mais aderentes ao contrato real;
- fluxos de outro front-end devem estar documentados como fora de escopo.

---

## História 1.1 — Corrigir e validar formato de datas em abonos

### Contexto

O fluxo de abono/esquecimento usa:

```http
POST /records/time-off/request
```

O DTO do back-end pode exigir formato específico de data. O front deve garantir o formato correto antes de enviar o payload.

### Objetivo

Garantir que `POST /records/time-off/request` envie datas no formato esperado pelo back-end.

### Arquivos prováveis

- `src/service/records.service.ts`
- `src/types/vacation.ts`
- `src/utils/date-format.ts`
- `src/pages/RequestManualRegistration.tsx`
- `src/hooks/*timeOff*`
- `src/hooks/*Manual*`

### Implementação esperada

Criar helper reutilizável:

```ts
export const toBackendDatePattern = (isoDate: string): string => {
  const [year, month, day] = isoDate.split("-");

  if (!year || !month || !day) {
    throw new Error("Data inválida.");
  }

  return `${day}-${month}-${year}`;
};
```

Aplicar somente nos endpoints cujo DTO exige esse padrão.

### Prompt sugerido para Codex CLI

```text
Localize o fluxo de solicitação de abono/esquecimento que chama requestTimeOff. Valide o formato de data esperado pelo DTO do back-end e garanta que startDate e endDate sejam enviados no formato correto. Crie helper reutilizável e teste unitário. Não implemente registro de ponto, termos biométricos ou qualquer lógica de token.
```

### Critérios de aceite

- `requestTimeOff` envia `startDate` e `endDate` no formato esperado pelo back-end.
- Datas inválidas são bloqueadas antes do envio.
- O helper possui teste unitário.
- Não quebra férias nem relatórios.
- Nenhum código de check-in/registro de ponto é criado.
- Nenhum código de termos biométricos é criado.
- Nenhum código de token é alterado.

### Testes mínimos

- converte `2026-04-30` para `30-04-2026`, caso esse seja o formato confirmado;
- rejeita string inválida;
- `requestTimeOff` envia payload convertido;
- erro de validação é exibido de forma amigável.

---

## História 1.2 — Validar formato de datas em férias

### Contexto

O fluxo de férias usa:

```http
POST /records/vacation-request
GET /records/vacation-request
PATCH /records/vacation-request/approve
PATCH /records/vacation-request/reject
```

Antes de converter datas de férias, é necessário confirmar o DTO do back-end.

### Objetivo

Validar o formato esperado pelo DTO de férias e aplicar conversão apenas se necessário.

### Arquivos prováveis

- `src/service/records.service.ts`
- `src/types/vacation.ts`
- `src/pages/RequestVacation.tsx`
- `src/pages/VacationApprovals.tsx`
- `src/utils/date-format.ts`

### Prompt sugerido para Codex CLI

```text
Verifique o DTO do back-end usado em POST /records/vacation-request e compare com o payload enviado pelo front em requestVacation. Se o DTO exigir dd-MM-yyyy, aplique o helper toBackendDatePattern. Se aceitar yyyy-MM-dd, apenas adicione teste garantindo o formato atual. Não implemente registro de ponto, termos biométricos ou qualquer lógica de token.
```

### Critérios de aceite

- Formato de férias confirmado.
- Teste cobrindo o formato enviado.
- Conversão aplicada somente se necessária.
- Nenhum impacto em abonos.
- Nenhum endpoint fora de escopo é consumido.

---

## História 1.3 — Corrigir menu de auditoria para CTO

### Contexto

A rota de auditoria fiscal deve ser visível para perfis autorizados no front.

### Objetivo

Garantir que usuários CTO vejam acesso de auditoria fiscal no menu quando a rota permitir esse perfil.

### Arquivos prováveis

- `src/components/Sidebar.tsx`
- `src/config/app-routes.ts`

### Implementação recomendada

Usar metadados de rota como fonte de verdade sempre que possível.

### Prompt sugerido para Codex CLI

```text
Corrija o Sidebar para que a rota de Auditoria Fiscal fique acessível no menu para MANAGER e CTO, conforme APP_ROUTE_META.auditoria.allowedRoles. Evite duplicar regras de role; prefira usar os metadados de rota quando possível. Não implemente registro de ponto, termos biométricos ou qualquer lógica de token.
```

### Critérios de aceite

- MANAGER vê auditoria fiscal.
- CTO vê auditoria fiscal.
- PARTNER não vê auditoria fiscal.
- A rota continua protegida por `RoleRoute`.
- Não há duplicação desnecessária de regra.
- Não altera autenticação/token.

### Testes mínimos

- Sidebar renderiza auditoria para CTO;
- Sidebar renderiza auditoria para MANAGER;
- Sidebar não renderiza auditoria para PARTNER.

---

## História 1.4 — Revisar equivalência de endpoints administrativos entre back-end e front

### Objetivo

Criar uma matriz de aderência entre endpoints do back-end e services/telas do front, marcando explicitamente o que é fora de escopo.

### Arquivos prováveis

- `docs/api-contract-map.md`
- `src/config/api-routes.ts`
- `src/service/*.ts`

### Classificação esperada

Cada endpoint deve ser classificado como:

- `IMPLEMENTADO`;
- `PARCIALMENTE_IMPLEMENTADO`;
- `PENDENTE`;
- `NAO_APLICAVEL_NESTE_FRONT`;
- `FORA_DE_ESCOPO_OUTRO_FRONT`.

### Endpoints que devem ser marcados como fora de escopo

```http
POST /records/checkin
GET /terms/status
POST /terms/accept-biometric
DELETE /terms/revoke-biometric
```

### Prompt sugerido para Codex CLI

```text
Crie docs/api-contract-map.md mapeando os endpoints definidos no back-end contra os services existentes no front-end. Classifique cada endpoint como IMPLEMENTADO, PARCIALMENTE_IMPLEMENTADO, PENDENTE, NAO_APLICAVEL_NESTE_FRONT ou FORA_DE_ESCOPO_OUTRO_FRONT. Marque POST /records/checkin e endpoints de termo biométrico como FORA_DE_ESCOPO_OUTRO_FRONT. Não altere código nessa tarefa.
```

### Critérios de aceite

- Documento criado em `docs/api-contract-map.md`.
- Todos os grupos aparecem:
    - auth;
    - companies;
    - employee;
    - users;
    - messages;
    - documents;
    - records;
    - legal;
    - geolocation.
- Grupo `terms` aparece apenas para registrar que é fora de escopo neste front.
- `/records/checkin` aparece como fora de escopo.
- Pendências administrativas viram backlog técnico.

---

## História 1.5 — Consolidar aderência de documentos

### Contexto

Documentos são responsabilidade deste front e devem continuar aderentes ao back-end.

### Endpoints relevantes

```http
GET /documents
POST /documents
GET /documents/{documentId}
DELETE /documents/{documentId}
```

### Objetivo

Revisar se o service de documentos usa exatamente o contrato do back-end.

### Arquivos prováveis

- `src/service/document.service.ts`
- `src/pages/Documentos.tsx`
- `src/pages/EnviarDocumentos.tsx`
- `src/pages/DocumentoColaborador.tsx`
- `src/types/document.ts`

### Prompt sugerido para Codex CLI

```text
Revise document.service.ts e as telas de documentos para garantir aderência aos endpoints GET /documents, POST /documents, GET /documents/{documentId} e DELETE /documents/{documentId}. Valide params, multipart, responseType blob, employeeId opcional e type obrigatório. Não implemente registro de ponto, termos biométricos ou lógica de token.
```

### Critérios de aceite

- Upload usa multipart correto.
- Download usa `responseType: "blob"`.
- Listagem exige/valida `type`.
- `employeeId` é enviado apenas quando necessário.
- Delete mantém contrato correto.
- Não há endpoints legados como `/documents/upload`, `/documents/me` ou `/documents/employee/{id}`.

---

## História 1.6 — Consolidar aderência de usuários e colaboradores

### Contexto

O front possui fluxos administrativos para colaborador e usuário.

### Endpoints relevantes

```http
GET /employee
POST /employee
GET /employee/{employeeId}
PATCH /employee/manager/update-employee/{employeeId}
GET /employee/own-profile
PATCH /employee/update-own-profile
GET /employee/check-cpf

POST /users
GET /users/search
GET /users/search/username/{userName}
GET /users/search/id/{userId}
PATCH /users/search/{userId}
PATCH /users/toggle-activate/{userId}
GET /users/own-profile
PUT /users/password
GET /users/check-username
```

### Objetivo

Garantir que cadastro, edição, listagem e status estejam alinhados ao contrato.

### Arquivos prováveis

- `src/service/collaborator-management.service.ts`
- `src/service/user.service.ts`
- `src/hooks/useCollaboratorList.ts`
- `src/pages/CriarColaborador.tsx`
- `src/pages/CriarManager.tsx`
- `src/pages/ListaColaboradores.tsx`
- `src/types/employee.ts`
- `src/types/user.ts`

### Prompt sugerido para Codex CLI

```text
Revise services e telas de colaboradores/usuários para garantir aderência aos endpoints reais de /employee e /users. Remova qualquer endpoint legado se existir. Valide payloads de criação, edição, check-cpf, check-username, toggle e listagem. Não implemente registro de ponto, termos biométricos ou lógica de token.
```

### Critérios de aceite

- Cadastro de colaborador usa `POST /employee`.
- Cadastro de usuário usa `POST /users`.
- CPF usa `GET /employee/check-cpf`.
- Username usa `GET /users/check-username`.
- Edição de colaborador usa `PATCH /employee/manager/update-employee/{employeeId}`.
- Edição de usuário usa `PATCH /users/search/{userId}`.
- Toggle de usuário usa `PATCH /users/toggle-activate/{userId}`.
- Não há uso de endpoints antigos.

---

# 2. Sprint 2 — Contrato e testes

## Objetivo da sprint

Reduzir risco de regressão e divergência de contrato entre front e back, sem cobrir fluxos de outro front-end.

---

## História 2.1 — Preparar geração de client por OpenAPI

### Objetivo

Introduzir base para contract-first sem substituir tudo de uma vez.

### Ferramentas candidatas

- `openapi-typescript`
- `orval`
- `openapi-generator-cli`

### Estratégia recomendada

Começar gerando apenas tipos em pasta isolada:

```text
src/generated/api
```

Não substituir services manualmente na primeira etapa.

### Prompt sugerido para Codex CLI

```text
Configure uma estratégia inicial para geração de tipos TypeScript a partir do OpenAPI do back-end. Adicione dependências e scripts necessários, mas não substitua os services existentes ainda. Gere os tipos em src/generated/api ou documente como gerar se o arquivo openapi.json ainda não estiver disponível no repositório. Não implemente registro de ponto, termos biométricos ou qualquer lógica de token.
```

### Critérios de aceite

- Existe script de geração no `package.json`.
- Existe documentação de como gerar.
- Código gerado fica isolado.
- Services atuais não são quebrados.
- Endpoints fora de escopo ficam documentados, mas não implementados.

---

## História 2.2 — Testar `api.ts`

### Objetivo

Garantir que o client HTTP se comporte de forma previsível.

### Casos mínimos

- não usar `fetch`;
- remover `Content-Type` em `FormData`;
- normalizar erro 400;
- normalizar erro 401;
- normalizar erro 403;
- normalizar erro 429;
- normalizar erro 503;
- tratar erro de termos apenas como erro normalizado, sem criar fluxo neste front.

### Prompt sugerido para Codex CLI

```text
Crie testes unitários para src/config/api.ts cobrindo remoção de Content-Type em FormData e normalização de erros HTTP. Preserve o comportamento existente de headers, mas não implemente nenhuma nova política de token. Não crie fluxo de termos biométricos.
```

### Critérios de aceite

- Testes passam.
- Não depende de API real.
- Não usa rede externa.
- Não adiciona comportamento novo de token.
- Não adiciona implementação de termo biométrico.

---

## História 2.3 — Testar `ProtectedRoute` e `RoleRoute`

### Objetivo

Garantir que rotas protegidas e permissões visuais funcionem.

### Prompt sugerido para Codex CLI

```text
Crie testes para ProtectedRoute e RoleRoute. Cubra status checking, authenticated e unauthenticated quando aplicável. Cubra liberação e bloqueio por role. Mocke dependências necessárias. Não implemente refresh, renovação ou armazenamento de token.
```

### Critérios de aceite

- `ProtectedRoute` mostra loading em checking.
- Redireciona para login quando não autenticado.
- Renderiza conteúdo quando autenticado.
- `RoleRoute` libera role permitida.
- `RoleRoute` bloqueia role não permitida.

---

## História 2.4 — Testar services por domínio administrativo

### Domínios obrigatórios

- `auth.service.ts`;
- `company.service.ts`;
- `collaborator-management.service.ts`;
- `user.service.ts`;
- `document.service.ts`;
- `records.service.ts`, exceto `/records/checkin`;
- `fiscal.service.ts`;
- `geolocation.service.ts`;
- `message.service.ts`;
- `dashboard.service.ts`.

### Fora de escopo nos testes

- `/records/checkin`;
- `/terms/status`;
- `/terms/accept-biometric`;
- `/terms/revoke-biometric`;
- renovação/armazenamento de token.

### Prompt sugerido para Codex CLI

```text
Adicione testes unitários para services de domínio administrativo. Mocke a instância api. Valide método HTTP, rota, params, body, FormData e responseType quando aplicável. Não teste nem implemente POST /records/checkin. Não teste nem implemente endpoints de termos biométricos. Não altere token.
```

### Critérios de aceite

- Cada service administrativo tem ao menos um teste de sucesso.
- Fluxos críticos têm teste de erro.
- Multipart é validado.
- Blob/download é validado.
- Rotas batem com `API_ROUTES`.
- Endpoints fora de escopo não são implementados.

---

## História 2.5 — Criar MSW handlers por domínio administrativo

### Objetivo

Permitir testes de integração de componentes sem back-end real.

### Estrutura sugerida

```text
src/test/msw/handlers/auth.handlers.ts
src/test/msw/handlers/company.handlers.ts
src/test/msw/handlers/employee.handlers.ts
src/test/msw/handlers/users.handlers.ts
src/test/msw/handlers/documents.handlers.ts
src/test/msw/handlers/records.handlers.ts
src/test/msw/handlers/legal.handlers.ts
src/test/msw/handlers/geolocation.handlers.ts
src/test/msw/server.ts
```

### Observação

O handler de `records` deve cobrir férias, abonos, relatórios e aprovações, mas **não deve cobrir `POST /records/checkin`**.

### Prompt sugerido para Codex CLI

```text
Configure MSW para testes de integração. Crie handlers por domínio administrativo para auth, companies, employee, users, documents, records administrativos, legal e geolocation. Não crie handlers para POST /records/checkin nem endpoints de termos biométricos.
```

### Critérios de aceite

- MSW inicializa nos testes.
- Handlers cobrem endpoints administrativos críticos.
- Testes não dependem de back-end real.
- Nenhum handler de check-in é criado.
- Nenhum handler de termos biométricos é criado.

---

## História 2.6 — Criar E2E básico com Playwright

### Fluxos mínimos

1. login com sucesso;
2. login com falha;
3. dashboard protegido;
4. solicitação de abono;
5. upload de documento;
6. listagem de aprovações;
7. geração/download fiscal com mock;
8. navegação por perfil/role.

### Fora de escopo no E2E

- registro de ponto;
- check-in/check-out;
- aceite de termo biométrico;
- revogação de termo biométrico;
- renovação de token.

### Prompt sugerido para Codex CLI

```text
Configure Playwright no projeto e crie um conjunto E2E básico com mocks ou ambiente local. Cubra login, rota protegida, solicitação de abono, upload de documento, aprovações e auditoria fiscal. Não implemente nem teste registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- `npm run test:e2e` existe.
- Pelo menos um fluxo crítico administrativo passa.
- Documentação de execução local criada.
- Não há teste de check-in.
- Não há teste de termo biométrico.

---

# 3. Sprint 3 — Fluxos críticos enterprise administrativos

## Objetivo da sprint

Aumentar robustez operacional dos fluxos sensíveis deste front: documentos, fiscal, férias, abonos, cadastros, relatórios e mutações.

---

## História 3.1 — Melhorar upload/download de documentos

### Objetivo

Elevar documentos a padrão enterprise de UX.

### Implementar

- loading por arquivo;
- barra/progresso quando possível;
- bloqueio de duplo envio;
- confirmação de exclusão;
- mensagens específicas por erro;
- retry quando fizer sentido;
- estado vazio;
- validação de extensão/tamanho antes do envio.

### Prompt sugerido para Codex CLI

```text
Melhore a experiência de documentos: adicione estados de loading, empty state, erro específico, confirmação de exclusão, bloqueio de duplo upload e validação visual de arquivo. Preserve o contrato atual de document.service.ts. Não implemente registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- Upload tem feedback.
- Download tem feedback.
- Delete exige confirmação.
- Erros são claros.
- Não há endpoints novos inexistentes.
- Não há fluxos fora de escopo.

---

## História 3.2 — Melhorar geração fiscal com idempotência visual

### Contexto

O back-end usa Redis/idempotência para proteger geração fiscal.

### Objetivo

No front, impedir múltiplos cliques e tratar erro de processamento em andamento.

### Arquivos prováveis

- `src/service/fiscal.service.ts`
- `src/pages/AuditoriaFiscal.tsx`
- `src/pages/EspelhoPonto.tsx`

### Prompt sugerido para Codex CLI

```text
Melhore as telas de geração fiscal para bloquear duplo clique, mostrar estado de geração em andamento e tratar erros 429/503 com mensagem amigável. Preserve os endpoints de fiscal.service.ts. Não implemente registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- Botão fica desabilitado durante download.
- Usuário vê “gerando arquivo”.
- Erro 429 mostra mensagem específica.
- Erro 503 mostra mensagem específica.
- Estado volta ao normal no finally.

---

## História 3.3 — Feedback rico para documentos, relatórios, férias, abonos e fiscal

### Objetivo

Transformar erros técnicos do back-end em mensagens compreensíveis nos fluxos administrativos.

### Mensagens esperadas

| Fluxo | Mensagem sugerida |
|---|---|
| documento inválido | “O arquivo enviado não é aceito. Use PDF, JPG, PNG, DOC ou DOCX.” |
| documento grande | “O arquivo excede o tamanho máximo permitido.” |
| falha no storage | “Não foi possível salvar ou baixar o documento agora.” |
| período fiscal inválido | “Confira as datas inicial e final do período.” |
| relatório em processamento | “Arquivo em geração. Aguarde alguns instantes antes de tentar novamente.” |
| férias duplicadas | “Já existe uma solicitação ou registro para esse período.” |
| abono duplicado | “Já existe registro ou solicitação para a data informada.” |
| manager inválido | “Selecione um gestor válido da mesma empresa.” |

### Prompt sugerido para Codex CLI

```text
Crie um helper de mensagens para erros administrativos: documentos, relatórios, férias, abonos e fiscal. Integre esse helper aos fluxos relevantes preservando normalizeServiceError como base. Não inclua mensagens para registro de ponto, biometria, geolocalização de ponto, NTP ou token.
```

### Critérios de aceite

- Erros técnicos viram mensagens úteis.
- Não há duplicação de strings em múltiplas telas.
- Testes cobrem mapeamentos principais.
- Não há mensagens/fluxos de check-in ou termo biométrico.

---

## História 3.4 — Garantir invalidação de cache após mutações administrativas

### Objetivo

Garantir que telas reflitam mudanças sem reload manual.

### Mutações críticas

- aprovar/rejeitar ajuste de ponto;
- aprovar/rejeitar férias;
- aprovar/rejeitar abono;
- criar aviso;
- excluir aviso;
- criar colaborador;
- atualizar colaborador;
- ativar/desativar usuário;
- upload/exclusão de documento;
- gerar/baixar artefato fiscal quando houver cache associado.

### Fora de escopo

- registrar ponto;
- aceitar/revogar termo biométrico;
- token.

### Prompt sugerido para Codex CLI

```text
Revise hooks com React Query e garanta invalidação de cache após todas as mutações administrativas críticas. Padronize queryKeys por domínio e evite chaves soltas duplicadas. Não implemente registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- Toda mutação administrativa crítica invalida/refaz queries relacionadas.
- Query keys são padronizadas.
- Não há tela que exija refresh manual após mutação.
- Não há invalidação relacionada a check-in ou termos biométricos.

---

## História 3.5 — Consolidar UX de férias e abonos

### Objetivo

Melhorar fluxos operacionais de férias e abonos.

### Implementar

- validação de período;
- bloqueio de data final menor que inicial;
- loading de envio;
- confirmação de envio;
- estado vazio em listagens;
- filtros consistentes;
- mensagens padronizadas;
- cache invalidation após aprovar/rejeitar.

### Prompt sugerido para Codex CLI

```text
Melhore UX de férias e abonos: validação de período, loading, confirmação, estado vazio, filtros consistentes e mensagens padronizadas. Preserve endpoints atuais de records.service.ts. Não implemente registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- Solicitação de férias valida datas.
- Solicitação de abono valida datas/horas.
- Aprovação/rejeição atualiza lista.
- Erros são amigáveis.
- Não há escopo de check-in.

---

# 4. Sprint 4 — Segurança, observabilidade e polish

## Objetivo da sprint

Elevar maturidade operacional e qualidade visual/técnica, sem mexer em token e sem incluir front de ponto/termos.

---

## História 4.1 — Adicionar observabilidade no front

### Objetivo

Capturar erros reais de produção nos fluxos deste front.

### Opções

- Sentry;
- OpenTelemetry;
- ferramenta equivalente.

### Dados que nunca devem ser enviados

- CPF completo;
- faceImageBase64;
- conteúdo de documentos;
- salários em payloads detalhados;
- qualquer credencial;
- qualquer token.

### Prompt sugerido para Codex CLI

```text
Adicione estrutura inicial de observabilidade no front-end usando Sentry ou OpenTelemetry. Capture erros globais, erros de boundary e falhas críticas de API. Use variáveis de ambiente. Não registre dados sensíveis como CPF, token, faceImageBase64, salários detalhados ou conteúdo de documentos. Não implemente registro de ponto ou termos biométricos.
```

### Critérios de aceite

- Observabilidade configurável por env.
- Erros globais capturados.
- ErrorBoundary integrado.
- Dados sensíveis não são enviados.
- Nenhuma política de token é alterada.

---

## História 4.2 — Adicionar correlation ID nas requisições

### Objetivo

Permitir rastrear requisições entre front e back sem alterar autenticação.

### Implementação sugerida

Adicionar header:

```http
X-Correlation-Id: <uuid>
```

### Prompt sugerido para Codex CLI

```text
Adicione geração de X-Correlation-Id no interceptor de request do api.ts. Use crypto.randomUUID quando disponível e fallback seguro quando não estiver. Não altere Authorization, armazenamento de token, refresh, sessão, registro de ponto ou termos biométricos.
```

### Critérios de aceite

- Toda request recebe `X-Correlation-Id`.
- Teste cobre geração do header.
- Não altera headers de multipart indevidamente.
- Não altera token.

---

## História 4.3 — Adicionar bundle analyzer

### Objetivo

Monitorar tamanho do bundle.

### Ferramentas candidatas

- `rollup-plugin-visualizer`
- `vite-bundle-visualizer`

### Prompt sugerido para Codex CLI

```text
Adicione ferramenta de análise de bundle ao projeto Vite. Crie script no package.json para gerar relatório local. Não altere comportamento de produção. Não implemente registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- Existe script `npm run analyze`.
- Relatório é gerado localmente.
- Build normal continua funcionando.

---

## História 4.4 — Revisar acessibilidade mínima

### Objetivo

Melhorar usabilidade e compatibilidade com teclado/leitores de tela.

### Telas prioritárias

- login;
- documentos;
- enviar documentos;
- férias;
- abonos;
- auditoria fiscal;
- colaboradores;
- empresa;
- usuário.

### Fora de escopo

- tela de registro de ponto;
- aceite de termo biométrico.

### Pontos mínimos

- botões com label acessível;
- modais com título/descrição;
- forms com `label`;
- inputs com erro associado;
- navegação por teclado;
- contraste mínimo;
- loading com `role=status`;
- mensagens de erro com `aria-live`.

### Prompt sugerido para Codex CLI

```text
Revise acessibilidade das telas críticas deste front: login, documentos, férias, abonos, auditoria fiscal, colaboradores, empresa e usuário. Adicione labels, aria-live, aria-describedby e navegação por teclado quando necessário. Não implemente telas de registro de ponto nem aceite de termo biométrico.
```

### Critérios de aceite

- Login acessível.
- Formulários críticos com labels.
- Estados de erro são anunciáveis.
- Fluxos administrativos navegáveis por teclado.
- Não há inclusão de telas fora de escopo.

---

## História 4.5 — Padronizar estados de tela

### Objetivo

Criar padrão consistente de loading, erro, vazio e sucesso.

### Componentes sugeridos

```text
src/components/states/LoadingState.tsx
src/components/states/ErrorState.tsx
src/components/states/EmptyState.tsx
src/components/states/SuccessState.tsx
```

### Prompt sugerido para Codex CLI

```text
Crie componentes reutilizáveis para LoadingState, ErrorState e EmptyState. Aplique gradualmente nas telas de documentos, aprovações, férias, abonos, auditoria e colaboradores. Preserve layout atual. Não implemente registro de ponto, termos biométricos ou token.
```

### Critérios de aceite

- Componentes reutilizáveis existem.
- Pelo menos três telas críticas usam o padrão.
- Mensagens ficam consistentes.
- Não há telas fora de escopo adicionadas.

---

# 5. Checklist final para considerar 100%

## 5.1 Aderência ao back-end neste front

- [ ] Todos os endpoints administrativos necessários estão mapeados no front.
- [ ] `/records/checkin` está documentado como `FORA_DE_ESCOPO_OUTRO_FRONT`.
- [ ] `/terms/status` está documentado como `FORA_DE_ESCOPO_OUTRO_FRONT`.
- [ ] `/terms/accept-biometric` está documentado como `FORA_DE_ESCOPO_OUTRO_FRONT`.
- [ ] `/terms/revoke-biometric` está documentado como `FORA_DE_ESCOPO_OUTRO_FRONT`.
- [ ] Datas administrativas são enviadas no formato esperado por cada DTO.
- [ ] Enums do front iguais aos enums usados nos fluxos administrativos do back.
- [ ] Uploads multipart compatíveis.
- [ ] Downloads blob compatíveis.
- [ ] Erros do back exibidos corretamente.
- [ ] RBAC do menu igual ao RBAC das rotas.
- [ ] Nenhuma regra de token foi alterada neste backlog.

## 5.2 Nível enterprise

- [ ] OpenAPI client ou tipos gerados automaticamente.
- [ ] Testes unitários nos helpers e services administrativos.
- [ ] Testes de componente nas telas críticas administrativas.
- [ ] Testes E2E dos fluxos administrativos principais.
- [ ] CI bloqueia build quebrado.
- [ ] CI bloqueia teste quebrado.
- [ ] Observabilidade no front.
- [ ] Tratamento visual de idempotência nos relatórios fiscais.
- [ ] UX robusta para loading, erro, retry e empty state.
- [ ] Segurança revisada para dados sensíveis.
- [ ] Acessibilidade mínima validada.
- [ ] Bundle/performance monitorados.
- [ ] Registro de ponto permanece fora deste front.
- [ ] Termo biométrico permanece fora deste front.
- [ ] Token permanece fora deste backlog.

---

# 6. Sequência recomendada de branches

## Sprint 1

```bash
git checkout v4/fase4/limpeza
git pull
git checkout -b codex/sprint-1-admin-adherence
```

Commits sugeridos:

```text
fix(records): normalize administrative date payloads
fix(routes): expose fiscal audit menu to CTO
docs(api): map backend endpoints and external frontend scope
fix(documents): align document contract usage
fix(users): align employee and user administrative contracts
```

## Sprint 2

```bash
git checkout v4/fase4/limpeza
git pull
git checkout -b codex/sprint-2-contract-tests
```

Commits sugeridos:

```text
chore(openapi): add contract generation setup
test(api): cover http client interceptors
test(routes): cover protected and role routes
test(services): cover administrative domain services
test(msw): add administrative api handlers
test(e2e): add playwright administrative smoke flows
```

## Sprint 3

```bash
git checkout v4/fase4/limpeza
git pull
git checkout -b codex/sprint-3-enterprise-admin-flows
```

Commits sugeridos:

```text
feat(documents): improve upload and download states
feat(fiscal): add generation lock feedback
feat(errors): improve administrative error messages
refactor(query): standardize administrative cache invalidation
feat(records): improve vacation and time-off UX
```

## Sprint 4

```bash
git checkout v4/fase4/limpeza
git pull
git checkout -b codex/sprint-4-observability-polish
```

Commits sugeridos:

```text
feat(observability): add frontend error monitoring
feat(api): add correlation id header
chore(bundle): add bundle analyzer
fix(a11y): improve critical administrative screens accessibility
refactor(ui): standardize loading error empty states
```

---

# 7. Prompt mestre para iniciar cada sprint no Codex CLI

Use este prompt no início da sprint, ajustando o número da sprint:

```text
Você está no repositório Kronos-Tech-Solution-User-Plataform, branch v4/fase4/limpeza.

Execute a Sprint <N> descrita no arquivo docs/codex-sprints-frontend-enterprise.md.

Regras:
- Não implemente registro de ponto.
- Não implemente POST /records/checkin.
- Não implemente aceite, revogação ou status de termo biométrico.
- Não implemente GET /terms/status.
- Não implemente POST /terms/accept-biometric.
- Não implemente DELETE /terms/revoke-biometric.
- Não implemente renovação, armazenamento, refresh ou política de token.
- Não altere AuthContext por causa de token.
- Não use fetch direto.
- Use api.ts, API_ROUTES e buildRoute.
- Mantenha TypeScript strict.
- Preserve padrões existentes de erro e normalização.
- Faça mudanças pequenas e revisáveis.
- Adicione ou ajuste testes quando a tarefa pedir.
- Ao final, rode npm run lint, npm run build e npm run test.
- Mostre resumo dos arquivos alterados, decisões tomadas e pendências restantes.
```

---

# 8. Prompt de revisão para Codex CLI após cada sprint

```text
Revise as alterações da sprint atual.

Verifique:
- se algum fetch direto foi introduzido;
- se algum endpoint inexistente foi chamado;
- se POST /records/checkin foi implementado indevidamente;
- se endpoints de termo biométrico foram implementados indevidamente;
- se houve alteração indevida de token/sessão/AuthContext;
- se as rotas usam API_ROUTES/buildRoute quando aplicável;
- se os testes cobrem os fluxos alterados;
- se npm run lint, npm run build e npm run test passam;
- se há quebra de contrato com o back-end flag/redis.

Gere um relatório objetivo com:
1. problemas encontrados;
2. correções recomendadas;
3. riscos restantes;
4. checklist de aceite.
```

---

# 9. Critério de conclusão do backlog

Este backlog pode ser considerado concluído quando:

- Sprint 1 estiver funcionalmente validada contra o back-end local/staging nos fluxos administrativos;
- Sprint 2 tiver testes mínimos rodando em CI;
- Sprint 3 tiver UX robusta nos fluxos críticos administrativos;
- Sprint 4 tiver observabilidade, acessibilidade mínima e controle de bundle;
- nenhum item de token tiver sido implementado;
- nenhum fluxo de registro de ponto tiver sido implementado;
- nenhum fluxo de termo biométrico tiver sido implementado;
- pendências de token estiverem registradas em backlog separado;
- registro de ponto e termo biométrico estiverem documentados como responsabilidade de outro front-end.
