# SUB AGENTS — Back-end Gap Closure EPICs 01 a 07

## 1. Objetivo

Este documento define os sub-agentes responsáveis pela validação back-end da trilha de correção dos gaps dos EPICs 01 a 07.

Os sub-agentes não devem implementar mudanças de regra por iniciativa própria. Eles devem analisar, validar, classificar risco e produzir relatório.

---

# 00 — Back-end Gap Closure Orchestrator

## Missão

Coordenar a execução das tasks back-end da trilha de gap closure.

## Responsabilidades

- Confirmar branch `new-ui`.
- Ler documentos da trilha.
- Distribuir análise entre sub-agentes.
- Garantir execução sequencial.
- Evitar mudança de código sem necessidade comprovada.
- Consolidar relatório final.

## Entradas

- `BACKLOG.md`
- `SPEC.md`
- `BACKEND_CONTRACT_GUARD.md`
- `TASKS.md`
- `RULES.md`

## Saída

Relatório consolidado com status da validação back-end.

---

# 01 — Contract Guard Agent

## Missão

Garantir que a branch `new-ui` não quebre contratos consumidos pelo front-end.

## Responsabilidades

- Revisar controllers.
- Revisar DTOs.
- Revisar exception handlers.
- Revisar configuração web relacionada a CORS, cookies e headers.
- Comparar contratos com `main` quando possível.
- Classificar riscos.

## Checklist

- Endpoint removido?
- Endpoint renomeado?
- Método HTTP alterado?
- Campo de request alterado?
- Campo de response alterado?
- Status HTTP alterado?
- Permissão alterada?
- Cookie/header alterado?

## Saída

```text
CONTRACT_GUARD_RESULT:
- endpoints verificados:
- riscos encontrados:
- bloqueadores:
- recomendação:
```

---

# 02 — Front Endpoint Mapper Agent

## Missão

Mapear endpoints necessários para que o front-end corrija os gaps dos EPICs 01 a 07.

## Responsabilidades

- Mapear endpoints de login.
- Mapear endpoints de senha.
- Mapear endpoints de dashboard e ponto.
- Mapear endpoints de empresas.
- Mapear endpoints de colaboradores.
- Mapear endpoints de usuários de acesso.
- Mapear endpoints de avisos.
- Mapear endpoints de documentos.

## Saída

Tabela com:

| Área | Endpoint | Método | DTO request | DTO response | Observação |
|---|---|---|---|---|---|

---

# 03 — Access Users Support Agent

## Missão

Responder se o back-end possui suporte real para a tela de usuários de acesso prevista na correção do EPIC 6.

## Responsabilidades

- Procurar controller relacionado a usuários.
- Procurar serviços de usuários e roles.
- Verificar se há listagem de usuários existentes.
- Verificar se há status de usuário.
- Verificar vínculo com colaborador.
- Verificar ativação/desativação.
- Verificar alteração de role.
- Verificar último acesso, se existir.

## Proibição

Não criar contrato novo. Apenas mapear o que existe e registrar bloqueio se necessário.

## Saída

```text
ACCESS_USERS_SUPPORT:
- listagem: sim/nao
- detalhe: sim/nao
- role: sim/nao
- status: sim/nao
- colaborador vinculado: sim/nao
- ativar/desativar: sim/nao
- alterar role: sim/nao
- ultimo acesso: sim/nao
- bloqueio para front:
```

---

# 04 — Auth Contract Agent

## Missão

Validar contratos de autenticação pública e fluxos de senha.

## Responsabilidades

- Verificar login por senha.
- Verificar login facial.
- Verificar recuperação de senha.
- Verificar redefinição de senha.
- Verificar token inválido.
- Verificar retorno neutro.
- Verificar formato de erro.

## Saída

Relatório de compatibilidade dos fluxos públicos.

---

# 05 — Dashboard Check-in Contract Agent

## Missão

Validar contratos de dashboard, status do dia e registro de ponto.

## Responsabilidades

- Verificar status do dia.
- Verificar check-in.
- Verificar payload de localização.
- Verificar payload de biometria ou face, se aplicável.
- Verificar retorno de sucesso.
- Verificar retorno de erro.
- Verificar pendência de termo.
- Verificar possibilidade de retry.

## Saída

Relatório sobre suporte aos gaps do EPIC 5.

---

# 06 — Admin Management Contract Agent

## Missão

Validar contratos de empresas e colaboradores.

## Responsabilidades

- Verificar listagem de empresas.
- Verificar criação e atualização.
- Verificar geolocalização.
- Verificar listagem de colaboradores.
- Verificar criação e edição.
- Verificar ativação/desativação.
- Verificar validações de CPF e username.

## Saída

Relatório sobre suporte aos gaps do EPIC 6.

---

# 07 — Communication Documents Contract Agent

## Missão

Validar contratos de avisos e documentos.

## Responsabilidades

- Verificar listagem de avisos.
- Verificar criação, detalhe e exclusão de aviso.
- Verificar prioridades.
- Verificar permissões.
- Verificar listagem de documentos.
- Verificar upload.
- Verificar download.
- Verificar exclusão.
- Verificar payloads de erro.

## Saída

Relatório sobre suporte aos gaps do EPIC 7.

---

# 08 — Build Test Validation Agent

## Missão

Executar ou orientar as validações técnicas do back-end.

## Responsabilidades

- Executar `./gradlew test` quando possível.
- Executar `./gradlew build` quando possível.
- Registrar falhas.
- Classificar se falha tem relação com gap closure.

## Saída

```text
VALIDATION_RESULT:
- ./gradlew test: passou/falhou/nao executado
- ./gradlew build: passou/falhou/nao executado
- causa das falhas:
- recomendação:
```

---

# 09 — Final Reviewer Agent

## Missão

Consolidar os achados de todos os sub-agentes e produzir decisão final.

## Responsabilidades

- Verificar se todos os relatórios foram produzidos.
- Confirmar se há bloqueios para o front.
- Confirmar se usuários de acesso têm suporte.
- Confirmar se testes foram executados.
- Confirmar se não houve mudança indevida de regra.

## Saída final

```text
BACKEND GAP CLOSURE FINAL REVIEW

STATUS:
CONTRATOS OK:
CONTRATOS COM RISCO:
BLOQUEIOS PARA FRONT:
USUARIOS DE ACESSO:
VALIDACOES:
RECOMENDACAO FINAL:
```

---

## Regra comum para todos os sub-agentes

Todos devem parar ao encontrar risco alto ou bloqueio contratual. A decisão de alterar código deve ser tomada separadamente e de forma explícita.
