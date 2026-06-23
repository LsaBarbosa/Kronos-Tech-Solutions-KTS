# Prompt compatível com Codex CLI — Kronos CTO Demo Sandbox

Atue como agente de implementação no projeto Kronos.

## Escopo

Implementar o fluxo CTO para criar/deletar empresa demo `Kronos Teste` com usuário `kronos_teste`, dados sintéticos, storage local isolado, purge idempotente, validação pós-purge, lock, kill switch e auditoria.

## Repositórios

```text
Backend: /home/deploy/apps/Kronos-Tech-Solutions-KTS — branch homolog
Frontend: /home/deploy/apps/Kronos-Tech-Solution-User-Plataform — branch homolog
Docs: /home/deploy/apps/kronos-business — branch main
```

## Instruções

1. Valide as branches antes de alterar.
2. Leia:
   - `.claude/rules/kronos-demo-sandbox.rules.md`
   - `.claude/skills/kronos-demo-sandbox/SKILL.md`
   - `docs_index/REPOSITORY_READING_MAP.md`
   - `docs_index/DEMO_SANDBOX_CONTRACT.md`
   - `plan/IMPLEMENTATION_PLAN_CTO_DEMO_SANDBOX.md`
3. Mapeie entidades e services reais.
4. Implemente back-end primeiro.
5. Implemente front-end depois.
6. Atualize docs por último.
7. Rode testes.
8. Gere relatório final.

## Restrições

- Não alterar provider global para local.
- Sandbox usa `/opt/kronos/sandbox/kronos-teste`.
- Empresa real continua usando storage normal.
- Sem AWS S3 para sandbox.
- Sem Rekognition real.
- Sem captura facial real.
- Sem imagem real.
- Sem dados reais.
- Usuário `kronos_teste` é apenas MANAGER.
- Somente CTO cria/deleta.
- Implementar lock e kill switch.
- Implementar auditoria.
- Invalidar sessões/tokens/cache/rate-limit conforme existir.
- Purge deve ser idempotente e limpar resíduos parciais.

## Entregáveis

- endpoints CTO;
- services de create/purge/validate/status;
- migration;
- storage router sandbox;
- auditoria;
- testes backend;
- UI CTO;
- service/hook frontend;
- banner sandbox;
- testes frontend;
- documentação atualizada.

## Comandos finais

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
./gradlew clean test

cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
npm run lint
npm run test
npm run build
```
