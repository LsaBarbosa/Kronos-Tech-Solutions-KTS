# SPEC — Backlog de Correção das Pendências LGPD Kronos

**Projeto:** Kronos  
**Branches-alvo:**
- Back-end: `feature/lgpd-compliance`
- Front-end: `feature/lgpd-compliance`

**Documento:** backlog técnico em modelo de especificação  
**Objetivo:** corrigir pendências remanescentes da auditoria LGPD sem alterar o comportamento atual de liveness.  
**Importante:** `liveness` permanecerá **não obrigatório**. Não criar tarefa, validação, config ou regra que torne `liveness` obrigatório em produção.

---

# 1. Escopo

Este backlog cobre exclusivamente as pendências listadas abaixo:

1. Retenção ainda não cobre todos os `RetentionResourceType` declarados.
2. Scheduler de retenção vem desligado em produção.
3. Anonimização de registros de ponto ainda é simplificada demais.
4. Dry-run de anonimização pode subestimar impacto.
5. Falhas parciais na anonimização podem não bloquear conclusão.
6. Inventário LGPD pode ter inconsistência de prefixo `/api`.
7. Exportação no front ainda deveria ter confirmação explícita.
8. Fluxo de incidentes precisa validar prazo/evidência de comunicação.
9. Testes/CI não foram comprovados na auditoria.

---

# 2. Fora de escopo obrigatório

## 2.1 Liveness

Não modificar o estado atual do `liveness`.

### Regra

```text
NÃO tornar liveness obrigatório.
NÃO alterar default de liveness.
NÃO bloquear produção caso liveness esteja false.
NÃO criar validação em ProductionConfigValidator para exigir liveness.
NÃO alterar aplicação para exigir liveness em check-in, login facial ou cadastro biométrico além do comportamento atual.
```

### Justificativa

O projeto decidiu manter `liveness` como não obrigatório neste momento. As correções deste backlog devem respeitar essa decisão.

---

# 3. Definition of Done geral

Uma task só pode ser considerada concluída quando:

- código implementado;
- testes unitários criados ou ajustados;
- testes de integração criados ou ajustados quando houver API;
- front-end ajustado quando houver impacto de tela;
- contrato front/back validado quando houver rota;
- logs de auditoria/execução revisados;
- documentação técnica atualizada;
- CI executado com sucesso;
- nenhum ajuste altera o comportamento atual do `liveness`.

---

# 4. Sprints

---

# Sprint LGPD-CORR-01 — Cobertura completa de retenção

## Objetivo

Garantir que todos os valores declarados em `RetentionResourceType` tenham processador explícito, comportamento conhecido, teste e log de execução.

## Pendência relacionada

> Retenção ainda não cobre todos os `RetentionResourceType` declarados.

## Tipos declarados

```java
BLACKLISTED_TOKEN,
PASSWORD_RESET_TOKEN,
MESSAGE,
DOCUMENT,
AUDIT_LOG,
LEGAL_CONSENT,
BIOMETRIC_ARTIFACT,
LGPD_REQUEST
```

---

## Task LGPD-CORR-01-01 — Criar matriz de cobertura de retenção

**Prioridade:** P0  
**Tipo:** Back-end / Documentação técnica  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

O enum declara vários tipos de retenção, mas nem todos possuem processador dedicado e testado.

### O que fazer

Criar documento:

```text
docs/legal/retention-resource-coverage.md
```

Com a matriz:

| ResourceType | Processor | Ação DRY_RUN | Ação APPLY | Preserva dado legal? | Testado? |
|---|---|---|---|---|---|
| BLACKLISTED_TOKEN | BlacklistedTokenRetentionProcessor | contar expirados | deletar expirados | não aplicável | sim |
| PASSWORD_RESET_TOKEN | PasswordResetTokenRetentionProcessor | contar expirados | deletar expirados | não aplicável | sim |
| MESSAGE | MessageRetentionProcessor | contar elegíveis | anonimizar/deletar | depende | sim |
| DOCUMENT | DocumentRetentionProcessor | contar removíveis/preservados | deletar storage + marcar DB | sim | sim |
| AUDIT_LOG | AuditLogRetentionProcessor | contar elegíveis | anonimizar detalhes | sim | sim |
| LEGAL_CONSENT | LegalConsentRetentionProcessor | contar elegíveis | preservar evidência mínima / expurgar excesso | sim | sim |
| BIOMETRIC_ARTIFACT | BiometricArtifactRetentionProcessor | contar órfãos/revogados | deletar S3/Rekognition | não | sim |
| LGPD_REQUEST | LgpdRequestRetentionProcessor | contar antigas | preservar registro mínimo | sim | sim |

### Critérios de aceite

- Documento criado.
- Todos os `RetentionResourceType` aparecem na matriz.
- Cada tipo possui decisão explícita.
- Tipos sem exclusão física possuem justificativa de preservação.

---

## Task LGPD-CORR-01-02 — Separar retenção de token em dois processadores

**Prioridade:** P0  
**Tipo:** Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

O processador atual de tokens suporta `BLACKLISTED_TOKEN`, mas também apaga `PASSWORD_RESET_TOKEN` internamente. Isso cria ambiguidade: uma política com `PASSWORD_RESET_TOKEN` pode não encontrar processor dedicado.

### O que fazer

Criar dois processadores:

```text
BlacklistedTokenRetentionProcessor
PasswordResetTokenRetentionProcessor
```

### Regras

#### BLACKLISTED_TOKEN

- `DRY_RUN`: contar tokens expirados antes do cutoff.
- `APPLY`: deletar tokens expirados.
- Não deletar tokens ainda válidos.

#### PASSWORD_RESET_TOKEN

- `DRY_RUN`: contar tokens expirados antes do cutoff.
- `APPLY`: deletar tokens expirados.
- Não deletar tokens ainda válidos.

### Critérios de aceite

- `supports()` de cada processor retorna o tipo correto.
- `RetentionPolicyExecutor` encontra processor para os dois tipos.
- Teste garante que `PASSWORD_RESET_TOKEN` não cai em warning `retention_no_processor`.
- Teste garante que `BLACKLISTED_TOKEN` e `PASSWORD_RESET_TOKEN` são processados separadamente.

### Testes

Criar ou ajustar:

```text
BlacklistedTokenRetentionProcessorTest
PasswordResetTokenRetentionProcessorTest
RetentionPolicyExecutorTest
```

---

## Task LGPD-CORR-01-03 — Criar `AuditLogRetentionProcessor`

**Prioridade:** P0  
**Tipo:** Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

Logs de auditoria podem conter detalhes sensíveis, mas também são evidência de segurança, LGPD e operação.

### Estratégia

Não deletar todos os logs de forma cega. Implementar anonimização/sanitização de detalhes antigos.

### Regras

#### DRY_RUN

Contar audit logs com:

```text
createdAt < cutoff
```

Separar por severidade:

```text
LOW
MEDIUM
HIGH
SECURITY
LGPD
```

#### APPLY

Para logs elegíveis:

- preservar:
    - `auditLogId`;
    - `action`;
    - `createdAt`;
    - `severity`;
    - `resourceType`;
    - `resourceId`;
    - `companyId`, se necessário para auditoria;
- sanitizar:
    - IP completo;
    - user-agent completo;
    - detalhes textuais com CPF/e-mail/token;
    - latitude/longitude;
    - qualquer base64;
- marcar campo de retenção, se existir:
    - `retentionAppliedAt`;
    - `retentionPolicyCode`.

### Critérios de aceite

- Logs antigos são sanitizados, não necessariamente apagados.
- Dados sensíveis são mascarados.
- Eventos de LGPD e segurança preservam evidência mínima.
- `DRY_RUN` informa quantidade escaneada e quantidade que seria sanitizada.
- `APPLY` informa quantidade afetada.

### Testes

- Deve mascarar CPF.
- Deve mascarar e-mail.
- Deve mascarar token.
- Deve mascarar IP.
- Deve preservar ação/evento.
- Deve criar `RetentionExecutionLog`.

---

## Task LGPD-CORR-01-04 — Criar `LegalConsentRetentionProcessor`

**Prioridade:** P0  
**Tipo:** Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

Consentimentos legais não podem ser simplesmente excluídos, pois são evidência de aceite/revogação. Porém dados acessórios podem ser reduzidos após prazo.

### Estratégia

Preservar evidência mínima e sanitizar dados acessórios após prazo.

### Regras

#### Preservar

- `consentId`;
- `employeeId`, se necessário;
- `consentType`;
- `legalBasis`;
- `purpose`;
- `version`;
- `grantedAt`;
- `revokedAt`;
- `evidenceDocumentId`;
- `contentHash`.

#### Sanitizar após retenção

- IP;
- user-agent;
- qualquer metadado excessivo.

### Critérios de aceite

- Consentimento ativo não é removido.
- Consentimento revogado antigo é minimizado.
- Evidência documental permanece referenciável.
- Logs de execução registram afetados/preservados.

### Testes

- Consentimento ativo não é alterado.
- Consentimento revogado antigo é minimizado.
- Consentimento recente não é alterado.
- Execução gera log.

---

## Task LGPD-CORR-01-05 — Criar `BiometricArtifactRetentionProcessor`

**Prioridade:** P0  
**Tipo:** Back-end / Storage / Rekognition  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

Artefatos biométricos devem ser removidos quando o consentimento é revogado, quando estiverem órfãos ou quando não houver base legal/finalidade.

### Escopo

Processar:

- imagem facial em S3;
- template/face indexada no Rekognition;
- referência `faceS3ObjectKey` em `Employee`.

### Regras

#### DRY_RUN

Contar:

- colaboradores com `faceS3ObjectKey` e sem consentimento ativo;
- colaboradores com consentimento revogado;
- imagens órfãs, se houver forma segura de detectar;
- divergências S3/Rekognition/DB.

#### APPLY

Para colaborador sem consentimento ativo:

- deletar imagem no S3;
- deletar faces no Rekognition por `externalImageId`;
- limpar `faceS3ObjectKey`;
- gerar log de retenção.

### Critérios de aceite

- Não remove biometria de colaborador com consentimento ativo.
- Remove biometria de colaborador sem consentimento ativo.
- Erro parcial de S3/Rekognition retorna `PARTIAL`.
- Não vaza imagem/base64 em log.

### Testes

- Consentimento ativo preserva biometria.
- Consentimento revogado remove biometria.
- S3 falhando gera resultado parcial.
- Rekognition falhando gera resultado parcial.

---

## Task LGPD-CORR-01-06 — Criar `LgpdRequestRetentionProcessor`

**Prioridade:** P1  
**Tipo:** Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

Solicitações LGPD devem ser preservadas como evidência mínima, mas dados excessivos nas descrições/notas podem ser reduzidos após prazo.

### Estratégia

Minimizar solicitações antigas encerradas.

### Elegíveis

```text
status in COMPLETED, REJECTED, PARTIALLY_COMPLETED, CANCELLED
resolvedAt < cutoff
```

### APPLY

Preservar:

- `requestId`;
- `employeeId` ou identificador pseudonimizado;
- `companyId`;
- `requestType`;
- `status`;
- `createdAt`;
- `resolvedAt`;
- `closedReason`, se necessário.

Sanitizar:

- descrição longa;
- notas internas;
- notas públicas com dados pessoais;
- campos livres.

### Critérios de aceite

- Solicitações abertas não são alteradas.
- Solicitações encerradas antigas são minimizadas.
- Histórico mantém evento mínimo.
- Não remove prova de atendimento.

---

# Sprint LGPD-CORR-02 — Ativação controlada do scheduler de retenção

## Objetivo

Ativar retenção em produção de forma controlada, sem causar exclusão indevida.

## Pendência relacionada

> Scheduler de retenção vem desligado em produção.

---

## Task LGPD-CORR-02-01 — Alterar configuração de produção para scheduler habilitado em DRY_RUN

**Prioridade:** P0  
**Tipo:** Back-end / DevOps  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Regra

Habilitar scheduler de retenção em produção, mas garantir que as políticas iniciais estejam em `DRY_RUN`.

### O que fazer

Alterar documentação e exemplo de env de produção para:

```env
LGPD_RETENTION_SCHEDULER_ENABLED=true
```

Não alterar política para `APPLY` automaticamente.

### Critérios de aceite

- Scheduler roda em produção quando env estiver configurado.
- Políticas padrão continuam `DRY_RUN`.
- Logs indicam claramente:
    - policyCode;
    - resourceType;
    - mode;
    - scanned;
    - affected;
    - skipped;
    - errors.

---

## Task LGPD-CORR-02-02 — Criar trava de segurança para `APPLY`

**Prioridade:** P0  
**Tipo:** Segurança operacional  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

`APPLY` não pode ser ativado por engano.

### O que fazer

Adicionar flag global:

```env
LGPD_RETENTION_ALLOW_APPLY=false
```

### Regra

Se policy estiver `dryRun=false`, mas `LGPD_RETENTION_ALLOW_APPLY=false`, o executor deve:

- não executar `APPLY`;
- registrar log de bloqueio;
- gerar `RetentionExecutionLog` com status `BLOCKED`;
- não alterar dados.

### Critérios de aceite

- `APPLY` só executa quando:
    - policy `dryRun=false`;
    - `LGPD_RETENTION_ALLOW_APPLY=true`;
    - policy está ativa.
- Teste garante bloqueio quando flag estiver false.

---

## Task LGPD-CORR-02-03 — Criar relatório de execução de retenção

**Prioridade:** P1  
**Tipo:** Observabilidade / Admin  
**Back-end:** Sim  
**Front-end:** Opcional

### Endpoint sugerido

```http
GET /api/lgpd/retention/executions
GET /api/lgpd/retention/executions/{executionId}
```

### Dados

- executionId;
- policyCode;
- resourceType;
- mode;
- status;
- scannedCount;
- affectedCount;
- skippedCount;
- errorCount;
- startedAt;
- finishedAt;
- errorMessage.

### Critérios de aceite

- CTO consegue consultar execuções.
- Falhas ficam visíveis.
- Execução `BLOCKED` aparece com motivo.

---

# Sprint LGPD-CORR-03 — Anonimização de registros de ponto

## Objetivo

Corrigir a simplificação excessiva da anonimização de registros de ponto, separando preservação legal, pseudonimização e remoção de dados excessivos.

## Pendências relacionadas

- Anonimização de registros de ponto ainda é simplificada demais.
- Dry-run de anonimização pode subestimar impacto.

---

## Task LGPD-CORR-03-01 — Definir estratégia formal para `TimeRecord`

**Prioridade:** P0  
**Tipo:** Domínio / Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

O comportamento atual apenas limpa latitude/longitude, independentemente de `preserveLaborData`.

### Regra proposta

#### Quando `preserveLaborData=true`

Preservar:

- `timeRecordId`;
- datas e horários;
- status;
- NSR;
- empresa;
- vínculo mínimo com colaborador se necessário para obrigação trabalhista/fiscal.

Remover ou minimizar:

- latitude;
- longitude;
- endLatitude;
- endLongitude;
- detalhes excessivos;
- metadados de dispositivo, se houver.

#### Quando `preserveLaborData=false`

Aplicar anonimização mais forte:

- remover geolocalização;
- pseudonimizar ou desvincular `employeeId`, se juridicamente permitido;
- preservar apenas estatística/histórico mínimo quando necessário.

### Critérios de aceite

- Com `preserveLaborData=true`, dados legais de ponto são preservados.
- Com `preserveLaborData=false`, anonimização é mais forte.
- O comportamento não é igual nos dois cenários.
- Decisão fica documentada.

---

## Task LGPD-CORR-03-02 — Corrigir `TimeRecordAnonymizer.executeDryRun`

**Prioridade:** P0  
**Tipo:** Bug  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

O dry-run encontra registros, mas retorna `affectedCount=0`, fazendo o resumo parecer que nada será alterado.

### O que fazer

Alterar retorno do dry-run para separar:

```text
scannedCount = total encontrado
affectedCount = total que seria alterado
skippedCount = total preservado sem alteração
```

### Regra

Se a execução `APPLY` alteraria geolocalização de 10 registros, o dry-run deve retornar:

```text
scannedCount = 10
affectedCount = 10
skippedCount = 0
```

Se 5 forem preservados sem alteração:

```text
scannedCount = 10
affectedCount = 5
skippedCount = 5
```

### Critérios de aceite

- Dry-run mostra impacto real.
- Tela/admin não mostra zero quando existem registros afetáveis.
- Teste cobre registros com e sem geolocalização.

---

## Task LGPD-CORR-03-03 — Ajustar `AnonymizationDryRunResponse`

**Prioridade:** P0  
**Tipo:** Contrato API  
**Back-end:** Sim  
**Front-end:** Sim

### Problema

O response atual deve diferenciar escaneado, afetado, preservado e erro.

### Modelo sugerido

```json
{
  "employeeId": "uuid",
  "summary": {
    "totalScanned": 100,
    "totalAffected": 80,
    "totalSkipped": 20,
    "totalErrors": 0
  },
  "domains": [
    {
      "resourceType": "TIME_RECORD",
      "scanned": 20,
      "affected": 20,
      "skipped": 0,
      "action": "REMOVE_PRECISE_GEOLOCATION",
      "warning": "Registros trabalhistas serão preservados."
    }
  ],
  "warnings": []
}
```

### Critérios de aceite

- Front mostra impacto por domínio.
- Admin entende o que será alterado antes de confirmar.
- Response não subestima registros afetados.

---

# Sprint LGPD-CORR-04 — Controle de falhas parciais na anonimização

## Objetivo

Impedir que uma anonimização parcial seja tratada como sucesso total.

## Pendência relacionada

> Falhas parciais na anonimização podem não bloquear conclusão.

---

## Task LGPD-CORR-04-01 — Criar status consolidado de anonimização

**Prioridade:** P0  
**Tipo:** Back-end / Domínio  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Problema

O executor pode registrar erro de processador, retornar `null` e continuar a execução.

### O que fazer

Criar status consolidado:

```java
SUCCESS
PARTIAL_SUCCESS
FAILED
BLOCKED
```

### Regra

- `SUCCESS`: todos os processadores obrigatórios executaram sem erro.
- `PARTIAL_SUCCESS`: pelo menos um processador falhou, mas outros concluíram.
- `FAILED`: nenhum processador crítico concluiu.
- `BLOCKED`: execução impedida por regra de segurança.

### Critérios de aceite

- Executor nunca retorna sucesso silencioso com processador falhando.
- Resultado final inclui lista de domínios com falha.
- Falha parcial fica visível no log e na resposta.

---

## Task LGPD-CORR-04-02 — Bloquear conclusão automática de solicitação LGPD se anonimização falhar parcialmente

**Prioridade:** P0  
**Tipo:** Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Regra

Uma solicitação de anonimização/exclusão não pode ir para `COMPLETED` se a execução ficou `PARTIAL_SUCCESS` ou `FAILED`.

### Comportamento

- Se `SUCCESS`: permitir conclusão.
- Se `PARTIAL_SUCCESS`: manter solicitação em `IN_ANALYSIS` ou `WAITING_LEGAL_REVIEW`.
- Se `FAILED`: manter em `IN_ANALYSIS` e registrar erro.
- Se admin quiser concluir mesmo com parcial:
    - exigir justificativa;
    - status deve ser `PARTIALLY_COMPLETED`, não `COMPLETED`.

### Critérios de aceite

- Solicitação não é concluída como completa com falha parcial.
- Histórico registra quais domínios falharam.
- Admin vê motivo da falha.

---

## Task LGPD-CORR-04-03 — Criar tela/resumo de resultado de anonimização

**Prioridade:** P1  
**Tipo:** Front-end  
**Repositório:** `Kronos-Tech-Solution-User-Plataform`

### O que mostrar

- status final;
- domínios processados;
- escaneados;
- afetados;
- preservados;
- erros;
- mensagens;
- ação recomendada.

### Critérios de aceite

- Admin não fica sem feedback.
- Falha parcial aparece visualmente.
- Botão “Concluir solicitação” fica bloqueado se status não permitir.

---

# Sprint LGPD-CORR-05 — Inventário LGPD e prefixo `/api`

## Objetivo

Remover risco de divergência entre rotas do front-end e do back-end no inventário LGPD.

## Pendência relacionada

> Inventário LGPD pode ter inconsistência de prefixo `/api`.

---

## Task LGPD-CORR-05-01 — Padronizar prefixo das rotas LGPD

**Prioridade:** P0  
**Tipo:** Contrato front/back  
**Back-end:** Sim  
**Front-end:** Sim

### Problema

O inventário está exposto no back-end com:

```text
/api/lgpd/inventory
```

Enquanto outros endpoints LGPD podem ser montados por constantes diferentes.

### Decisão necessária

Escolher uma única convenção.

#### Opção recomendada

Back-end expõe tudo sob:

```text
/api/lgpd/**
```

Front-end `api.baseURL` deve apontar para domínio base, sem duplicar `/api`.

Exemplo:

```env
VITE_API_BASE_URL=https://api.kronostechsolutions.com
```

E o front monta:

```text
/api/lgpd/inventory
/api/lgpd/requests
```

### Tarefas

- Revisar `API_ROUTES.LGPD`.
- Revisar `buildRoute`.
- Revisar `DataProcessingInventoryController`.
- Revisar `LgpdController`.
- Garantir que inventário e solicitações usem o mesmo prefixo.
- Criar teste de contrato.

### Critérios de aceite

- Front chama exatamente a URL esperada pelo back.
- Inventário lista, cria e atualiza em ambiente integrado.
- Não há duplicidade `/api/api`.
- Não há chamada sem `/api` quando back exige `/api`.

---

## Task LGPD-CORR-05-02 — Corrigir atualização de inventário por `inventoryId` ou `processCode`

**Prioridade:** P1  
**Tipo:** Contrato API  
**Back-end:** Sim  
**Front-end:** Sim

### Problema

O front pode operar por `processCode`, enquanto o back atualiza por `inventoryId`.

### Escolher padrão

#### Padrão recomendado

- Buscar por `processCode`.
- Atualizar por `inventoryId`.

### Tarefas

- Garantir que tela de edição carregue inventário por `processCode`.
- Guardar `inventoryId` retornado.
- Enviar `PATCH /api/lgpd/inventory/{inventoryId}`.
- Ajustar types do front.

### Critérios de aceite

- Criar inventário funciona.
- Editar inventário existente funciona.
- Buscar por processCode funciona.
- Atualização não tenta enviar processCode no path errado.

---

# Sprint LGPD-CORR-06 — Confirmação explícita na exportação do titular

## Objetivo

Evitar download impulsivo de arquivo contendo dados pessoais e potencialmente sensíveis.

## Pendência relacionada

> Exportação no front ainda deveria ter confirmação explícita.

---

## Task LGPD-CORR-06-01 — Criar modal de confirmação antes da exportação

**Prioridade:** P1  
**Tipo:** Front-end / UX LGPD  
**Repositório:** `Kronos-Tech-Solution-User-Plataform`

### Tela

`PrivacyCenter`

### Comportamento atual

Usuário clica em “Exportar Meus Dados” e o download inicia diretamente.

### Novo comportamento

Ao clicar, abrir modal:

```text
Você está prestes a exportar seus dados pessoais.

O arquivo pode conter CPF, PIS, endereço, salário, documentos,
histórico de ponto, geolocalização, mensagens, logs e consentimentos.

Guarde este arquivo em local seguro e não compartilhe com terceiros.

Deseja continuar?
```

### Botões

- `Cancelar`
- `Confirmar exportação`

### Critérios de aceite

- Exportação só ocorre após confirmação.
- Modal informa que pode haver dados sensíveis.
- Botão mostra loading após confirmação.
- Erro de exportação aparece em toast.
- Download mantém nome padronizado.

---

## Task LGPD-CORR-06-02 — Exibir resumo do manifesto após exportação

**Prioridade:** P2  
**Tipo:** Front-end  
**Repositório:** `Kronos-Tech-Solution-User-Plataform`

### O que fazer

Após download, mostrar toast ou card com:

- data/hora da exportação;
- se geolocalização precisa foi incluída;
- seções exportadas;
- aviso de armazenamento seguro.

### Critérios de aceite

- Usuário entende o conteúdo exportado.
- Não exibir dados pessoais no toast.
- Apenas metadados seguros.

---

# Sprint LGPD-CORR-07 — Incidentes de segurança com prazo e evidência

## Objetivo

Garantir que incidentes com comunicação obrigatória tenham prazo, evidência e bloqueios mínimos antes do encerramento.

## Pendência relacionada

> Fluxo de incidentes precisa validar prazo/evidência de comunicação.

---

## Task LGPD-CORR-07-01 — Validar prazos quando comunicação for obrigatória

**Prioridade:** P0  
**Tipo:** Back-end / Segurança  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Regra

Se:

```text
communicationRequired = true
```

Então exigir:

```text
anpdCommunicationDeadline
subjectsCommunicationDeadline
```

### Comportamento

- Rejeitar avaliação de risco sem prazos.
- Registrar auditoria.
- Retornar erro padronizado.

### Erro sugerido

```json
{
  "code": "INCIDENT_COMMUNICATION_DEADLINE_REQUIRED",
  "message": "Prazos de comunicação à ANPD e aos titulares são obrigatórios quando a comunicação é requerida."
}
```

### Critérios de aceite

- Incidente com comunicação obrigatória não salva sem prazos.
- Incidente sem comunicação obrigatória pode salvar sem prazos.
- Testes cobrem os dois cenários.

---

## Task LGPD-CORR-07-02 — Bloquear encerramento de incidente sem evidência

**Prioridade:** P0  
**Tipo:** Back-end  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Regra

Se incidente estiver indo para status final:

```text
CLOSED
RESOLVED
```

E `communicationRequired = true`, exigir:

- `notifiedAnpdAt` ou justificativa formal;
- `notifiedSubjectsAt` ou justificativa formal;
- `evidenceLinks`;
- `correctiveActions`.

### Critérios de aceite

- Incidente com comunicação obrigatória não encerra sem evidência.
- Incidente sem comunicação obrigatória exige pelo menos plano corretivo.
- Histórico registra tentativa bloqueada.

---

## Task LGPD-CORR-07-03 — Criar alerta de prazo de incidente

**Prioridade:** P1  
**Tipo:** Back-end / Observabilidade  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### O que fazer

Criar scheduler ou métrica para incidentes:

- com `communicationRequired=true`;
- sem `notifiedAnpdAt`;
- com deadline próximo ou vencido.

### Métricas sugeridas

```text
kronos_security_incident_communication_due_total
kronos_security_incident_communication_overdue_total
```

### Critérios de aceite

- Incidente vencido gera log/metric.
- Incidente próximo do prazo aparece em métrica.
- Não há dados pessoais nas tags da métrica.

---

# Sprint LGPD-CORR-08 — Testes e CI de conformidade

## Objetivo

Comprovar por testes automatizados que as correções funcionam e não terão regressão.

## Pendência relacionada

> Testes/CI não foram comprovados nesta auditoria.

---

## Task LGPD-CORR-08-01 — Criar suíte de testes back-end LGPD P0

**Prioridade:** P0  
**Tipo:** Testes  
**Repositório:** `Kronos-Tech-Solutions-KTS`

### Testes obrigatórios

#### Biometria

- manager não cria colaborador com `faceImageBase64`;
- manager não atualiza colaborador com `faceImageBase64`;
- titular só cadastra biometria com consentimento ativo;
- titular sem consentimento recebe erro.

#### Multi-tenant

- manager A não lista solicitações da empresa B;
- manager A não acessa detalhes de solicitação da empresa B;
- manager A não exporta dados de colaborador da empresa B;
- CTO acessa dados globais.

#### Exportação

- export usa `userId`;
- export inclui manifesto;
- export sem usuário não falha;
- export de terceiro exige justificativa.

#### Retenção

- cada `RetentionResourceType` tem processor;
- `DRY_RUN` não altera dados;
- `APPLY` bloqueado quando flag global não permite;
- `APPLY` altera quando flag permite.

#### Anonimização

- dry-run mostra impacto correto;
- falha parcial retorna `PARTIAL_SUCCESS`;
- solicitação não conclui como `COMPLETED` com falha parcial.

#### Incidentes

- comunicação obrigatória exige deadlines;
- encerramento exige evidência;
- relatório só gera após avaliação de risco.

### Critérios de aceite

- Todos os testes passam localmente.
- CI executa a suíte.
- Falha em qualquer teste P0 bloqueia merge.

---

## Task LGPD-CORR-08-02 — Criar suíte front-end LGPD

**Prioridade:** P1  
**Tipo:** Testes  
**Repositório:** `Kronos-Tech-Solution-User-Plataform`

### Testes obrigatórios

- Privacy Center renderiza.
- Exportação abre modal antes do download.
- Cancelar modal não chama API.
- Confirmar modal chama API.
- Inventário usa rota correta.
- Admin vê falha parcial de anonimização.
- Admin não consegue concluir solicitação quando backend retorna bloqueio.
- Solicitações LGPD exibem status novos.

### Critérios de aceite

- `npm run test` passa.
- Testes cobrem fluxos principais.
- Não há snapshot frágil desnecessário.

---

## Task LGPD-CORR-08-03 — Atualizar pipeline de CI

**Prioridade:** P0  
**Tipo:** DevOps / CI  
**Back-end:** Sim  
**Front-end:** Sim

### Back-end CI

Executar:

```bash
./gradlew clean test
```

Gerar relatório:

```text
build/reports/tests/test/index.html
```

### Front-end CI

Executar:

```bash
npm ci
npm run lint
npm run test
npm run build
```

### Critérios de aceite

- PR falha se back-end falhar.
- PR falha se front-end falhar.
- Artefatos de testes ficam disponíveis.
- Documentar resultado em `docs/legal/evidence/ci-validation.md`.

---

## Task LGPD-CORR-08-04 — Criar evidência final da auditoria técnica

**Prioridade:** P1  
**Tipo:** Documentação  
**Back-end:** Sim  
**Front-end:** Sim

### Arquivo

```text
docs/legal/evidence/lgpd-correction-final-validation.md
```

### Conteúdo

- SHA do back-end.
- SHA do front-end.
- Resultado de testes.
- Pendências corrigidas.
- Pendências aceitas como risco.
- Confirmação explícita:

```text
Liveness permanece não obrigatório por decisão de produto/operação.
Nenhuma task deste backlog alterou esse comportamento.
```

### Critérios de aceite

- Documento criado.
- Evidência versionada.
- Release só segue após esse documento.

---

# 5. Ordem recomendada de execução

## Primeiro bloco — P0 técnico

1. `LGPD-CORR-01-02` — separar token retention.
2. `LGPD-CORR-01-03` — audit log retention.
3. `LGPD-CORR-01-04` — legal consent retention.
4. `LGPD-CORR-01-05` — biometric artifact retention.
5. `LGPD-CORR-02-02` — trava de segurança para APPLY.
6. `LGPD-CORR-03-02` — corrigir dry-run de ponto.
7. `LGPD-CORR-04-01` — status consolidado de anonimização.
8. `LGPD-CORR-04-02` — bloquear conclusão indevida.
9. `LGPD-CORR-07-01` — prazo obrigatório para incidentes comunicáveis.
10. `LGPD-CORR-07-02` — evidência obrigatória para encerramento.

## Segundo bloco — contrato/front

11. `LGPD-CORR-05-01` — padronizar prefixo `/api`.
12. `LGPD-CORR-05-02` — update de inventário.
13. `LGPD-CORR-06-01` — modal de confirmação da exportação.

## Terceiro bloco — comprovação

14. `LGPD-CORR-08-01` — testes back-end.
15. `LGPD-CORR-08-02` — testes front-end.
16. `LGPD-CORR-08-03` — CI.
17. `LGPD-CORR-08-04` — evidência final.

---

# 6. Critério de conclusão do backlog

Este backlog será considerado concluído quando:

- todos os `RetentionResourceType` tiverem processor ou justificativa formal;
- scheduler de retenção estiver preparado para produção com `DRY_RUN`;
- `APPLY` tiver trava global;
- anonimização de ponto diferenciar preservação legal e anonimização;
- dry-run não subestimar impacto;
- falhas parciais bloquearem conclusão como sucesso total;
- rotas do inventário estiverem padronizadas;
- exportação exigir confirmação explícita no front;
- incidentes comunicáveis exigirem prazos e evidências;
- CI comprovar os testes;
- liveness permanecer sem alteração de obrigatoriedade.

---

# 7. Checklist final

```text
[ ] Liveness não foi tornado obrigatório.
[ ] Todos os RetentionResourceType possuem processor.
[ ] PASSWORD_RESET_TOKEN tem processor próprio.
[ ] AUDIT_LOG tem retenção/sanitização.
[ ] LEGAL_CONSENT tem retenção/minimização.
[ ] BIOMETRIC_ARTIFACT tem retenção própria.
[ ] LGPD_REQUEST tem retenção/minimização.
[ ] Scheduler de retenção está preparado para produção em DRY_RUN.
[ ] APPLY depende de flag global explícita.
[ ] TimeRecordAnonymizer diferencia preserveLaborData true/false.
[ ] Dry-run retorna impacto correto.
[ ] Anonimização retorna SUCCESS/PARTIAL_SUCCESS/FAILED/BLOCKED.
[ ] Solicitação LGPD não conclui como COMPLETED em falha parcial.
[ ] Prefixo /api do inventário está padronizado.
[ ] Exportação no front exige confirmação.
[ ] Incidente comunicável exige deadline.
[ ] Incidente comunicável exige evidência para encerrar.
[ ] Testes back-end passam.
[ ] Testes front-end passam.
[ ] CI executa tudo.
[ ] Evidência final foi criada.
```
