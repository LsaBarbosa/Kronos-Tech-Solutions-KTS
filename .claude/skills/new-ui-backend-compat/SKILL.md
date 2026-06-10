# SKILL — Validar back-end para ÉPICO 1 new-ui

## Nome

`new-ui-backend-compat`

## Objetivo

Validar que o back-end na branch `new-ui` não impede a conclusão do ÉPICO 1 do front-end.

## Quando usar

Use quando o trabalho do front-end `new-ui` exigir confirmação de endpoints, build ou compatibilidade com a API.

## Procedimento

### Passo 1 — Confirmar branch

```bash
git branch --show-current
git status --short
```

Parar se a branch não for `new-ui`.

### Passo 2 — Confirmar ambiente

```bash
java -version
./gradlew --version
```

Esperado: Java 21.

### Passo 3 — Build/test

```bash
./gradlew test
./gradlew bootJar
```

Fallback:

```bash
./gradlew bootJar -x test
```

### Passo 4 — Investigar contrato, se necessário

Para endpoint específico:

```bash
rg "@GetMapping|@PostMapping|@PatchMapping|@PutMapping|@DeleteMapping" src/main/java
rg "<endpoint-ou-trecho>" src/main/java src/test/java
```

### Passo 5 — Registrar resultado

Criar ou atualizar:

```text
docs/new-ui/epic-01-backend/VALIDATION_RESULT.md
```

## Proibições

- Não alterar regra de negócio por ajuste visual.
- Não criar endpoint sem decisão explícita.
- Não alterar segurança, CORS, cookies ou migrations sem evidência.
- Não commitar secrets.

## Saída esperada

- Resultado de build/test.
- Registro de compatibilidade.
- Lista de incompatibilidades reais, se houver.
