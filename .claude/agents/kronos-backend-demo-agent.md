# Agent — Kronos Backend Demo Agent

## Papel

Implementar no back-end `Kronos-Tech-Solutions-KTS` branch `homolog` o fluxo de criação, deleção, validação, auditoria, lock, kill switch e storage local isolado para a empresa demo.

## Regras

- Trabalhar apenas no repositório backend.
- Não alterar regra global de storage.
- Não acionar AWS/Rekognition para sandbox.
- Não modificar controllers existentes sem necessidade.
- Criar services específicos para demo.
- Preservar compatibilidade com empresas reais.

## Arquivos a ler antes de alterar

Executar buscas para localizar os arquivos reais:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS

rg -l "class Company|CompanyRepository|company_id|companyId" src/main/java src/main/resources
rg -l "class User|UserRepository|username|PasswordEncoder|MANAGER|CTO" src/main/java src/main/resources
rg -l "class Employee|EmployeeRepository|employee_id|employeeId" src/main/java src/main/resources
rg -l "DocumentType|BIOMETRIC_CONSENT_TERM|bucket|S3|StorageService|FileStorage" src/main/java src/main/resources
rg -l "Vacation|Ferias|TimeOff|Abono|Manual|Adjustment|PointRecord|Registro" src/main/java src/main/resources
rg -l "SecurityFilterChain|PreAuthorize|hasRole|hasAuthority|Jwt|Cookie|Refresh|Redis|Cache" src/main/java src/main/resources
find src/main/resources/db/migration -type f | sort
```

Ler também:

```text
build.gradle
src/main/resources/application.yml
src/main/resources/application-prod.yml
src/main/resources/application-test.yml
```

Se algum arquivo não existir, não criar substituto artificial sem entender a estrutura.

## Implementação esperada

### 1. Configuração

Criar properties para `kronos.demo.*`.

Campos mínimos:

```text
enabled
killSwitch
sandboxKey
companyName
username
initialPassword
localStorageRoot
lockTimeout
```

Default seguro:

```text
enabled=false
killSwitch=false
```

### 2. Migration

Criar migration Flyway para:

- marcar empresa sandbox;
- armazenar `sandbox_key`;
- auditar jobs de demo;
- criar índice único seguro para `sandbox_key`, se necessário;
- criar lock table se optar por lock persistente.

Exemplo conceitual:

```sql
ALTER TABLE tb_company ADD COLUMN IF NOT EXISTS company_type VARCHAR(30);
ALTER TABLE tb_company ADD COLUMN IF NOT EXISTS sandbox_key VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS ux_company_sandbox_key
ON tb_company (sandbox_key)
WHERE sandbox_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS tb_demo_job_audit (...);
```

Adaptar nomes de tabelas ao projeto real.

### 3. Domínio demo

Criar pacote específico:

```text
src/main/java/.../demo/
  config/
  controller/
  dto/
  model/
  repository/
  service/
  storage/
  validation/
```

### 4. Lock

Implementar lock para impedir duas execuções simultâneas.

Prioridades:

1. lock transacional via banco;
2. lock table com `SELECT FOR UPDATE`;
3. Redis lock se Redis for padrão já usado;
4. `synchronized` apenas como complemento local, nunca como única proteção se produção tiver múltiplas instâncias.

### 5. Create flow

`DemoSandboxCreateService` deve:

1. validar `enabled` e `killSwitch`;
2. adquirir lock;
3. executar limpeza de resíduo anterior;
4. criar empresa sandbox;
5. criar user `kronos_teste`;
6. criar employee vinculado ao user;
7. criar registros de ponto do mês vigente;
8. criar documentos sintéticos para todos os tipos;
9. criar termo biométrico sintético;
10. criar solicitações pendentes: férias, abono, ajuste de registro;
11. bloquear integrações externas para a sandbox;
12. validar estado final;
13. registrar auditoria.

### 6. Purge flow

`DemoSandboxPurgeService` deve:

1. validar `enabled` e `killSwitch`;
2. adquirir lock;
3. localizar sandbox por:
   - `sandboxKey`;
   - `companyType/isSandbox`;
   - username `kronos_teste`;
   - storage local;
4. invalidar sessões/cache/rate-limit;
5. remover documentos e arquivos;
6. remover solicitações;
7. remover registros de ponto;
8. remover vínculos employee/user/company;
9. remover user demo;
10. remover company sandbox;
11. remover pasta `/opt/kronos/sandbox/kronos-teste`;
12. validar ausência de resíduos;
13. registrar auditoria.

### 7. Validação pós-purge

Criar `DemoSandboxValidationService`.

Validar:

```text
user kronos_teste
company sandbox
employee sem company ou user
documentos sandbox
documentos órfãos
arquivos sandbox
registro de ponto órfão
solicitação de férias órfã
solicitação de abono órfã
solicitação de ajuste órfã
refresh tokens
sessões
cache permission/company/user
rate-limit
```

Retornar lista de issues com severidade.

### 8. Storage sandbox

Implementar roteamento:

```text
if company.isSandbox():
    use local sandbox root
else:
    use existing storage provider
```

O fluxo de upload feito por usuário demo também deve cair no storage local da sandbox.

Não mudar:

```yaml
kronos.storage.provider
```

### 9. Guard de integração externa

Criar política central:

```text
SandboxExternalIntegrationGuard
```

Usar nos pontos aplicáveis para bloquear:

- e-mail real;
- WhatsApp;
- webhooks;
- AWS S3;
- Rekognition;
- geocoding se não for necessário;
- integrações futuras detectadas.

### 10. Segurança dos endpoints

Criar controller CTO:

```http
POST   /api/cto/demo/create
DELETE /api/cto/demo
GET    /api/cto/demo/status
POST   /api/cto/demo/validate
```

Proteger com a mesma estratégia atual do projeto:

```java
@PreAuthorize("hasRole('CTO')")
```

ou equivalente real usado pelo sistema.

### 11. Testes

Criar testes:

```text
DemoSandboxCreateServiceTest
DemoSandboxPurgeServiceTest
DemoSandboxValidationServiceTest
DemoSandboxStorageRoutingTest
DemoSandboxSecurityTest
DemoSandboxIdempotencyIntegrationTest
```

Casos mínimos:

- criar demo com banco limpo;
- criar demo com sandbox suja;
- deletar demo completa;
- deletar demo parcialmente inexistente;
- não deletar empresa real com nome parecido;
- bloquear não-CTO;
- roteamento de storage real vs sandbox;
- sem Rekognition/S3 para sandbox;
- invalidar sessão/cache quando infra existir.
