# PROMPTS — Claude Code — Back-end compatibility — ÉPICO 1 new-ui

## Prompt principal

```text
Você está no repositório Kronos-Tech-Solutions-KTS, branch new-ui.

Leia:
- CLAUDE.md
- docs/new-ui/epic-01-backend/SPEC.md
- docs/new-ui/epic-01-backend/RULES.md
- docs/new-ui/epic-01-backend/VALIDATION.md
- .claude/skills/new-ui-backend-compat/SKILL.md
- .claude/agents/new-ui-backend-compat-agent.md

Objetivo:
Validar compatibilidade do back-end para o ÉPICO 1 new-ui.

Regras:
- Não implemente design system no back-end.
- Não altere endpoints sem necessidade comprovada.
- Não altere DTO, segurança, CORS, cookies ou migrations sem autorização explícita.
- Rode validação Gradle quando possível.
- Registre resultado em docs/new-ui/epic-01-backend/VALIDATION_RESULT.md.
```

## Prompt de validação rápida

```text
Valide a branch new-ui do back-end para compatibilidade com o front-end.

Execute:
- git branch --show-current
- git status --short
- ./gradlew test
- ./gradlew bootJar

Se testes falharem por Docker/Testcontainers, rode ./gradlew bootJar -x test e documente.
Atualize docs/new-ui/epic-01-backend/VALIDATION_RESULT.md.
```

## Prompt para investigar endpoint

```text
Investigue se o endpoint informado pelo front-end existe no back-end da branch new-ui.

Não altere código inicialmente.
Procure controller, request DTO, response DTO e testes.
Se existir endpoint equivalente, documente o contrato real.
Se não existir, registre incompatibilidade em docs/new-ui/epic-01-backend/VALIDATION_RESULT.md e aguarde decisão antes de alterar backend.
```
