# Skill — Kronos ÉPICO 2 Back-end Validation

## Quando usar

Use esta skill na task de validação do back-end para o ÉPICO 2.

## Objetivo

Confirmar que o App Shell responsivo do front-end não exige alteração no back-end.

## Regras

- Não alterar controllers.
- Não alterar DTOs.
- Não alterar endpoints.
- Não alterar entities.
- Não criar migrations.
- Não alterar regras de negócio.

## Validação

```bash
./gradlew test
./gradlew build
```

## Saída esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 2.
```
