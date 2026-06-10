# SPEC — Back-end compatibility — ÉPICO 1 new-ui

## 1. Objetivo

Orientar o Claude Code no repositório back-end durante a correção do ÉPICO 1 da branch `new-ui`.

O ÉPICO 1 trata de design system no front-end. Portanto, o back-end deve ser usado apenas para:

- validar contratos consumidos pelo front-end;
- garantir que a branch `new-ui` compila;
- evitar mudanças desnecessárias;
- registrar incompatibilidades reais, se existirem.

## 2. Repositório e branch

```text
Repositório: LsaBarbosa/Kronos-Tech-Solutions-KTS
Branch: new-ui
```

## 3. Stack detectada

- Java 21.
- Spring Boot 3.5.x.
- Gradle.
- JPA/Flyway/PostgreSQL.
- Spring Security.
- Actuator.
- Testcontainers.

## 4. Papel do back-end no ÉPICO 1

### Deve fazer

- Confirmar que endpoints usados pelo front-end existem.
- Confirmar que a aplicação compila.
- Rodar testes quando ambiente permitir.
- Registrar resultado de validação.
- Informar incompatibilidade real caso o front-end precise de contrato inexistente.

### Não deve fazer

- Criar endpoint novo para resolver problema de cor, tipografia ou layout.
- Alterar DTO por ajuste visual.
- Alterar autenticação sem evidência de quebra.
- Alterar CORS/cookies sem evidência de problema de integração.
- Criar migration sem necessidade funcional real.

## 5. Endpoints principais a preservar

Com base nos fluxos já mapeados do Kronos, preservar contratos de:

```text
/auth/login
/auth/login-face
/auth/recover-password
/auth/reset-password
/companies
/companies/{cnpj}
/companies/check-cnpj
/employee
/employee/{employeeId}
/employee/own-profile
/employee/check-cpf
/users
/users/own-profile
/users/check-username
/documents
/documents/{documentId}
/messages
/records/*
/terms/*
/legal/*
/actuator/health
```

## 6. Interação com o front-end

Se durante a execução do front-end for detectado que uma tela depende de endpoint inexistente:

1. confirmar no back-end;
2. procurar endpoint equivalente;
3. preferir ajustar o front-end para o contrato real;
4. só propor alteração no back-end se não existir contrato equivalente;
5. registrar em `VALIDATION_RESULT.md`.

## 7. Validação

Executar:

```bash
git branch --show-current
git status --short
./gradlew test
./gradlew bootJar
```

Se `./gradlew test` falhar por Docker/Testcontainers indisponível, tentar pelo menos:

```bash
./gradlew bootJar -x test
```

Registrar tudo em:

```text
docs/new-ui/epic-01-backend/VALIDATION_RESULT.md
```

## 8. Critério de pronto

Back-end está pronto para o ÉPICO 1 quando:

- branch `new-ui` validada;
- build executado ou impedimento registrado;
- nenhuma mudança funcional desnecessária foi feita;
- contratos consumidos pelo front-end não foram quebrados.
