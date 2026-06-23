# Mapa de leitura obrigatória dos repositórios

## Back-end — `Kronos-Tech-Solutions-KTS` branch `homolog`

Ler primeiro:

```text
README.md
build.gradle
src/main/resources/application.yml
src/main/resources/application-prod.yml
src/main/resources/application-test.yml
src/main/resources/db/migration/*
```

Localizar e ler arquivos reais relacionados a:

| Tema | Busca |
|---|---|
| Empresa | `Company`, `CompanyRepository`, `companyId`, `company_id` |
| Usuário | `User`, `UserRepository`, `username`, `PasswordEncoder` |
| Papel | `Role`, `MANAGER`, `CTO`, `PARTNER` |
| Colaborador | `Employee`, `EmployeeRepository` |
| Documentos | `Document`, `DocumentType`, `BIOMETRIC_CONSENT_TERM` |
| Storage | `Storage`, `StorageService`, `S3`, `bucket`, `FileStorage` |
| Biometria | `Rekognition`, `face`, `biometric`, `consent` |
| Ponto | `PointRecord`, `Registro`, `Ponto`, `TimeRecord` |
| Férias | `Vacation`, `Ferias`, `Férias`, `TimeOff` |
| Abono | `Abono`, `Allowance`, `Manual` |
| Ajuste | `Adjustment`, `ManualRegistration`, `RegisterAdjustment` |
| Segurança | `SecurityFilterChain`, `PreAuthorize`, `Jwt`, `Cookie` |
| Sessão | `Refresh`, `Session`, `Redis`, `Cache`, `RateLimit` |
| Integração | `Mail`, `Webhook`, `WhatsApp`, `Here`, `AWS` |

## Front-end — `Kronos-Tech-Solution-User-Plataform` branch `homolog`

Ler primeiro:

```text
README.md
package.json
src/App.tsx
src/config/app-routes.ts
```

Localizar e ler:

| Tema | Busca |
|---|---|
| Rotas | `APP_PATHS`, `APP_ROUTE_META` |
| Guards | `ProtectedRoute`, `RoleRoute` |
| Auth | `AuthContext`, `user.role`, `activeCompany` |
| HTTP | `axios`, `apiClient`, `service` |
| Administração | `Administracao`, `Empresa`, `Dashboard` |
| UI | `Button`, `Card`, `Dialog`, `AlertDialog`, `toast` |
| Testes | `vitest`, `testing-library`, `msw` |

## Documentação — `kronos-business` branch `main`

Ler:

```text
README.md
01-visao-negocio.md
03-atores-permissoes.md
04-fluxos-aplicacao.md
05-entradas-saidas-fluxos.md
06-contratos-api.md
10-regras-negocio.md
11-autenticacao-seguranca.md
12-autorizacao-permissoes.md
13-integracoes-externas.md
14-banco-dados-migrations.md
15-lgpd-privacidade.md
17-ambientes-variaveis.md
18-deploy.md
19-troubleshooting.md
ADR-*.md
```

## Comandos de leitura

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
git switch homolog
rg -n "Company|User|Employee|DocumentType|BIOMETRIC_CONSENT_TERM|Vacation|Abono|Manual|PointRecord|Storage|S3|Rekognition|Jwt|Refresh|Redis|RateLimit" src/main/java src/main/resources

cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
git switch homolog
rg -n "APP_PATHS|APP_ROUTE_META|RoleRoute|ProtectedRoute|AuthContext|Administracao|axios|useMutation|toast" src

cd /home/deploy/apps/kronos-business
git switch main
rg -n "CTO|MANAGER|empresa|documento|ponto|férias|abono|LGPD|storage|AWS|Rekognition|sessão|token|cache" .
```
