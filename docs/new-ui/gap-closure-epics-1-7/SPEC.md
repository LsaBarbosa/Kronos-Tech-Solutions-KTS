# SPEC — Back-end Gap Closure EPICs 01 a 07

## 1. Finalidade

Esta SPEC define a atuação do back-end na trilha de correção dos gaps dos EPICs 01 a 07 da branch `new-ui`.

A implementação principal dos gaps ocorre no front-end. O back-end deve atuar como camada de contrato, validação e diagnóstico.

---

## 2. Objetivo

Garantir que os fluxos corrigidos no front-end tenham suporte contratual no back-end, sem alterar regra de negócio indevidamente.

Objetivos específicos:

- mapear endpoints consumidos pelo front;
- verificar DTOs de request e response;
- validar autenticação, autorização, CORS, cookies e headers;
- confirmar suporte para usuários de acesso;
- identificar bloqueios reais para a implementação do front;
- executar testes disponíveis;
- produzir relatório final.

---

## 3. Escopo incluído

Esta SPEC cobre validação back-end para:

- autenticação pública;
- login facial;
- recuperação e redefinição de senha;
- dashboard;
- registro de ponto;
- empresas;
- colaboradores;
- usuários de acesso;
- avisos;
- documentos;
- upload, download e exclusão de documentos.

---

## 4. Fora de escopo

Não faz parte desta trilha:

- reescrever controllers;
- criar endpoints novos sem autorização;
- alterar entidades;
- criar migrations;
- mudar autenticação;
- mudar autorização por perfil;
- mudar regra de ponto;
- mudar regra de documentos;
- mudar regra de empresas ou colaboradores;
- mudar contratos apenas por ajuste visual.

---

## 5. Requisitos funcionais de validação

### REQ-BE-01 — Autenticação

Validar se o back-end suporta os fluxos usados pelas telas públicas:

- login por senha;
- login facial;
- recuperação de senha;
- redefinição de senha;
- retorno neutro;
- tratamento de token inválido.

### REQ-BE-02 — Dashboard e ponto

Validar contratos de:

- status do dia;
- check-in;
- respostas de sucesso;
- respostas de erro;
- pendência de termo;
- retry seguro.

### REQ-BE-03 — Empresas

Validar contratos de:

- listagem;
- criação;
- atualização;
- geolocalização;
- status;
- payloads de erro.

### REQ-BE-04 — Colaboradores

Validar contratos de:

- listagem;
- criação;
- edição;
- ativação;
- desativação;
- CPF;
- username;
- vínculo com empresa.

### REQ-BE-05 — Usuários de acesso

Validar se existe suporte para:

- listagem de usuários de acesso;
- role;
- status;
- colaborador vinculado;
- ativar e desativar;
- alterar role;
- último acesso, se disponível.

Se não houver suporte, registrar bloqueio para o front-end.

### REQ-BE-06 — Avisos

Validar contratos de:

- listagem;
- criação;
- detalhe;
- filtro por prioridade;
- exclusão;
- permissão por role.

### REQ-BE-07 — Documentos

Validar contratos de:

- listagem;
- filtros;
- upload;
- download;
- exclusão;
- status;
- erro de listagem;
- erro de upload.

---

## 6. Requisitos técnicos

### REQ-TECH-01 — Comparação com main

Quando houver workspace local, comparar:

```bash
git diff main...new-ui -- src/main/java src/main/resources
```

### REQ-TECH-02 — Testes

Executar quando possível:

```bash
./gradlew test
./gradlew build
```

### REQ-TECH-03 — Classificação de risco

Cada diferença encontrada deve ser classificada como:

- sem impacto;
- risco baixo;
- risco médio;
- risco alto;
- bloqueador.

### REQ-TECH-04 — Relatório

A saída deve informar:

- arquivos analisados;
- endpoints analisados;
- DTOs analisados;
- contratos preservados;
- riscos;
- bloqueios;
- testes executados;
- recomendação final.

---

## 7. Sequência recomendada

1. Confirmar branch `new-ui`.
2. Ler `BACKLOG.md`, `TASKS.md`, `RULES.md`, `AGENTS.md` e `BACKEND_CONTRACT_GUARD.md`.
3. Mapear endpoints usados pelo front-end.
4. Mapear DTOs relacionados.
5. Verificar suporte para usuários de acesso.
6. Comparar com `main` se possível.
7. Executar testes.
8. Produzir relatório final.

---

## 8. Definition of Done

A SPEC estará atendida quando:

- houver validação contratual documentada;
- suporte ou bloqueio de usuários de acesso estiver claro;
- contratos consumidos pelo front estiverem mapeados;
- riscos estiverem classificados;
- testes forem executados ou falhas forem documentadas;
- nenhuma regra de negócio for alterada sem autorização.
