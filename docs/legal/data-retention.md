# Data Retention Policy

## Objetivo

Documentar a política técnica de retenção, descarte, anonimização e preservação de dados no Kronos, com foco em controles implementados e condicionantes operacionais.

## Escopo

Este documento cobre dados e evidências tratados pelo back-end, incluindo registros de ponto, documentos, consentimentos, logs, solicitações LGPD, mensagens internas e tokens de recuperação.

## Princípios LGPD aplicados

- necessidade;
- minimização;
- limitação de finalidade;
- segurança;
- rastreabilidade;
- preservação de evidência quando houver obrigação legal, contratual ou defesa administrativa.

## Categorias de dados

- registros de ponto e jornada;
- documentos corporativos e trabalhistas;
- biometria facial e artefatos relacionados;
- logs de auditoria e segurança;
- solicitações LGPD e seu histórico;
- mensagens internas;
- tokens de reset de senha;
- evidências de consentimento legal.

## Retenção por categoria

| Categoria | Exemplos | Retenção sugerida | Ação após prazo | Observação |
|---|---|---:|---|---|
| Registro de ponto | entrada, saída, NSR, geolocalização | conforme obrigação legal aplicável | preservar ou anonimizar parcialmente | validar com jurídico antes de descarte |
| Documentos trabalhistas | atestado, comprovante, espelho | conforme obrigação legal aplicável | preservar enquanto obrigatório | não excluir sem análise |
| Biometria facial | imagem facial, template, artefatos biométricos | enquanto houver consentimento ativo e finalidade válida | excluir ou revogar quando elegível | dado sensível |
| Logs de auditoria | IP, `User-Agent`, ação, trilha de segurança | prazo operacional/legal definido | anonimizar ou expurgar | preservar segurança e responsabilização |
| Solicitações LGPD | histórico, decisão, resposta | prazo de defesa/auditoria | preservar ou anonimizar | evidência de atendimento |

## Dados preservados por obrigação legal/trabalhista

Os catálogos de retenção do projeto já diferenciam dados que exigem preservação reforçada, como:

- registros de ponto;
- contratos e vínculos trabalhistas;
- documentos laborais;
- evidências de consentimento quando necessárias para conformidade e defesa.

Esses conjuntos não devem ser apagados automaticamente sem validação jurídica e operacional.

## Dados elegíveis para anonimização

Podem existir fluxos de anonimização ou minimização para:

- solicitações LGPD encerradas após prazo de retenção definido;
- consentimentos revogados antigos, com minimização de metadados;
- logs e trilhas quando a preservação integral deixar de ser necessária;
- dados pessoais de colaboradores desligados quando o cenário permitir anonimização em vez de exclusão total.

## Dados elegíveis para exclusão

Os fluxos técnicos já apontam elegibilidade de exclusão para:

- artefatos biométricos sem consentimento ativo ou com consentimento revogado elegível;
- mensagens internas fora do prazo;
- tokens de reset de senha expirados;
- documentos gerais quando não houver obrigação de preservação.

Exclusão efetiva em produção deve respeitar análise jurídica, backup, rastreabilidade e confirmação operacional.

## Status dos processadores de retenção

| Recurso | DRY_RUN | APPLY | Observação |
|---|:---:|:---:|---|
| TIME_RECORD | ✓ | ✗ | Preservado por obrigação legal (evidência trabalhista) |
| EMPLOYEE_CONTRACT | ✓ | ✗ | Preservado por obrigação legal (evidência fiscal/trabalhista) |
| AUDIT_LOG | ✓ | ✓ | Descarte com preservação de trilha configurável |
| BIOMETRIC_ARTIFACT | ✓ | ✓ | Remove de S3 e Rekognition quando elegível |
| MESSAGE | ✓ | ✓ | Soft-delete com preservação de contexto |
| PASSWORD_RESET_TOKEN | ✓ | ✓ | Hard-delete de tokens expirados |
| LEGAL_CONSENT | ✓ | ✓ | Descarte de consentimentos revogados/expirados |
| LGPD_REQUEST | ✓ | ✓ | Preservação de histórico com minimização de dados pessoais |
| DOCUMENT | ✓ | ✗ | Preservado até validação jurídica por classe documental |
| BLACKLISTED_TOKEN | ✓ | ✓ | Limpeza de tokens revogados |

**Nota:** Processadores para TIME_RECORD e EMPLOYEE_CONTRACT têm implementação bloqueada em APPLY até validação jurídica completa da política de preservação. Os dados podem ser simulados em DRY_RUN e preservados indefinidamente conforme obrigação legal.

## Execução em modo DRY_RUN

`DRY_RUN` é o modo seguro para simular a política sem apagar ou minimizar dados de forma efetiva.

Uso recomendado:

- validar elegibilidade;
- revisar contagens e impacto;
- confirmar se preservações trabalhistas e fiscais estão corretas;
- obter evidência para decisão operacional.

## Execução em modo APPLY

`APPLY` executa a ação efetiva da política e exige cuidado reforçado.

No comportamento atual do projeto:

- o endpoint administrativo recebe `justification` e `confirmed`;
- a execução pode ser bloqueada se `LGPD_RETENTION_ALLOW_APPLY` estiver desabilitado;
- o modo padrão de produção do scheduler é `DRY_RUN`;
- o scheduler possui flags próprias de modo, confirmação e justificativa;
- o validator de produção não exige `APPLY` global;
- quando `APPLY` estiver ativo, cada política ativa precisa ter processor com `supportsApply() = true`;
- o resultado é auditado e persistido em log de execução.

`APPLY` deve ser usado somente com justificativa registrada, confirmação explícita e autorização organizacional apropriada.

## Auditoria da retenção

O back-end registra:

- logs de execução de retenção;
- auditoria para `DRY_RUN`, `APPLY` executado e `APPLY` bloqueado;
- contagens de itens avaliados, afetados, ignorados e com erro.

Esses registros apoiam prestação de contas e investigação posterior.

## Variáveis de ambiente relacionadas

| Variável | Finalidade |
| --- | --- |
| `LGPD_RETENTION_SCHEDULER_ENABLED` | Habilita execução agendada. |
| `LGPD_RETENTION_SCHEDULER_CRON` | Define a agenda do scheduler. |
| `LGPD_RETENTION_SCHEDULER_MODE` | Define `DRY_RUN` ou `APPLY` no scheduler. |
| `LGPD_RETENTION_SCHEDULER_APPLY_CONFIRMED` | Exige confirmação explícita para execuções automáticas em `APPLY`. |
| `LGPD_RETENTION_SCHEDULER_JUSTIFICATION` | Registra justificativa padrão do batch agendado. |
| `LGPD_RETENTION_ALLOW_APPLY` | Libera ou bloqueia execuções efetivas de `APPLY`. |
| `LGPD_LOG_HASH_SECRET` | Segredo HMAC usado para gerar referências pseudonimizadas em logs. |

## Checklist de produção

- [ ] As políticas aplicáveis foram validadas com jurídico e controlador.
- [ ] `DRY_RUN` foi executado e revisado antes de qualquer `APPLY`.
- [ ] `LGPD_RETENTION_ALLOW_APPLY` permanece `false` até autorização formal.
- [ ] Execuções `APPLY` registram justificativa e confirmação.
- [ ] Dados trabalhistas e fiscais não serão apagados automaticamente sem análise.
- [ ] Backups e trilhas de auditoria foram considerados antes de retenção efetiva.
- [ ] O time consegue explicar por que cada categoria é preservada, minimizada ou excluída.

Esta política descreve controles técnicos de apoio à conformidade com a LGPD. A validação jurídica final deve ser realizada pelo responsável legal, controlador ou DPO.
