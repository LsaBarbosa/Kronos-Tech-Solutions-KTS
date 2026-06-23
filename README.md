# Kronos — Pacote Claude Code para correção multiempresa por CPF

## Objetivo

Implementar de forma segura o cenário em que uma mesma pessoa física, identificada por um CPF, pode atuar como gestor em mais de uma empresa no Kronos, sem quebrar isolamento por tenant, autenticação, autorização, LGPD, auditoria e fluxos existentes.

## Decisão técnica central

Não remover validação de CPF.

Ajustar a modelagem para diferenciar:

1. CPF como identidade da pessoa física.
2. `company_id` como escopo/tenant.
3. Acesso do usuário como relação entre usuário, empresa e papel.
4. Colaborador como vínculo operacional/trabalhista dentro de uma empresa.

## Entrega esperada

Este pacote contém:

- regras permanentes para Claude Code;
- skill de implementação;
- agentes e subagentes especializados;
- plano de ação por fases;
- critérios de aceite;
- plano de rollback;
- prompt mestre para execução no Claude Code;
- prompt compatível caso o executor seja Codex.

## Estrutura

```text
.claude/
  rules/
  skills/
  agents/
  subagents/
  commands/
plan/
prompts/
docs_index/
```

## Como usar

1. Copie o conteúdo deste pacote para a raiz do repositório backend Kronos.
2. Garanta que a branch atual seja `homolog`.
3. Garanta que os documentos técnicos estejam disponíveis no repositório ou no diretório de documentação do projeto.
4. Abra o Claude Code na raiz do repositório.
5. Execute o prompt de `prompts/CLAUDE_CODE_MASTER_PROMPT.md`.

## Escopo seguro para homolog

A implementação deve seguir uma estratégia incremental:

1. Corrigir unicidade de CPF por empresa: `UNIQUE(company_id, cpf)`.
2. Criar relação explícita de acesso usuário-empresa: `tb_user_company_access`.
3. Adicionar contexto de empresa ativa no JWT.
4. Adicionar endpoints para listar empresas acessíveis e trocar empresa ativa.
5. Ajustar validações de cadastro, login, autorização e testes.
6. Ajustar front-end somente depois do backend estabilizado.

## O que não fazer

- Não remover a validação de CPF.
- Não permitir CPF duplicado dentro da mesma empresa.
- Não confiar em `companyId` vindo do front quando o usuário autenticado é MANAGER.
- Não colocar regra de negócio em controller.
- Não quebrar fluxos de PARTNER, CTO, login facial, termos, LGPD e ponto.
- Não emitir JWT sem empresa ativa para usuário MANAGER/PARTNER em fluxos autenticados normais.
