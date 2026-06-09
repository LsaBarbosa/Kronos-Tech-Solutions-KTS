# BACKEND CONTRACT GUARD — Gap Closure EPICs 01 a 07

## 1. Objetivo

Este documento define como validar o back-end durante a trilha de correção dos gaps dos EPICs 01 a 07 da branch `new-ui`.

O objetivo não é implementar novas funcionalidades no back-end. O objetivo é garantir que o front-end tenha contratos compatíveis para concluir as correções sem quebrar API, autenticação, autorização ou payloads existentes.

---

## 2. Princípio central

A trilha de gaps é majoritariamente front-end. Portanto, no back-end, a atuação padrão é:

```text
validar contrato, documentar risco e evitar mudança desnecessária.
```

Qualquer mudança de código precisa cumprir três condições:

1. O gap do front-end depende diretamente de dado ou comportamento do back-end.
2. O contrato atual foi verificado e não atende ao caso.
3. A alteração foi documentada como necessária e autorizada.

---

## 3. Contratos prioritários

### 3.1 Autenticação

Verificar:

- login por senha;
- login facial;
- recuperação de senha;
- redefinição de senha;
- retorno neutro para recuperação;
- token inválido ou expirado;
- payload de erro;
- headers e cookies relevantes.

Risco a evitar:

- mudar formato de login;
- mudar nomes de campos;
- mudar status HTTP esperado pelo front;
- mudar redirecionamento ou regra de sessão.

---

### 3.2 Dashboard e ponto

Verificar:

- endpoint de status do dia;
- endpoint de registro de ponto;
- retorno de sucesso;
- retorno de erro de localização;
- retorno de erro biométrico;
- retorno usado para pendência de termo;
- possibilidade de retry seguro.

Risco a evitar:

- alterar regra de cálculo de ponto;
- alterar nomes de status;
- alterar semântica de check-in;
- criar resposta nova sem o front estar preparado.

---

### 3.3 Empresas

Verificar:

- listagem;
- criação;
- atualização;
- dados de endereço;
- geolocalização;
- status ativo/inativo;
- payload de erro.

Risco a evitar:

- alterar DTO de empresa;
- remover campos usados pela tela `Empresa`;
- quebrar criação ou atualização já usadas pelo front.

---

### 3.4 Colaboradores

Verificar:

- listagem;
- criação;
- edição;
- ativação/desativação;
- CPF;
- username;
- vínculo com empresa;
- role ou perfil associado.

Risco a evitar:

- mudar validação de CPF sem alinhar front;
- mudar validação de username sem alinhar front;
- alterar resposta de ativação/desativação;
- alterar status usados por `StatusBadge`.

---

### 3.5 Usuários de acesso

Este é o maior ponto de atenção do EPIC 6.

Verificar se existe suporte para:

- listar usuários de acesso;
- consultar usuário por id;
- identificar role;
- identificar status;
- identificar colaborador vinculado;
- ativar acesso;
- desativar acesso;
- alterar role;
- reenviar convite ou redefinir fluxo de acesso, se existir;
- consultar último acesso, se existir.

Se o suporte não existir, registrar bloqueio. Não criar contrato novo apenas para permitir uma tela visual.

Saída esperada:

```text
SUPORTE_USUARIOS_ACESSO:
- LISTAGEM: sim/nao
- DETALHE: sim/nao
- STATUS: sim/nao
- ROLE: sim/nao
- COLABORADOR_VINCULADO: sim/nao
- ATIVAR_DESATIVAR: sim/nao
- ALTERAR_ROLE: sim/nao
- ULTIMO_ACESSO: sim/nao
- OBSERVACOES:
```

---

### 3.6 Avisos

Verificar:

- listagem de avisos;
- criação de aviso;
- filtro por prioridade;
- detalhe;
- exclusão;
- permissões por role;
- payload de erro.

Risco a evitar:

- alterar prioridade sem alinhar com front;
- mudar permissões de criação;
- quebrar exclusão ou detalhe.

---

### 3.7 Documentos

Verificar:

- listagem;
- filtro;
- upload;
- download;
- exclusão;
- status do documento;
- status do funcionário vinculado;
- payload de erro de listagem;
- payload de erro de upload.

Risco a evitar:

- mudar multipart;
- alterar nome de campo de arquivo;
- alterar URL ou formato de download;
- alterar status usado pelo front.

---

## 4. Áreas que exigem comparação com `main`

Quando possível, comparar:

```bash
git diff main...new-ui -- src/main/java src/main/resources
```

Foco da revisão:

- controllers;
- DTOs;
- request/response objects;
- exception handlers;
- configuração web;
- configuração de autenticação;
- CORS;
- cookies;
- migrations;
- properties de ambiente.

---

## 5. O que procurar no diff

Classificar cada diferença como:

| Tipo | Descrição | Ação |
|---|---|---|
| Compatível | Não muda contrato consumido pelo front | registrar |
| Risco baixo | muda detalhe interno sem impacto aparente | registrar |
| Risco médio | pode afetar tela específica | testar e documentar |
| Risco alto | muda endpoint, payload, auth ou status | parar e pedir decisão |

---

## 6. Proibições

Não fazer sem autorização explícita:

- criar endpoint novo;
- alterar DTO público;
- alterar status HTTP;
- alterar autenticação;
- alterar autorização;
- alterar CORS ou cookie;
- alterar schema;
- alterar regra de ponto;
- alterar regra de documento;
- alterar regra de biometria;
- alterar regra de empresa ou colaborador.

---

## 7. Relatório obrigatório

Ao final da validação contratual, entregar:

```text
BACKEND CONTRACT REPORT

BRANCH:
COMANDOS EXECUTADOS:
ARQUIVOS ANALISADOS:
ENDPOINTS VERIFICADOS:
DTOS VERIFICADOS:
AUTH/CORS/COOKIES:
CONTRATOS PRESERVADOS:
DIFERENCAS ENCONTRADAS:
RISCOS:
BLOQUEIOS PARA O FRONT:
SUPORTE A USUARIOS DE ACESSO:
TESTES EXECUTADOS:
RESULTADO:
PROXIMA ACAO:
```

---

## 8. Validações recomendadas

Executar quando houver ambiente local:

```bash
./gradlew test
./gradlew build
```

Se houver falha, registrar:

- comando;
- erro principal;
- provável causa;
- se a falha é relacionada à trilha de gaps;
- próxima ação sugerida.

---

## 9. Critério de aceite

Este documento é considerado atendido quando o relatório final deixa claro:

- se o back-end suporta os fluxos exigidos pelo front;
- se há bloqueio para usuários de acesso;
- se houve diferença contratual relevante entre `main` e `new-ui`;
- se testes foram executados;
- se nenhuma alteração de regra foi feita indevidamente.
