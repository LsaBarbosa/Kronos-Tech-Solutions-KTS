# AGENTS — Back-end Gap Closure EPICs 01 a 07

## 1. Papel deste arquivo

Este arquivo orienta agentes de implementação e revisão que atuarem no repositório back-end durante a trilha de correção dos gaps dos EPICs 01 a 07 da branch `new-ui`.

O back-end não é o foco visual dessa trilha. Ele atua como camada de verificação contratual para garantir que o front-end consiga fechar os gaps sem inventar dados, rotas ou regras que não existam.

---

## 2. Repositório e branch

Repositório:

```text
LsaBarbosa/Kronos-Tech-Solutions-KTS
```

Branch obrigatória:

```text
new-ui
```

Antes de qualquer análise local, confirmar:

```bash
git branch --show-current
git status --short
```

Se a branch não for `new-ui`, parar e avisar.

---

## 3. Missão do agente back-end

O agente back-end deve:

1. Validar se os contratos atuais suportam os gaps do front-end.
2. Mapear endpoints consumidos pelas telas dos EPICs 01 a 07.
3. Comparar, quando possível, `main...new-ui` nas áreas de contrato.
4. Identificar riscos de quebra para o front-end.
5. Registrar bloqueios quando o front-end precisar de dados que a API não fornece.
6. Executar testes do back-end quando disponível.
7. Não transformar correção visual em mudança de regra de negócio.

---

## 4. Escopo permitido

É permitido:

- ler controllers;
- ler DTOs de entrada e saída;
- ler services apenas para entender contrato;
- ler configurações de autenticação, CORS, cookies e headers;
- ler rotas relacionadas a login, empresas, colaboradores, usuários, avisos, documentos e ponto;
- executar testes;
- criar relatório técnico;
- propor ajuste contratual somente se houver bloqueio real documentado.

---

## 5. Fora de escopo

Não fazer sem autorização explícita:

- criar endpoint novo;
- remover endpoint existente;
- alterar payload de request ou response;
- alterar autenticação;
- alterar autorização por role;
- alterar entidades JPA;
- alterar migrations;
- alterar regra de negócio;
- alterar processamento de documentos, ponto, biometria, termos ou permissões;
- alterar comportamento para satisfazer apenas ajuste visual.

---

## 6. Arquivos de referência

Ler antes de qualquer conclusão:

```text
docs/new-ui/gap-closure-epics-1-7/BACKLOG.md
docs/new-ui/gap-closure-epics-1-7/SPEC.md
docs/new-ui/gap-closure-epics-1-7/BACKEND_CONTRACT_GUARD.md
docs/new-ui/gap-closure-epics-1-7/RULES.md
docs/new-ui/gap-closure-epics-1-7/TASKS.md
docs/new-ui/gap-closure-epics-1-7/SUB_AGENTS.md
docs/new-ui/gap-closure-epics-1-7/SKILLS.md
```

Se algum arquivo estiver ausente, registrar no relatório.

---

## 7. Áreas contratuais a verificar

### 7.1 Autenticação pública

Verificar suporte para:

- login por senha;
- login facial;
- recuperação de senha;
- redefinição de senha;
- retorno neutro de solicitação de recuperação;
- tratamento de token inválido ou expirado.

### 7.2 Dashboard e ponto

Verificar suporte para:

- status do dia;
- histórico ou resumo de ponto;
- fluxo de check-in;
- erro de geolocalização;
- erro de face ou câmera, quando retornado pela API;
- retry seguro para sincronização do status.

### 7.3 Empresas e colaboradores

Verificar suporte para:

- listagem de empresas;
- criação e atualização de empresa;
- geolocalização vinculada à empresa;
- listagem de colaboradores;
- criação de colaborador;
- ativação e desativação;
- edição de dados básicos;
- validações de CPF e username.

### 7.4 Usuários de acesso

Verificar se existe suporte para:

- listar usuários de acesso;
- ver role;
- ver status;
- ver colaborador vinculado;
- ativar ou desativar acesso;
- alterar role, se permitido;
- resetar ou reenviar fluxo de acesso, se existir.

Se não houver suporte, registrar bloqueio para a task de front-end correspondente. Não inventar contrato.

### 7.5 Avisos

Verificar suporte para:

- listar avisos;
- criar aviso;
- filtrar por prioridade;
- detalhar aviso;
- excluir aviso;
- identificar permissões de criação.

### 7.6 Documentos

Verificar suporte para:

- listar documentos;
- filtrar documentos;
- upload;
- download;
- exclusão;
- retorno de erro de listagem;
- estados de documento, funcionário e processamento.

---

## 8. Sequência de atuação

Para cada task back-end:

1. Declarar a task.
2. Listar arquivos que serão lidos.
3. Ler arquivos antes de concluir.
4. Verificar se há alteração contratual real.
5. Executar teste aplicável, se o workspace permitir.
6. Produzir relatório.
7. Parar.

Não avançar para próxima task sem autorização.

---

## 9. Formato obrigatório de relatório

```text
TASK:
STATUS:
ARQUIVOS ANALISADOS:
CONTRATOS VERIFICADOS:
RISCO PARA O FRONT-END:
ALTERAÇÃO DE CÓDIGO NECESSÁRIA:
BLOQUEIOS:
VALIDAÇÕES EXECUTADAS:
ERROS OU ALERTAS:
PRÓXIMA TASK SUGERIDA:
AGUARDANDO AUTORIZAÇÃO:
```

---

## 10. Validações recomendadas

Quando houver workspace local:

```bash
./gradlew test
./gradlew build
```

Quando a task envolver comparação entre branches:

```bash
git diff main...new-ui -- src/main/java src/main/resources
```

Foco do diff:

- controllers;
- DTOs;
- configuração de autenticação;
- CORS;
- cookies;
- endpoints consumidos pelo front-end;
- serialização de responses.

---

## 11. Critério de conclusão

O back-end estará concluído para esta trilha quando houver relatório informando:

- contratos preservados;
- riscos encontrados;
- bloqueios reais para o front-end;
- resultado dos testes executados;
- decisão sobre suporte à tela de usuários de acesso;
- confirmação de que nenhuma mudança de regra foi feita sem autorização.
