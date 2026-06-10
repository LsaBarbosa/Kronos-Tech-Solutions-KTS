# VALIDATION — Back-end compatibility — ÉPICO 1 new-ui

## 1. Pré-check

```bash
git branch --show-current
git status --short
java -version
./gradlew --version
```

Esperado:

```text
branch = new-ui
Java = 21
```

## 2. Build e testes

Executar:

```bash
./gradlew test
./gradlew bootJar
```

Se `test` falhar por Docker/Testcontainers indisponível, registrar e executar:

```bash
./gradlew bootJar -x test
```

## 3. Health local

Se a aplicação for iniciada localmente:

```bash
curl -i http://localhost:8080/actuator/health
```

## 4. Checklist de compatibilidade

| Item | Status | Observação |
|---|---|---|
| Branch `new-ui` confirmada | OK/NOK | |
| Build Gradle executado | OK/NOK | |
| Testes executados | OK/NOK/NOT RUN | |
| Contratos principais preservados | OK/NOK | |
| Nenhum endpoint visual criado | OK/NOK | |
| Nenhum secret alterado | OK/NOK | |

## 5. Modelo de resultado

Criar:

```text
docs/new-ui/epic-01-backend/VALIDATION_RESULT.md
```

Formato:

```md
# Resultado de Validação — Back-end — ÉPICO 1 new-ui

## Ambiente

- Branch:
- Java:
- Gradle:

## Comandos

| Comando | Resultado | Observação |
|---|---|---|
| ./gradlew test | PASS/FAIL/NOT RUN | |
| ./gradlew bootJar | PASS/FAIL/NOT RUN | |
| ./gradlew bootJar -x test | PASS/FAIL/NOT RUN | |

## Compatibilidade com front-end

- Endpoints preservados:
- Incompatibilidades encontradas:
- Mudanças necessárias no back-end:

## Conclusão

- APTO / NÃO APTO
```
