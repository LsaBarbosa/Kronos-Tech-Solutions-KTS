# SKILLS — Back-end Gap Closure EPICs 01 a 07

## 1. Objetivo

Este documento define as habilidades operacionais necessárias para validar o back-end durante a correção dos gaps dos EPICs 01 a 07.

A função do back-end nesta trilha é proteger contratos e registrar bloqueios. A implementação visual permanece no front-end.

---

# Skill 01 — Contract Mapping

## Quando usar

Usar quando uma task do front-end depender de endpoint, payload, status ou campo vindo da API.

## Procedimento

1. Identificar a tela ou componente front-end impactado.
2. Identificar serviço front-end correspondente.
3. Mapear rota usada.
4. Encontrar controller no back-end.
5. Encontrar DTO de request e response.
6. Confirmar método HTTP.
7. Confirmar status HTTP esperado.
8. Registrar se o contrato atende ao gap.

## Saída

```text
CONTRACT_MAPPING:
- tela:
- serviço front:
- endpoint:
- método:
- controller:
- request DTO:
- response DTO:
- status esperado:
- atende ao gap: sim/nao
- observações:
```

---

# Skill 02 — Main vs New UI Contract Diff

## Quando usar

Usar para confirmar se a branch `new-ui` alterou contratos relevantes em relação à `main`.

## Procedimento

Executar, quando houver workspace local:

```bash
git diff main...new-ui -- src/main/java src/main/resources
```

Analisar:

- controllers;
- DTOs;
- configuração web;
- autenticação;
- autorização;
- exception handlers;
- application properties;
- migrations.

## Classificação

| Classificação | Uso |
|---|---|
| Sem impacto | alteração interna sem efeito no front |
| Baixo risco | mudança pequena e compatível |
| Médio risco | exige teste específico |
| Alto risco | pode quebrar front |
| Bloqueador | impede continuidade sem decisão |

---

# Skill 03 — Access Users Support Check

## Quando usar

Usar para fechar o gap de usuários de acesso do EPIC 6.

## Procedimento

Verificar se o back-end permite:

- listar usuários;
- consultar role;
- consultar status;
- consultar colaborador vinculado;
- ativar acesso;
- desativar acesso;
- alterar role;
- consultar último acesso.

## Decisão

Se não houver suporte, não criar endpoint automaticamente. Registrar bloqueio para o front-end.

## Saída

```text
ACCESS_USERS_CHECK:
- listagem:
- detalhe:
- role:
- status:
- colaborador vinculado:
- ativar/desativar:
- alterar role:
- último acesso:
- bloqueio:
```

---

# Skill 04 — Public Auth Contract Check

## Quando usar

Usar nas correções dos gaps de autenticação pública.

## Procedimento

Validar contratos de:

- login por senha;
- login facial;
- recuperação de senha;
- redefinição de senha;
- token inválido;
- retorno neutro;
- erros de autenticação.

## Atenção

Não alterar sem autorização:

- regra de autenticação;
- payload de login;
- cookies;
- headers;
- status HTTP;
- mensagens de erro se o front depender delas.

---

# Skill 05 — Dashboard and Check-in Contract Check

## Quando usar

Usar nas correções do EPIC 5.

## Procedimento

Validar:

- status do dia;
- check-in;
- localização;
- face ou biometria;
- pendência de termo;
- sucesso;
- erro;
- retry.

## Saída

```text
DASHBOARD_CHECKIN_CONTRACT:
- status do dia:
- check-in:
- erros:
- retry:
- pendência de termo:
- riscos:
```

---

# Skill 06 — Admin Management Contract Check

## Quando usar

Usar nas correções de empresas, colaboradores e usuários de acesso.

## Procedimento

Verificar:

- listagem de empresas;
- criação de empresa;
- atualização de empresa;
- geolocalização;
- listagem de colaboradores;
- criação de colaborador;
- edição de colaborador;
- ativação e desativação;
- CPF;
- username.

## Saída

Relatório de compatibilidade para EPIC 6.

---

# Skill 07 — Communication and Documents Contract Check

## Quando usar

Usar nas correções do EPIC 7.

## Procedimento

Verificar avisos:

- listagem;
- criação;
- detalhe;
- exclusão;
- prioridades;
- permissões.

Verificar documentos:

- listagem;
- filtros;
- upload;
- download;
- exclusão;
- status;
- erros.

## Saída

Relatório de compatibilidade para EPIC 7.

---

# Skill 08 — Test and Build Validation

## Quando usar

Usar no fechamento da trilha ou após qualquer alteração back-end autorizada.

## Procedimento

Executar:

```bash
./gradlew test
./gradlew build
```

Registrar:

- comando executado;
- resultado;
- erro principal;
- se a falha é relacionada à trilha;
- próxima ação.

---

# Skill 09 — Risk Report Writing

## Quando usar

Usar ao final de qualquer task de validação.

## Template

```text
TASK:
AREA:
FILES_ANALYZED:
CONTRACTS_ANALYZED:
FINDINGS:
RISKS:
BLOCKERS:
TESTS:
RECOMMENDATION:
```

---

# Skill 10 — Stop Condition

## Quando usar

Usar quando houver risco de mudança indevida no back-end.

## Parar se encontrar

- endpoint ausente exigido pelo front;
- payload incompatível;
- regra de autorização não confirmada;
- necessidade de alteração de schema;
- necessidade de nova regra de negócio;
- alteração de autenticação;
- alteração de contrato público.

## Ação

Registrar bloqueio e aguardar decisão. Não implementar por conta própria.

---

## Checklist final de skills

Antes de encerrar a trilha back-end, confirmar:

- contratos mapeados;
- usuários de acesso verificados;
- riscos classificados;
- testes executados ou motivo de não execução registrado;
- bloqueios informados ao front-end;
- nenhuma regra alterada sem autorização.
