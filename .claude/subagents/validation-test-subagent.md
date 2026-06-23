# Subagent — Validation and Tests

## Missão

Criar e executar testes suficientes para provar que o fluxo não quebra clientes reais e é recuperável.

## Back-end — testes mínimos

### Security

- CTO acessa create/delete/status/validate.
- MANAGER recebe 403.
- PARTNER recebe 403.
- Não autenticado recebe 401/403 conforme padrão do projeto.

### Create

- cria empresa sandbox limpa;
- cria usuário `kronos_teste`;
- cria employee vinculado;
- cria registros do mês vigente;
- cria documentos sintéticos para todos os tipos;
- cria termo biométrico sintético;
- cria solicitações pendentes;
- não chama AWS/Rekognition;
- cria auditoria.

### Purge

- remove dados criados;
- remove dados criados depois pelo usuário sandbox;
- remove arquivos físicos;
- remove refresh/sessões/cache/rate-limit;
- mantém auditoria sanitizada;
- não remove empresa real;
- roda duas vezes sem erro fatal;
- limpa sandbox suja parcial.

### Storage

- empresa real usa provider normal;
- empresa sandbox usa local sandbox;
- path traversal bloqueado;
- purge só apaga path dentro da raiz sandbox.

## Front-end — testes mínimos

- CTO vê botões.
- MANAGER/PARTNER não vêem botões.
- create chama endpoint correto.
- delete exige confirmação.
- status é exibido.
- erros são tratados.
- banner aparece para sandbox.

## Comandos

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
./gradlew clean test
```

```bash
cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
npm run lint
npm run test
npm run build
```

## Relatório final

Registrar:

- total de testes novos;
- comandos executados;
- falhas;
- evidências;
- riscos.
