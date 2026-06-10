# Slash Command — validate-new-ui-backend-compat

Use como comando manual no Claude Code:

```text
Valide o back-end Kronos-Tech-Solutions-KTS na branch new-ui para compatibilidade com o ÉPICO 1 do front-end.

Leia CLAUDE.md, docs/new-ui/epic-01-backend/SPEC.md, RULES.md, VALIDATION.md e .claude/skills/new-ui-backend-compat/SKILL.md.

Execute:
- git branch --show-current
- git status --short
- java -version
- ./gradlew --version
- ./gradlew test
- ./gradlew bootJar

Se testes falharem por Docker/Testcontainers, execute ./gradlew bootJar -x test e registre o motivo.

Crie/atualize docs/new-ui/epic-01-backend/VALIDATION_RESULT.md.
Não altere endpoints, DTOs, segurança, CORS, cookies ou migrations sem autorização explícita.
```
