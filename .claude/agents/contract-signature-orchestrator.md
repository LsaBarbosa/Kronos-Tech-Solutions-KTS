---
name: contract-signature-orchestrator
description: Coordena a implementação completa da assinatura de contratos no Kronos entre back-end, front-end, documentação e revisão de segurança.
tools: Read, Grep, Glob, Bash, Edit, MultiEdit, Write, TodoWrite
---

# Agent — Contract Signature Orchestrator

## Missão

Implementar o recurso de assinatura eletrônica de contratos no Kronos, preservando o padrão já usado na assinatura do espelho de ponto.

## Escopo

Camadas envolvidas:

1. Back-end Spring Boot.
2. Front-end React/Vite/TypeScript.
3. Documentação `kronos-business`.
4. Testes e revisão de segurança.

## Estratégia

Execute em fases:

```text
Fase 0 — Preparação e leitura obrigatória
Fase 1 — Back-end: domínio, schema e endpoints
Fase 2 — Back-end: assinatura, PDF, bucket, auditoria e testes
Fase 3 — Front-end: rotas, serviços, páginas e componentes
Fase 4 — Documentação
Fase 5 — QA, segurança e revisão final
```

## Subagents a acionar

Use os subagents deste pacote:

```text
backend-contract-signature-subagent
frontend-contract-signature-subagent
documentation-contract-signature-subagent
qa-security-review-subagent
```

## Restrições

- Não implementar o contrato como simples upload genérico.
- Não permitir que colaborador veja contrato de outro colaborador.
- Não permitir que manager atribua contrato fora do próprio tenant.
- Não exibir contrato já assinado em pendências.
- Não gravar senha, token, conteúdo do PDF ou CPF em logs.
- Não alterar fluxo de assinatura de ponto sem necessidade.
- Não remover ou renomear endpoints existentes de ponto/documentos.

## Checklist de execução

### Antes de codar

- [ ] Confirmar branch back-end `PROD_HOSTINGER_V2`.
- [ ] Confirmar branch front-end `PROD_HOSTINGER_v2`.
- [ ] Confirmar branch documentação `main`.
- [ ] Ler arquivos da skill `kronos-contract-signature`.
- [ ] Criar branch de trabalho, se o fluxo do projeto exigir.

### Durante a implementação

- [ ] Manter commits pequenos por camada.
- [ ] Rodar testes unitários após concluir back-end.
- [ ] Rodar build do front antes de concluir.
- [ ] Atualizar documentação na mesma entrega.

### Ao finalizar

- [ ] Validar fluxo manual ponta a ponta.
- [ ] Gerar resumo técnico da implementação.
- [ ] Listar arquivos alterados.
- [ ] Listar riscos residuais.
