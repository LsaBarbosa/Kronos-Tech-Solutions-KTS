# Data Retention Status

## Status Atual

Kronos possui infraestrutura inicial para retenção, mas não executa ações destrutivas automáticas neste momento. O `RetentionPolicyService` roda em modo seguro, registra o que seria processado e atualiza `lastExecutedAt`, sem apagar ou anonimizar dados por política.

Essa limitação é intencional. Registros trabalhistas, fiscais, documentos legais e trilhas de auditoria não devem ser removidos automaticamente sem base jurídica e regra específica por domínio.

## Implementado Hoje

- tabela `tb_retention_policy`
- scheduler `DataRetentionScheduler`
- execução controlada por feature flag `kronos.lgpd.retention.scheduler.enabled`
- marcação de `last_executed_at`
- logs técnicos de execução
- proteção explícita contra ações destrutivas automáticas

## Não Implementado Hoje

- deleção física automática
- anonimização automática por retenção
- remoção automática de documentos vencidos
- limpeza automática de mensagens, tokens ou anexos por política LGPD
- retenção destrutiva segmentada por domínio documental

## Situação por Domínio

| Domínio | Status atual | Ação automática hoje | Próxima evolução |
|---|---|---|---|
| Tokens | Parcial | Não via `RetentionPolicyService` | Criar política destrutiva segura |
| Mensagens | Planejado | Nenhuma | Definir prazo, critérios e job |
| Documentos legais | Planejado | Nenhuma | Modelar legal hold e retenção por tipo |
| Biometria | Parcial | Removida na revogação | Definir política formal de retenção da evidência |
| Audit logs | Parcial | Nenhuma | Definir prazo mínimo e rotina segura |
| Incidentes | Parcial | Registro mantido | Formalizar retenção mínima e descarte |

## Diretriz Operacional

- Não habilitar ações destrutivas por retenção sem validação jurídica.
- Não apagar automaticamente documentos trabalhistas ou fiscais sem base legal.
- Tratar retenção atual como infraestrutura técnica preparada para backlog posterior.
