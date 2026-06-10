# RULES — Back-end compatibility — ÉPICO 1 new-ui

## 1. Branch

Antes de qualquer alteração:

```bash
git branch --show-current
git status --short
```

A branch deve ser:

```text
new-ui
```

Se não for, parar.

## 2. Escopo permitido

Permitido:

- documentação em `docs/new-ui/epic-01-backend/*`;
- testes de compatibilidade se estritamente necessários;
- correção mínima de build se a branch estiver quebrada.

Não permitido sem autorização explícita:

- alteração de endpoints;
- alteração de DTO;
- alteração de segurança;
- alteração de CORS/cookies;
- criação de migration;
- alteração de regra de negócio;
- alteração de integrações externas.

## 3. Regra de compatibilidade

O front-end deve se adaptar ao contrato real do back-end sempre que houver endpoint equivalente.

Só considerar mudança no back-end quando:

- o contrato necessário não existir;
- o comportamento for requisito funcional real;
- a mudança não for apenas visual.

## 4. Regras de validação

Executar:

```bash
./gradlew test
./gradlew bootJar
```

Fallback permitido:

```bash
./gradlew bootJar -x test
```

Registrar motivo se testes não rodarem.

## 5. Secrets

Nunca commitar:

```text
.env
application-prod.yml com segredo real
chaves privadas
tokens
senhas
logs com credenciais
```

## 6. Resultado obrigatório

Criar ou atualizar:

```text
docs/new-ui/epic-01-backend/VALIDATION_RESULT.md
```

com:

- branch;
- comandos executados;
- resultado;
- impedimentos;
- impacto no front-end.
