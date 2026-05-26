# Backlog técnico — Correção da documentação `/docs` referenciada no README

## Projeto

**Repositório:** `LsaBarbosa/Kronos-Tech-Solutions-KTS`  
**Branch alvo:** `feature/lgpd-compliance`  
**Escopo:** back-end  
**Contexto:** o `README.md` referencia arquivos dentro de `/docs`, mas esses arquivos não foram confirmados como acessíveis na branch analisada.

---

## 1. Problema

O `README.md` aponta para documentos de segurança, compliance, produção e banco de dados:

```md
docs/security/session-policy.md
docs/security/csrf-policy.md
docs/legal/data-retention.md
docs/production/hostinger-deploy.md
docs/database/migrations.md
PRE_PRODUCTION_CHECKLIST.md
```

A ausência ou inacessibilidade desses arquivos cria uma inconformidade documental para uma aplicação que trata:

- dados pessoais;
- dados pessoais sensíveis;
- biometria facial;
- geolocalização;
- documentos trabalhistas;
- registros de ponto;
- logs de auditoria;
- solicitações LGPD.

A correção deve garantir que os arquivos referenciados realmente existam, estejam versionados, sejam úteis para auditoria e sejam mantidos consistentes.

---

## 2. Objetivo do backlog

Criar uma base documental mínima e auditável dentro do repositório, cobrindo:

- política de sessão;
- política de CSRF;
- política de retenção de dados;
- guia de deploy em produção sem exposição de segredos;
- guia de migrações Flyway;
- checklist pré-produção;
- validação automática de links internos de documentação.

---

## 3. Fora de escopo

Este backlog **não deve** alterar regras de negócio do sistema.

Não alterar:

- controllers;
- services;
- entidades;
- repositories;
- migrations existentes;
- configuração de segurança;
- configuração de cookies;
- fluxo LGPD;
- fluxo biométrico.

Alterações permitidas:

- criação de arquivos `.md`;
- ajuste de links no `README.md`, se necessário;
- criação de script simples de validação documental;
- inclusão opcional do script em CI, se já houver workflow existente.

---

## 4. Estrutura esperada após conclusão

```text
docs/
  README.md
  security/
    session-policy.md
    csrf-policy.md
  legal/
    data-retention.md
    lgpd-overview.md
    biometric-data-policy.md
    data-subject-rights.md
    incident-response-lgpd.md
  production/
    hostinger-deploy.md
  database/
    migrations.md
scripts/
  check-doc-links.sh
PRE_PRODUCTION_CHECKLIST.md
```

Observação:

- Os arquivos diretamente referenciados no `README.md` são obrigatórios.
- Os demais arquivos legais são recomendados para completar a base LGPD mínima.

---

# Épico DOCS-00 — Preparação e inventário

---

## História DOCS-001 — Inventariar links documentais referenciados no README

### Objetivo

Identificar todos os links internos do `README.md` que apontam para arquivos locais e registrar quais precisam existir.

### Arquivos envolvidos

```text
README.md
```

### Tarefas

- Ler o `README.md`.
- Identificar todos os links internos no formato Markdown:

```md
[texto](caminho/local.md)
```

- Separar links por categoria:

```text
docs/security
docs/legal
docs/production
docs/database
raiz do projeto
```

- Confirmar se cada arquivo existe.
- Registrar a lista final de arquivos obrigatórios no próprio backlog ou no PR.

### Critérios de aceite

- Todos os links internos do `README.md` foram identificados.
- Existe uma lista objetiva de arquivos obrigatórios.
- Nenhum link externo precisa ser validado nesta história.
- Nenhum código Java foi alterado.

### Validação manual

```bash
grep -oE '\([A-Za-z0-9_./-]+\.md\)' README.md | tr -d '()'
```

---

## História DOCS-002 — Criar estrutura base da pasta `/docs`

### Objetivo

Criar a estrutura de diretórios esperada para documentação técnica, legal e operacional.

### Arquivos/diretórios envolvidos

```text
docs/
docs/security/
docs/legal/
docs/production/
docs/database/
```

### Tarefas

- Criar a pasta `docs`, caso não exista.
- Criar subpastas:
    - `docs/security`
    - `docs/legal`
    - `docs/production`
    - `docs/database`
- Criar `docs/README.md` como índice principal.
- No índice, listar todos os documentos disponíveis.
- Informar claramente que a documentação não substitui assessoria jurídica.

### Conteúdo mínimo de `docs/README.md`

```md
# Documentação técnica e compliance — Kronos

Esta pasta centraliza documentos técnicos, operacionais e de conformidade do back-end Kronos.

## Segurança

- [Session Policy](security/session-policy.md)
- [CSRF Protection](security/csrf-policy.md)

## LGPD e documentos legais

- [LGPD Overview](legal/lgpd-overview.md)
- [Data Retention](legal/data-retention.md)
- [Biometric Data Policy](legal/biometric-data-policy.md)
- [Data Subject Rights](legal/data-subject-rights.md)
- [Incident Response LGPD](legal/incident-response-lgpd.md)

## Operação

- [Production Deployment](production/hostinger-deploy.md)
- [Database Migrations](database/migrations.md)
```

### Critérios de aceite

- A estrutura de pastas existe.
- `docs/README.md` existe.
- O índice aponta para todos os documentos criados neste backlog.
- Os links são relativos e funcionam no GitHub.

---

# Épico DOCS-10 — Documentação de segurança

---

## História DOCS-011 — Criar `docs/security/session-policy.md`

### Objetivo

Documentar a política de sessão do Kronos, incluindo JWT, cookies, expiração, logout, refresh e revogação.

### Arquivo a criar

```text
docs/security/session-policy.md
```

### Conteúdo obrigatório

O documento deve conter as seções:

```md
# Session Policy

## Objetivo

## Escopo

## Componentes envolvidos

## Estratégia de autenticação

## Cookies de autenticação

## Expiração de sessão

## Refresh token / renovação

## Logout e blacklist

## Revogação por troca de senha

## Regras para produção

## Variáveis de ambiente relacionadas

## Riscos conhecidos

## Checklist de validação
```

### Pontos técnicos que devem ser documentados

- O sistema usa autenticação stateless baseada em JWT.
- O token de acesso deve ser entregue preferencialmente em cookie `HttpOnly`.
- Cookie em produção deve usar:
    - `HttpOnly=true`
    - `Secure=true`
    - `SameSite=Lax` ou configuração justificada
    - path `/`
- A expiração curta reduz impacto de roubo de token.
- Logout deve adicionar token atual à blacklist.
- Troca de senha deve invalidar sessões anteriores via versão de sessão.
- Nunca documentar valores reais de `JWT_SECRET`.

### Variáveis a listar

```text
JWT_SECRET
JWT_EXPIRATION
AUTH_COOKIE_NAME
AUTH_COOKIE_SECURE
AUTH_COOKIE_SAME_SITE
AUTH_COOKIE_PATH
AUTH_COOKIE_DOMAIN
AUTH_COOKIE_MAX_AGE_SECONDS
```

### Critérios de aceite

- Arquivo criado.
- Documento explica a política sem expor segredo.
- Documento diferencia desenvolvimento e produção.
- Documento contém checklist verificável.

### Checklist mínimo dentro do arquivo

```md
## Checklist de validação

- [ ] `JWT_SECRET` não possui valor default em produção.
- [ ] `AUTH_COOKIE_SECURE=true` em produção.
- [ ] `AUTH_COOKIE_SAME_SITE` está definido conscientemente.
- [ ] Logout adiciona o token à blacklist.
- [ ] Troca de senha invalida sessões antigas.
- [ ] Endpoints sensíveis exigem autenticação.
```

---

## História DOCS-012 — Criar `docs/security/csrf-policy.md`

### Objetivo

Documentar a estratégia de proteção CSRF usada pela aplicação.

### Arquivo a criar

```text
docs/security/csrf-policy.md
```

### Conteúdo obrigatório

```md
# CSRF Protection Policy

## Objetivo

## Por que CSRF é necessário

## Estratégia adotada

## Cookie CSRF

## Header CSRF

## Endpoints isentos

## Integração com front-end

## Regras para produção

## Variáveis de ambiente relacionadas

## Checklist de validação
```

### Pontos técnicos que devem ser documentados

- A API usa cookie de autenticação com `withCredentials`.
- Requisições de escrita devem enviar header CSRF.
- Métodos afetados:
    - `POST`
    - `PUT`
    - `PATCH`
    - `DELETE`
- Endpoints públicos de autenticação podem ser isentos.
- O cookie CSRF pode não ser `HttpOnly`, pois o front precisa ler o token.
- O token de autenticação deve continuar `HttpOnly`.

### Variáveis a listar

```text
app.security.csrf.cookie-name
app.security.csrf.header-name
app.security.csrf.cookie-path
app.security.csrf.secure
app.security.csrf.same-site
```

### Critérios de aceite

- Arquivo criado.
- Documento explica diferença entre cookie de autenticação e cookie CSRF.
- Documento lista endpoints isentos e justifica.
- Documento possui checklist.

### Checklist mínimo dentro do arquivo

```md
## Checklist de validação

- [ ] Métodos de escrita exigem CSRF.
- [ ] Cookie de autenticação é `HttpOnly`.
- [ ] Cookie CSRF é acessível ao front quando necessário.
- [ ] Endpoints isentos são mínimos e justificados.
- [ ] Ambiente de produção usa HTTPS.
```

---

# Épico DOCS-20 — Documentação LGPD

---

## História DOCS-021 — Criar `docs/legal/lgpd-overview.md`

### Objetivo

Criar uma visão geral de LGPD aplicada ao Kronos.

### Arquivo a criar

```text
docs/legal/lgpd-overview.md
```

### Conteúdo obrigatório

```md
# LGPD Overview — Kronos

## Objetivo

## Dados tratados

## Dados pessoais sensíveis

## Finalidades de tratamento

## Bases legais

## Papéis: controlador, operador e suboperadores

## Direitos dos titulares

## Segurança e prevenção

## Auditoria e responsabilização

## Limitações deste documento
```

### Dados que devem ser citados

- Dados de identificação:
    - nome;
    - CPF;
    - PIS;
    - e-mail;
    - telefone.
- Dados funcionais:
    - cargo;
    - salário;
    - jornada;
    - escala;
    - registros de ponto.
- Dados sensíveis:
    - biometria facial.
- Dados técnicos:
    - IP;
    - User-Agent;
    - logs;
    - geolocalização.
- Documentos:
    - atestados;
    - comprovantes;
    - termos;
    - documentos trabalhistas.

### Critérios de aceite

- Documento criado.
- Documento diferencia dado pessoal comum e sensível.
- Documento informa que biometria exige proteção reforçada.
- Documento não promete conformidade absoluta.
- Documento orienta validação jurídica antes de produção.

---

## História DOCS-022 — Criar `docs/legal/data-retention.md`

### Objetivo

Documentar política de retenção, descarte, anonimização e preservação legal.

### Arquivo a criar

```text
docs/legal/data-retention.md
```

### Conteúdo obrigatório

```md
# Data Retention Policy

## Objetivo

## Escopo

## Princípios LGPD aplicados

## Categorias de dados

## Retenção por categoria

## Dados preservados por obrigação legal/trabalhista

## Dados elegíveis para anonimização

## Dados elegíveis para exclusão

## Execução em modo DRY_RUN

## Execução em modo APPLY

## Auditoria da retenção

## Variáveis de ambiente relacionadas

## Checklist de produção
```

### Tabela mínima obrigatória

```md
| Categoria | Exemplos | Retenção sugerida | Ação após prazo | Observação |
|---|---|---:|---|---|
| Registro de ponto | entrada, saída, NSR, geolocalização | conforme obrigação legal aplicável | preservar/anonimizar parcialmente | validar com jurídico |
| Documentos trabalhistas | atestado, comprovante, espelho | conforme obrigação legal aplicável | preservar enquanto obrigatório | não excluir sem análise |
| Biometria facial | imagem/template facial | enquanto houver consentimento ativo e finalidade válida | excluir/revogar | dado sensível |
| Logs de auditoria | IP, User-Agent, ação | prazo operacional/legal definido | anonimizar ou expurgar | preservar segurança |
| Solicitações LGPD | histórico e resposta | prazo de defesa/auditoria | preservar ou anonimizar | evidência de atendimento |
```

### Variáveis a listar

```text
LGPD_RETENTION_SCHEDULER_ENABLED
LGPD_RETENTION_SCHEDULER_CRON
LGPD_RETENTION_SCHEDULER_MODE
LGPD_RETENTION_SCHEDULER_APPLY_CONFIRMED
LGPD_RETENTION_SCHEDULER_JUSTIFICATION
LGPD_RETENTION_ALLOW_APPLY
```

### Critérios de aceite

- Arquivo criado.
- Documento explica diferença entre `DRY_RUN` e `APPLY`.
- Documento informa que `APPLY` exige justificativa e confirmação.
- Documento orienta que retenção real em produção depende de validação jurídica.
- Documento não manda apagar dados trabalhistas/fiscais automaticamente sem análise.

---

## História DOCS-023 — Criar `docs/legal/biometric-data-policy.md`

### Objetivo

Documentar regras específicas de tratamento de biometria facial.

### Arquivo a criar

```text
docs/legal/biometric-data-policy.md
```

### Conteúdo obrigatório

```md
# Biometric Data Policy

## Objetivo

## Por que biometria é dado sensível

## Finalidades permitidas

## Consentimento biométrico

## Revogação de consentimento

## Liveness

## Armazenamento de imagem/template

## Integração com provedor externo

## Exclusão de artefatos biométricos

## Auditoria

## Checklist de produção
```

### Pontos obrigatórios

- Biometria deve ser usada apenas para finalidade documentada.
- Login facial e registro de ponto devem ter consentimentos/finalidades separadas quando aplicável.
- Revogação deve remover imagem e template quando não houver outra base legal/finalidade.
- Liveness deve ser obrigatório em produção.
- Não incluir exemplos reais de imagem, bucket ou identificadores de usuários.

### Critérios de aceite

- Arquivo criado.
- Documento trata biometria como dado sensível.
- Documento descreve revogação.
- Documento menciona liveness obrigatório em produção.
- Documento referencia o fluxo de consentimento biométrico.

---

## História DOCS-024 — Criar `docs/legal/data-subject-rights.md`

### Objetivo

Documentar como o Kronos trata solicitações dos titulares.

### Arquivo a criar

```text
docs/legal/data-subject-rights.md
```

### Conteúdo obrigatório

```md
# Data Subject Rights — LGPD

## Objetivo

## Tipos de solicitação suportados

## Fluxo de abertura

## Fluxo de análise

## Fluxo de conclusão

## Rejeição justificada

## Complementação pelo titular

## Exportação de dados

## Correção de dados

## Anonimização, bloqueio e exclusão

## Portabilidade

## Histórico e auditoria

## SLA interno

## Checklist operacional
```

### Tipos de solicitação a listar

```text
CONFIRM_PROCESSING
ACCESS
CORRECTION
ANONYMIZATION
BLOCKING
DELETION
PORTABILITY
CONSENT_REVOCATION
SHARING_INFORMATION
```

### Critérios de aceite

- Arquivo criado.
- Documento explica os direitos do titular em linguagem operacional.
- Documento informa que pedidos podem ser rejeitados com justificativa quando houver obrigação legal de preservação.
- Documento separa solicitação do titular de execução automática.

---

## História DOCS-025 — Criar `docs/legal/incident-response-lgpd.md`

### Objetivo

Documentar o fluxo operacional de resposta a incidentes de segurança envolvendo dados pessoais.

### Arquivo a criar

```text
docs/legal/incident-response-lgpd.md
```

### Conteúdo obrigatório

```md
# LGPD Security Incident Response

## Objetivo

## O que é incidente de segurança

## Classificação de severidade

## Fluxo de registro

## Avaliação de risco

## Plano de correção

## Comunicação interna

## Comunicação ao controlador

## Comunicação à ANPD e titulares

## Evidências e auditoria

## Checklist de resposta
```

### Critérios de aceite

- Arquivo criado.
- Documento descreve que incidentes devem ser avaliados quanto a risco aos titulares.
- Documento não define prazo legal absoluto sem validação jurídica.
- Documento orienta acionar jurídico/DPO/controlador quando aplicável.
- Documento referencia o módulo de incidentes de segurança, sem depender de endpoint específico.

---

# Épico DOCS-30 — Documentação operacional

---

## História DOCS-031 — Criar `docs/production/hostinger-deploy.md`

### Objetivo

Criar guia de deploy seguro em produção na Hostinger/VPS.

### Arquivo a criar

```text
docs/production/hostinger-deploy.md
```

### Conteúdo obrigatório

```md
# Production Deployment — Hostinger VPS

## Objetivo

## Arquitetura de produção

## Pré-requisitos

## Variáveis de ambiente

## Banco de dados

## Build da aplicação

## Execução como serviço

## Nginx / proxy reverso

## HTTPS

## Health checks

## Logs

## Backup

## Rollback

## Segurança operacional

## Checklist pós-deploy
```

### Regras obrigatórias

- Não inserir IP real, senha real, segredo real, certificado real ou token real.
- Usar placeholders:

```text
<APP_DOMAIN>
<DB_HOST>
<DB_NAME>
<DB_USERNAME>
<JWT_SECRET>
<AWS_REGION>
```

- Alertar que `.env`, certificados e secrets não devem ser commitados.
- Incluir comandos genéricos, não comandos com segredo real.

### Critérios de aceite

- Arquivo criado.
- Documento não contém segredos.
- Documento orienta uso de HTTPS.
- Documento orienta `nginx -t` antes de reload.
- Documento orienta rollback.
- Documento orienta validação de `/actuator/health`.

---

## História DOCS-032 — Criar `docs/database/migrations.md`

### Objetivo

Documentar política de migrações com Flyway.

### Arquivo a criar

```text
docs/database/migrations.md
```

### Conteúdo obrigatório

```md
# Database Migrations — Flyway

## Objetivo

## Convenção de nomes

## Ordem de execução

## Regras para criar migrations

## Regras para alterar tabelas com dados sensíveis

## Rollback

## Validação local

## Validação em produção

## Boas práticas

## Checklist de PR
```

### Convenção recomendada

```text
V{numero}__descricao_curta.sql
```

Exemplo:

```text
V42__create_lgpd_request_tables.sql
```

### Regras obrigatórias

- Não editar migration já aplicada em ambiente compartilhado.
- Criar nova migration para alteração incremental.
- Evitar `DROP` destrutivo sem plano de backup.
- Para dados pessoais/sensíveis, documentar impacto.
- Validar localmente antes do PR.

### Critérios de aceite

- Arquivo criado.
- Documento explica Flyway de forma prática.
- Documento contém checklist de PR.
- Documento alerta sobre dados sensíveis.

---

## História DOCS-033 — Validar ou criar `PRE_PRODUCTION_CHECKLIST.md`

### Objetivo

Garantir que o arquivo referenciado pelo `README.md` exista na raiz do projeto e cubra segurança/LGPD antes de produção.

### Arquivo envolvido

```text
PRE_PRODUCTION_CHECKLIST.md
```

### Tarefas

- Verificar se o arquivo existe.
- Se não existir, criar.
- Se existir, revisar se cobre os itens mínimos abaixo.
- Não remover conteúdo existente sem necessidade.

### Conteúdo mínimo obrigatório

```md
# Pre-Production Checklist — Kronos

## Build e testes

- [ ] `./gradlew clean build` executa com sucesso.
- [ ] Testes unitários executam com sucesso.
- [ ] Testes de integração críticos executam com sucesso.

## Segurança

- [ ] `JWT_SECRET` definido por variável de ambiente.
- [ ] Cookies seguros em produção.
- [ ] CSRF ativo para métodos de escrita.
- [ ] Swagger público desativado em produção.
- [ ] Actuator expõe apenas endpoints necessários.

## LGPD

- [ ] Política de privacidade publicada.
- [ ] Termo de uso publicado.
- [ ] Termo biométrico versionado.
- [ ] Revogação biométrica testada.
- [ ] Exportação LGPD testada.
- [ ] Retenção em `DRY_RUN` validada.
- [ ] Retenção em `APPLY` somente com autorização.

## Uploads e documentos

- [ ] Limite de upload validado.
- [ ] MIME real validado.
- [ ] Antivírus habilitado em produção ou risco formalmente aceito.
- [ ] Bucket/document storage com acesso restrito.

## Produção

- [ ] HTTPS ativo.
- [ ] Backup validado.
- [ ] Rollback documentado.
- [ ] Logs sem dados sensíveis desnecessários.
```

### Critérios de aceite

- Arquivo existe.
- Checklist cobre build, segurança, LGPD, uploads e produção.
- Nenhum segredo real foi documentado.

---

# Épico DOCS-40 — Validação automática de documentação

---

## História DOCS-041 — Criar script `scripts/check-doc-links.sh`

### Objetivo

Criar um script simples para detectar links internos quebrados em arquivos Markdown.

### Arquivo a criar

```text
scripts/check-doc-links.sh
```

### Requisitos do script

- Usar Bash.
- Não depender de Node, npm, Python ou ferramentas externas.
- Validar links Markdown locais terminados em `.md`.
- Ignorar links externos `http://` e `https://`.
- Ignorar anchors internos `#secao`.
- Retornar exit code `1` se algum arquivo referenciado não existir.
- Retornar exit code `0` se todos existirem.

### Exemplo de implementação esperada

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FAILED=0

while IFS= read -r md_file; do
  dir="$(dirname "$md_file")"

  while IFS= read -r link; do
    target="$(echo "$link" | sed -E 's/.*\(([^)]+)\).*/\1/')"

    if [[ "$target" =~ ^https?:// ]]; then
      continue
    fi

    if [[ "$target" =~ ^# ]]; then
      continue
    fi

    target="${target%%#*}"

    if [[ "$target" != *.md ]]; then
      continue
    fi

    if [[ "$target" = /* ]]; then
      resolved=".$target"
    else
      resolved="$dir/$target"
    fi

    if [[ ! -f "$resolved" ]]; then
      echo "Broken markdown link: $md_file -> $target"
      FAILED=1
    fi
  done < <(grep -oE '\[[^]]+\]\([^)]+\.md(#[^)]+)?\)' "$md_file" || true)
done < <(find . -name "*.md" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.git/*")

exit "$FAILED"
```

### Critérios de aceite

- Script criado.
- Script executável.
- Script retorna erro quando link local `.md` está quebrado.
- Script passa quando todos os links estão corretos.

### Validação

```bash
chmod +x scripts/check-doc-links.sh
./scripts/check-doc-links.sh
```

---

## História DOCS-042 — Documentar validação de links no README

### Objetivo

Adicionar ao `README.md` uma instrução curta para validar links de documentação.

### Arquivo envolvido

```text
README.md
```

### Tarefa

Adicionar seção em `Documentation` ou `Contributing`:

```md
### Validate documentation links

```bash
./scripts/check-doc-links.sh
```
```

### Critérios de aceite

- O README informa como validar links.
- O comando funciona localmente.
- A alteração não remove os links existentes.

---

## História DOCS-043 — Integrar validação documental no CI se existir GitHub Actions

### Objetivo

Executar `scripts/check-doc-links.sh` automaticamente no CI, se o projeto já possuir workflow.

### Arquivos possíveis

```text
.github/workflows/*.yml
.github/workflows/*.yaml
```

### Tarefas

- Verificar se existe workflow de CI.
- Se existir, adicionar step:

```yaml
- name: Validate markdown documentation links
  run: ./scripts/check-doc-links.sh
```

- Se não existir workflow, não criar um novo nesta história.
- Registrar no PR que a validação fica manual enquanto não houver CI.

### Critérios de aceite

- Se houver CI, o step foi adicionado.
- Se não houver CI, nada foi criado indevidamente.
- O script continua executando localmente.

---

# Épico DOCS-50 — Ajustes finais e revisão

---

## História DOCS-051 — Revisar links do `README.md`

### Objetivo

Garantir que todos os links do `README.md` apontem para arquivos reais.

### Arquivo envolvido

```text
README.md
```

### Tarefas

- Rodar `scripts/check-doc-links.sh`.
- Corrigir links quebrados.
- Se algum arquivo foi renomeado, ajustar o link no README.
- Não remover links importantes apenas para passar no script.

### Critérios de aceite

- `./scripts/check-doc-links.sh` passa.
- Todos os links internos do README abrem no GitHub.
- Os nomes dos documentos são consistentes.

---

## História DOCS-052 — Revisão de segurança documental

### Objetivo

Garantir que nenhum documento criado exponha dados sensíveis.

### Tarefas

Buscar nos arquivos `.md` por padrões sensíveis:

```bash
grep -RniE 'password|secret|token|access-key|private-key|BEGIN PRIVATE|AKIA|aws_secret|jwt_secret' docs README.md PRE_PRODUCTION_CHECKLIST.md || true
```

### Regras

- É permitido mencionar nomes de variáveis, como `JWT_SECRET`.
- É proibido inserir valores reais.
- É proibido inserir IP público real da VPS, se isso não for necessário.
- É proibido inserir senha, certificado, chave privada ou token real.
- É proibido inserir e-mails pessoais reais em exemplos.

### Critérios de aceite

- Nenhum segredo real aparece nos documentos.
- Variáveis sensíveis são representadas por placeholders.
- Documentos usam exemplos seguros.

---

## História DOCS-053 — Revisão de linguagem LGPD

### Objetivo

Garantir que a documentação não prometa conformidade jurídica absoluta.

### Tarefas

Revisar documentos legais para evitar frases como:

```text
O sistema está 100% conforme a LGPD.
Este documento garante conformidade legal.
Não há risco jurídico.
```

Substituir por linguagem adequada:

```text
Este documento descreve controles técnicos de apoio à conformidade com a LGPD.
A validação jurídica final deve ser realizada pelo responsável legal/controlador/DPO.
```

### Critérios de aceite

- Documentos não fazem promessa absoluta de conformidade.
- Documentos deixam claro que a validação jurídica é externa ao código.
- Documentos descrevem controles técnicos e operacionais.

---

# 5. Ordem recomendada de execução para o Codex

Executar nesta ordem:

```text
DOCS-001
DOCS-002
DOCS-011
DOCS-012
DOCS-021
DOCS-022
DOCS-023
DOCS-024
DOCS-025
DOCS-031
DOCS-032
DOCS-033
DOCS-041
DOCS-042
DOCS-043
DOCS-051
DOCS-052
DOCS-053
```

---

# 6. Definition of Done geral

O backlog só deve ser considerado concluído quando:

- [ ] Todos os arquivos referenciados no `README.md` existem.
- [ ] `docs/README.md` existe e funciona como índice.
- [ ] Documentos de segurança foram criados.
- [ ] Documentos LGPD mínimos foram criados.
- [ ] Guia de produção foi criado sem segredos.
- [ ] Guia de migrations foi criado.
- [ ] `PRE_PRODUCTION_CHECKLIST.md` existe.
- [ ] `scripts/check-doc-links.sh` existe e é executável.
- [ ] `./scripts/check-doc-links.sh` passa com exit code `0`.
- [ ] Nenhum segredo real foi inserido na documentação.
- [ ] Nenhum código funcional Java foi alterado.
- [ ] O PR descreve que a correção resolve a inconsistência documental de `/docs`.

---

# 7. Prompt sugerido para o Codex

```text
Você está no repositório LsaBarbosa/Kronos-Tech-Solutions-KTS, branch feature/lgpd-compliance.

Corrija exclusivamente a inconsistência documental onde o README.md referencia arquivos dentro de /docs que não existem ou não estão acessíveis.

Não altere código Java, regras de negócio, migrations existentes ou configurações funcionais.

Implemente o backlog DOCS abaixo:

1. Crie a estrutura docs/security, docs/legal, docs/production e docs/database.
2. Crie docs/README.md como índice.
3. Crie os documentos:
   - docs/security/session-policy.md
   - docs/security/csrf-policy.md
   - docs/legal/lgpd-overview.md
   - docs/legal/data-retention.md
   - docs/legal/biometric-data-policy.md
   - docs/legal/data-subject-rights.md
   - docs/legal/incident-response-lgpd.md
   - docs/production/hostinger-deploy.md
   - docs/database/migrations.md
4. Valide ou crie PRE_PRODUCTION_CHECKLIST.md.
5. Crie scripts/check-doc-links.sh para validar links Markdown locais.
6. Atualize README.md apenas se necessário para corrigir links ou adicionar instrução de validação.
7. Se existir workflow GitHub Actions, adicione um step para rodar ./scripts/check-doc-links.sh. Se não existir, não crie workflow novo.
8. Garanta que nenhum segredo real, IP real, certificado, senha, token, AWS key ou dado pessoal real seja inserido nos documentos.
9. Rode:
   - chmod +x scripts/check-doc-links.sh
   - ./scripts/check-doc-links.sh
10. Entregue um resumo dos arquivos criados/alterados.

Critérios finais:
- Todos os links internos .md do README.md e docs/**/*.md devem existir.
- O script deve retornar exit code 0.
- Nenhum código funcional deve ser alterado.
```

---

# 8. Observação importante

Este backlog cria documentação técnica e operacional para apoiar conformidade LGPD, mas não substitui validação jurídica.

Antes de produção, os documentos devem ser revisados por quem exerce o papel de controlador, operador, responsável legal ou DPO, conforme aplicável ao modelo comercial do Kronos.
