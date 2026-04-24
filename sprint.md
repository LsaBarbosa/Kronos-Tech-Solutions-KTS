# Backlog de Refatoração do Front-end
## Projeto: `Kronos-Tech-Solution-User-Plataform`
## Branch analisada: `v4/fase4/limpeza`
## Backend de referência: `Kronos-Tech-Solutions-KTS`
## Branch do backend: `main`

---

## 1. Objetivo

Este documento consolida a análise do front-end com foco em:

- aderência real ao contrato do backend
- correção de payloads, tipos e respostas esperadas
- eliminação dos últimos desalinhamentos estruturais
- organização das mudanças em formato de backlog priorizado

A branch `v4/fase4/limpeza` já executou uma limpeza importante da base. Portanto, este backlog não parte mais de um cenário de front completamente legado. O foco agora é **alinhamento fino e definitivo com o backend**.

---

## 2. Conclusão executiva

A base do projeto evoluiu bastante nessa branch.

Já existem elementos estruturais corretos e que **não devem ser refeitos do zero**:

- base de testes com `Vitest`, `Testing Library` e `MSW`
- `AuthProvider` aplicado na árvore principal
- `ProtectedRoute` usando o estado real da sessão
- client HTTP centralizado com `axios` e interceptors
- rotas centralizadas em `api-routes.ts`
- `auth.service.ts`, `document.service.ts`, `records.service.ts`, `session-profile.service.ts`
- helpers de erro e normalização
- remoção de boa parte do legado antigo

O problema atual do projeto não está mais na fundação. O problema está em **pontos específicos onde o front ainda não reflete exatamente o que o backend espera receber ou retornar**.

Os principais riscos, em ordem de prioridade, estão em:

1. listagem/edição de colaboradores e usuários
2. contrato de empresas e uso de geolocalização
3. domínio de abonos, férias e tipos/status de registros
4. pequenos desalinhamentos de payload e endpoint

---

## 3. O que já está correto e não deve ser refeito

### 3.1 Estrutura de autenticação e sessão
A árvore principal já usa `AuthProvider`, e a sessão está centralizada.

### 3.2 Proteção de rotas
`ProtectedRoute` já usa o status do contexto (`checking`, `authenticated`, `unauthenticated`), o que é correto.

### 3.3 Client HTTP centralizado
A aplicação já possui `api.ts` com interceptors, adição automática de token e tratamento de erro de termos.

### 3.4 Base de testes
A branch já adicionou testes unitários e de integração simulando o backend.

### 3.5 Consolidação de vários serviços
Domínios como autenticação, documentos, records, fiscal e sessão já estão muito mais organizados do que estavam nas versões anteriores.

---

## 4. Principais problemas restantes

### 4.1 O front ainda assume respostas que o backend não entrega
O caso mais crítico está no domínio de usuários e colaboradores.

### 4.2 Há tipos front-end semanticamente incorretos
Especialmente em `recordApproval.ts` e `vacation.ts`, onde request types e status do domínio ainda aparecem misturados.

### 4.3 Há fluxos que funcionam por coincidência, não por contrato oficialmente alinhado
O maior exemplo disso está no onboarding de empresa.

### 4.4 Geolocalização ainda está espalhada no front
Ainda há uso de integração externa e lógica em componente/página onde deveria haver centralização.

---

# 5. Backlog priorizado

---

## P0 — Bloqueadores funcionais

---

### BL-01 — Corrigir o contrato de listagem de usuários usado no fluxo de colaboradores

#### Problema
O front usa `listUsers()` para cruzar usuários com colaboradores e assume que cada item retornado possui `employeeId`.

Porém, o backend atual em `/users/search` retorna apenas:

- `userId`
- `username`
- `role`
- `active`

Sem `employeeId`.

#### Impacto
O merge realizado entre colaborador e usuário na listagem de colaboradores fica estruturalmente quebrado ou frágil.

Isso afeta diretamente:

- tela de listagem de colaboradores
- edição de usuário vinculado ao colaborador
- ativação/desativação de usuário
- consistência entre dados de employee e user

#### O que alterar
No front:

- criar um tipo específico para a resposta de `/users/search`
- remover a suposição de que esse endpoint traz `employeeId`
- parar de reaproveitar `UserAccountData` para respostas que não são de own-profile

No backend:

- idealmente incluir `employeeId` em `/users/search`, porque a tela administrativa precisa desse vínculo

#### Tipo de intervenção
- Front-end: obrigatório
- Backend: recomendado para alinhamento definitivo

#### Critério de aceite
- a lista de colaboradores consegue cruzar corretamente colaborador e usuário
- a edição do usuário vinculado ao colaborador não depende de heurística frágil
- o contrato consumido pelo front reflete exatamente o que o backend retorna

---

### BL-02 — Corrigir o contrato de empresa no front: o backend não retorna `location`

#### Problema
Os tipos do front modelam `CompanyListItem` e `CompanyData` com `location`, mas a resposta atual do backend para empresa não entrega esse campo na leitura.

A resposta exposta pelo backend contém:

- `id`
- `name`
- `cnpj`
- `email`
- `active`
- `address`
- `activeEmployees`
- `inactiveEmployees`

#### Impacto
A edição de empresa depende de `location` carregada da API em pontos como:

- reset de formulário
- comparação de alterações
- reaproveitamento de latitude/longitude
- montagem de payload de update

Isso pode gerar quebra ou estado inconsistente.

#### O que alterar
- tornar `location` opcional nos tipos de leitura do front ou removê-la desses tipos
- ajustar `fetchCompanyDetails()` para refletir o contrato real
- revisar `useUpdateCompanyForm` para não depender de `location` retornada
- tratar geolocalização como dado derivado apenas quando o endereço mudar

#### Tipo de intervenção
- Front-end: obrigatório
- Backend: opcional, caso se deseje devolver `location` também na leitura

#### Critério de aceite
- busca de empresa funciona sem depender de `location`
- edição de empresa não quebra quando `location` não vier do backend
- os tipos TS ficam aderentes ao payload real

---

### BL-03 — Saneamento semântico do domínio de abono, esquecimento e ponto

#### Problema
O front ainda mistura:

- **tipo de solicitação** enviado no request
- **status do registro** persistido no domínio

O backend usa no request:

- `TIME_OFF_REQUEST`
- `FORGOTTEN_REGISTRATION`

Mas os status do domínio caminham em outra linha, por exemplo:

- `TIME_OFF_REQUEST`
- `WORK_TIME_REQUEST`
- `TIME_OFF_REJECTED`
- `WORK_TIME_REJECTED`

No front, ainda existem sinais de inconsistência como:

- `FORGOTTEN_REGISTRATION` tratado como status
- ausência de `WORK_TIME_REJECTED`
- members duplicados
- duplicação de tipos

#### Impacto
Isso afeta:

- filtros
- renderização de status
- validação de formulários
- telas de aprovação
- entendimento do domínio

#### O que alterar
- separar `RequestType` de `StatusRecord`
- manter no request apenas:
  - `TIME_OFF_REQUEST`
  - `FORGOTTEN_REGISTRATION`
- ajustar o enum/status de registros para refletir o domínio real
- remover duplicações em `recordApproval.ts` e `vacation.ts`

#### Tipo de intervenção
- Front-end: obrigatório

#### Critério de aceite
- o front distingue corretamente tipo de solicitação e status do registro
- as telas de aprovação e listagem usam os estados reais do backend
- não existem enums/uniões duplicadas ou semanticamente incorretas

---

## P1 — Alta prioridade

---

### BL-04 — Remover `password` do payload de criação de usuário

#### Problema
O front ainda define payload de criação de usuário com `password`, mas o backend `CreateUserRequest` aceita apenas:

- `username`
- `role`
- `employeeId`

#### Impacto
- ruído de contrato
- falsa expectativa na UI
- manutenção mais difícil

#### O que alterar
- remover `password` do type de criação de usuário
- remover envio de `password` no fluxo de `CriarColaborador`
- ajustar mensagens da interface, deixando claro que a senha inicial não faz parte desse contrato

#### Tipo de intervenção
- Front-end: obrigatório

#### Critério de aceite
- `POST /users` envia apenas os campos aceitos pelo backend

---

### BL-05 — Corrigir `toggleUserStatus` para refletir o endpoint real

#### Problema
O front envia body em `PATCH /users/toggle-activate/{userId}` com `{ active: !currentStatus }`, mas o backend expõe esse endpoint como toggle puro, sem body.

#### Impacto
- acoplamento desnecessário
- ruído no contrato
- maior chance de confusão futura

#### O que alterar
- chamar o endpoint sem body
- manter a lógica de alternância apenas no cliente para feedback visual

#### Tipo de intervenção
- Front-end: obrigatório

#### Critério de aceite
- o endpoint é chamado apenas com path param, sem payload extra

---

### BL-06 — Consolidar e normalizar os tipos do domínio de usuário

#### Problema
O front ainda reaproveita conceitos de usuário entre endpoints com contratos distintos:

- `/users/own-profile`
- `/users/search`

Esses endpoints não retornam o mesmo shape.

#### O que alterar
- criar tipos separados para:
  - own profile
  - listagem de usuários
  - dados combinados de sessão
- manter `session-profile.service.ts` como camada oficial de composição de sessão
- impedir reaproveitamento indevido de tipos entre endpoints diferentes

#### Tipo de intervenção
- Front-end: obrigatório

#### Critério de aceite
- nenhum tipo de listagem herda campos que só existem em own-profile
- os serviços ficam semanticamente corretos

---

### BL-07 — Corrigir o fluxo de empresas para refletir o onboarding real

#### Problema
A tela `CriarEmpresa.tsx` envia apenas dados da empresa e depois redireciona para criação de administrador em outra etapa.

Só que o DTO `CreateCompanyRequest` do backend contém `employeeRequest` embutido.

Ao mesmo tempo, o `CompanyService.createCompany()` atual não usa esse campo e salva apenas a empresa.

#### Leitura correta
Hoje esse fluxo está funcional por coincidência da implementação do service, não por aderência plena ao DTO do backend.

#### O que alterar
Definir oficialmente um dos dois caminhos:

**Opção A — Fluxo em 2 etapas**
- simplificar DTO do backend
- deixar explícito que empresa e primeiro administrador são criados separadamente

**Opção B — Fluxo em 1 etapa**
- fazer o front enviar empresa + `employeeRequest`
- fazer o backend realmente usar isso no onboarding

#### Tipo de intervenção
- Front-end + Backend

#### Critério de aceite
- o fluxo de onboarding de empresa fica oficialmente coerente entre DTO, service e UI

---

## P2 — Média prioridade

---

### BL-08 — Tirar a geolocalização externa da página e centralizar o domínio de empresa

#### Problema
Ainda há geocodificação espalhada no front usando ViaCEP e HERE API.

Isso aparece tanto em service quanto em página.

#### Impacto
- segredo exposto no front
- duplicação de lógica
- dificuldade de teste
- maior custo de manutenção

#### O que alterar
- definir um único ponto responsável por geocodificação
- idealmente mover esse fluxo para o backend
- se ficar no front por enquanto, centralizar exclusivamente no service
- remover `fetch` direto em páginas/componentes

#### Tipo de intervenção
- Front-end: obrigatório
- Backend: recomendado para solução mais enterprise

#### Critério de aceite
- não existe geolocalização implementada em página
- a lógica fica centralizada
- o projeto deixa de depender de múltiplos pontos de integração externa no front

---

### BL-09 — Saneamento de tipos duplicados e nomes inconsistentes

#### Problema
Ainda restam sinais de duplicação e inconsistência de nomenclatura em tipos de records/vacation.

#### O que alterar
- remover duplicações de interfaces
- centralizar tipos por domínio
- padronizar nomes e responsabilidades

#### Tipo de intervenção
- Front-end: obrigatório

#### Critério de aceite
- cada conceito possui um único type/interface
- não existem definições contraditórias no projeto

---

### BL-10 — Tornar o módulo de férias consistente com paginação e shape do backend

#### Problema
O módulo de férias está funcional, mas ainda precisa de formalização melhor do retorno do backend, paginação e modelagem de resposta.

#### O que alterar
- formalizar o tipo da resposta de férias
- alinhar paginação, filtros e totalizações
- remover dependências de cast implícito e fallback excessivamente genérico

#### Tipo de intervenção
- Front-end: obrigatório

#### Critério de aceite
- o módulo de férias trabalha com tipos estáveis e com resposta aderente ao backend

---

### BL-11 — Revisar contagens baseadas em carga massiva

#### Problema
Há contagens que ainda dependem de buscar grandes volumes de dados e contar localmente.

#### Impacto
- escala mal
- pode distorcer indicadores
- gera custo desnecessário no front

#### O que alterar
- preferir metadados reais de paginação
- se necessário, criar endpoint de count no backend

#### Tipo de intervenção
- Front-end ou Front-end + Backend

#### Critério de aceite
- os contadores deixam de depender de carga massiva artificial

---

## P3 — Melhorias estruturais

---

### BL-12 — Revisar o mapeamento final do domínio de documentos

#### Situação atual
Esse domínio já está bem próximo do ideal.

#### O que fazer
- revisar apenas os nomes finais de campos retornados pela API
- garantir consistência de mapeamento

#### Tipo de intervenção
- Front-end: opcional de robustez

---

### BL-13 — Manter mensagens, fiscal e sessão como domínios estáveis

#### Situação atual
Esses domínios estão estruturalmente mais corretos.

#### O que fazer
- não reescrever
- apenas reforçar testes e pequenos ajustes quando necessário

#### Tipo de intervenção
- Front-end: manutenção leve

---

# 6. Backlog consolidado por sprint

## Sprint 1 — Correção de contrato crítico

1. corrigir o contrato de `/users/search`
2. ajustar merge colaborador ↔ usuário
3. separar tipos de own-profile e search
4. corrigir o fluxo da lista de colaboradores

## Sprint 2 — Empresas e contrato de leitura/update

5. remover dependência rígida de `location` nas respostas de empresa
6. ajustar `useUpdateCompanyForm`
7. revisar tipos `CompanyListItem` e `CompanyData`
8. definir o fluxo oficial de onboarding de empresa

## Sprint 3 — Records, férias e abonos

9. separar `RequestType` de `StatusRecord`
10. corrigir enums/status incorretos
11. remover duplicações em `recordApproval.ts` e `vacation.ts`
12. formalizar paginação e retorno do módulo de férias

## Sprint 4 — Payloads e limpeza fina

13. remover `password` do payload de criação de usuário
14. corrigir `toggleUserStatus`
15. revisar contratos residuais de services
16. reforçar testes sobre os contratos corrigidos

## Sprint 5 — Evolução estrutural

17. centralizar geocodificação
18. reduzir dependência de integração externa em páginas
19. revisar contadores baseados em carga massiva
20. congelar domínios estáveis para evitar regressão

---

# 7. Ordem recomendada de execução

A ordem mais segura para implementação é:

1. corrigir o contrato de usuários na gestão de colaboradores
2. corrigir os tipos e o merge do módulo de colaboradores
3. corrigir o contrato de empresas e o uso de `location`
4. sanear o domínio de records/férias/abonos
5. remover payloads/body desnecessários
6. centralizar geolocalização
7. ampliar cobertura de testes nas áreas corrigidas

---

# 8. Critérios de aceite gerais

O backlog pode ser considerado concluído quando estas condições forem verdadeiras:

1. o front não assume campos que o backend não retorna
2. os tipos TS refletem os DTOs reais do backend
3. o domínio de colaboradores cruza employee e user de forma consistente
4. o domínio de empresa não depende de `location` inexistente na leitura
5. request type e status de records estão semanticamente corretos
6. criação de usuário envia apenas o payload aceito pelo backend
7. toggle de usuário reflete o contrato real
8. geolocalização não está espalhada em páginas/componentes
9. os módulos críticos possuem testes cobrindo os contratos corrigidos

---

# 9. Conclusão final

A branch `v4/fase4/limpeza` já fez a maior parte da limpeza estrutural pesada.

O que falta agora é menos sobre “reescrever o projeto” e mais sobre:

- alinhar contratos
- corrigir suposições erradas do front sobre o backend
- estabilizar o domínio operacional
- consolidar a tipagem definitiva

A prioridade real neste momento é:

- **contrato de usuários na gestão de colaboradores**
- **contrato de empresas e uso de `location`**
- **saneamento do domínio de records/férias/abonos**

Esses são os pontos que mais impactam aderência real entre front e backend.
