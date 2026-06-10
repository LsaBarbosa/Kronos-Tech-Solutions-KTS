# Claude Code — Kronos Back-end

## Escopo obrigatório

Repositório:

```text
LsaBarbosa/Kronos-Tech-Solutions-KTS
```

Branch obrigatória:

```text
new-ui
```

## Objetivo nesta frente

O ÉPICO 1 é essencialmente de front-end/design system. No back-end, o papel do Claude Code é **compatibilidade e validação**, não implementação visual.

## Ordem obrigatória de leitura

1. `docs/new-ui/epic-01-backend/SPEC.md`
2. `docs/new-ui/epic-01-backend/RULES.md`
3. `docs/new-ui/epic-01-backend/VALIDATION.md`
4. `docs/new-ui/epic-01-backend/PROMPTS.md`
5. `.claude/skills/new-ui-backend-compat/SKILL.md`
6. `.claude/agents/new-ui-backend-compat-agent.md`

## Regras fortes

- Trabalhar somente na branch `new-ui`.
- Não alterar endpoints para resolver problema visual do front-end.
- Não modificar regra de negócio sem necessidade comprovada.
- Não criar migrations para o ÉPICO 1, salvo se uma incompatibilidade real for descoberta e aprovada.
- Não alterar segurança, CORS, cookies ou autenticação sem evidência concreta.
- Não commitar secrets, `.env`, logs ou artefatos de build.

## Validação mínima

Executar quando possível:

```bash
./gradlew test
./gradlew bootJar
```

Se o projeto exigir Docker/Testcontainers e o ambiente não tiver suporte, registrar em:

```text
docs/new-ui/epic-01-backend/VALIDATION_RESULT.md
```
