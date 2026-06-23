# Subagent — Backend Domain Mapping

## Missão

Mapear o domínio real do backend antes de qualquer alteração.

## Saída obrigatória

Criar no resumo do Claude uma tabela com:

| Área | Entidade/Service/Repository real | Arquivo | Observações |
|---|---|---|---|
| Company | | | |
| User | | | |
| Employee | | | |
| Role/Auth | | | |
| Document | | | |
| DocumentType | | | |
| Storage | | | |
| S3 | | | |
| Rekognition | | | |
| Ponto | | | |
| Férias | | | |
| Abono | | | |
| Ajuste de registro | | | |
| Refresh/session | | | |
| Redis/cache | | | |
| Rate-limit | | | |
| Flyway | | | |

## Comandos

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS

find src/main/java -type f | sort | sed -n '1,240p'
find src/main/resources -type f | sort | sed -n '1,240p'

rg -n "class Company|@Entity.*Company|CompanyRepository|companyId|company_id" src/main/java src/main/resources
rg -n "class User|@Entity.*User|UserRepository|username|PasswordEncoder|Role|MANAGER|CTO|PARTNER" src/main/java src/main/resources
rg -n "class Employee|EmployeeRepository|employeeId|employee_id" src/main/java src/main/resources
rg -n "DocumentType|Document|BIOMETRIC_CONSENT_TERM|Storage|S3|bucket|Rekognition|face|biometric" src/main/java src/main/resources
rg -n "Vacation|Ferias|Férias|TimeOff|Abono|Manual|Adjustment|PointRecord|Registro|Ponto" src/main/java src/main/resources
rg -n "Jwt|JWT|Refresh|Session|Cookie|Redis|Cache|RateLimit|Permission|SecurityFilterChain|PreAuthorize" src/main/java src/main/resources
```

## Regras

- Não alterar arquivos.
- Não criar código.
- Não assumir nomes de tabelas.
- Não inferir tipos de documentos sem localizar enum/tabela real.
