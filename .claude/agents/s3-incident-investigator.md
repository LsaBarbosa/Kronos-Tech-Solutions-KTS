# Agent: Investigador de Incidente S3 — Kronos

## Objetivo

Diagnosticar, corrigir e documentar falhas no pipeline de upload/download de documentos S3
do sistema Kronos em produção (VPS Hostinger).

## Escopo

- Incidentes em `POST /documents` (upload) e `GET /documents/{id}` (download)
- Falhas com HTTP 400, 500, ou mensagem vazia em exceção S3
- Problemas de credenciais AWS, região, bucket, antivírus (ClamAV)
- Registros órfãos no banco (documento persistido sem arquivo no S3)

## Subagentes orquestrados

| Subagente | Responsabilidade |
|---|---|
| `log-forensics` | Extrai stack traces e correlaciona eventos por timestamp e thread |
| `env-aws-validator` | Valida presença de variáveis, região AWS, status IAM, permissões |
| `code-path-tracer` | Rastreia fluxo Controller → UseCase → Adapter e identifica ponto de falha |

## Critério de sucesso

- [ ] Causa raiz identificada com evidência (stack trace + linha de código)
- [ ] Correção aplicada e validada (upload e download funcionam end-to-end)
- [ ] Registros órfãos tratados ou confirmados como inexistentes
- [ ] Auditoria em `/home/deploy/auditorias/AUDITORIA_CORRECAO_S3_YYYYMMDD.md` criada

## Artefatos de saída esperados

1. **DIAG-08**: Causa raiz identificada (preenchida no `PLANO_INCIDENTE_S3_*.md`)
2. **Código corrigido**: diff aplicado no adapter ou service relevante
3. **Env atualizado**: se credenciais AWS rotacionadas
4. **AUDITORIA_CORRECAO_S3_YYYYMMDD.md**: registro completo do incidente
5. **Banco limpo**: confirmação de que não há registros órfãos (ou limpeza realizada)

## Contexto do incidente 2026-07-01

**INCIDENTE 1 (upload)**: ClamAV não instalado → `ConnectException` porta 3310 → HTTP 500
**Status**: CORRIGIDO (ClamAV instalado e rodando)

**INCIDENTE 2 (download)**: IAM user `kronos_render` quarentenado pela AWS com policy
`AWSCompromisedKeyQuarantineV3` → HTTP 403 em todas as operações S3 → HTTP 400 no cliente
com mensagem "Falha ao buscar o arquivo no storage: " (vazia por bug no catch do DocumentService)

**Causa raiz INCIDENTE 2**: Chave `AKIA5XMWTWT5HGX5KEV3` foi commitada no histórico git
(ver AUDITORIA_SEGURANCA_20260629 item CRIT-001). AWS detectou e quarentenou automaticamente.

**Ação necessária (manual)**: Acessar console AWS → criar novas credenciais IAM → atualizar env.

## Skills referenciadas

- `.claude/skills/s3-storage-skill.md`
- `.claude/skills/backend-prod-debug-skill.md`
