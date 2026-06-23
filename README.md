# Kronos — Pacote Claude Code para fluxo CTO de CRIAR DEMO / DELETAR DEMO

## Objetivo

Implementar, revisar e validar o fluxo controlado por usuário `CTO` para criar e deletar uma empresa sandbox/demo chamada `Kronos Teste`.

O fluxo deve criar dados sintéticos completos para demonstração e permitir purge idempotente sem deixar resíduos de banco, storage local, sessão, cache, permissões ou arquivos.

## Repositórios e branches obrigatórias

| Camada | Repositório | Branch |
|---|---|---|
| Back-end | `Kronos-Tech-Solutions-KTS` | `homolog` |
| Front-end | `Kronos-Tech-Solution-User-Plataform` | `homolog` |
| Documentação | `kronos-business` | `main` |

## Estrutura do pacote

```text
.claude/
  rules/
    kronos-demo-sandbox.rules.md
  skills/
    kronos-demo-sandbox/
      SKILL.md
  agents/
    kronos-demo-sandbox-architect.md
    kronos-backend-demo-agent.md
    kronos-frontend-demo-agent.md
    kronos-qa-security-agent.md
  subagents/
    backend-domain-mapping-subagent.md
    sandbox-storage-purge-subagent.md
    frontend-cto-ui-subagent.md
    validation-test-subagent.md
    documentation-contract-subagent.md
  commands/
    implement-kronos-demo-sandbox.md
docs_index/
  REPOSITORY_READING_MAP.md
  DEMO_SANDBOX_CONTRACT.md
plan/
  IMPLEMENTATION_PLAN_CTO_DEMO_SANDBOX.md
prompts/
  CLAUDE_CODE_MASTER_PROMPT.md
  CODEX_COMPATIBLE_PROMPT.md
scripts/
  install_into_repos.sh
```

## Como usar na VPS

1. Copie este pacote para `/home/deploy/apps`.
2. Execute:

```bash
cd /home/deploy/apps
bash kronos-demo-sandbox-claude-package/scripts/install_into_repos.sh
```

3. Entre no back-end:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
git switch homolog
claude
```

4. No Claude Code, cole o conteúdo de:

```text
prompts/CLAUDE_CODE_MASTER_PROMPT.md
```

## Diretriz principal

A implementação deve ser feita primeiro no back-end, depois no front-end, e por fim na documentação. O front-end não deve inventar contrato. Ele só deve consumir endpoints implementados e testados no back-end.
