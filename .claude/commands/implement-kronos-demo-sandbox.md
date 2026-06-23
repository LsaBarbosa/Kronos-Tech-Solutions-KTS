# Command — Implement Kronos Demo Sandbox

Execute a skill `kronos-demo-sandbox`.

## Entrada

Repositórios locais:

```text
/home/deploy/apps/Kronos-Tech-Solutions-KTS
/home/deploy/apps/Kronos-Tech-Solution-User-Plataform
/home/deploy/apps/kronos-business
```

Branches:

```text
backend: homolog
frontend: homolog
docs: main
```

## Execução

1. Validar branches.
2. Mapear domínio backend.
3. Definir contrato HTTP.
4. Implementar backend.
5. Testar backend.
6. Implementar frontend.
7. Testar frontend.
8. Atualizar documentação.
9. Gerar relatório final.

## Não fazer

- Não mudar provider global para local.
- Não usar AWS/Rekognition para sandbox.
- Não deletar por nome sem `sandboxKey`.
- Não permitir endpoints para não-CTO.
- Não armazenar face real.
- Não usar dados reais.
- Não quebrar fluxos existentes.
