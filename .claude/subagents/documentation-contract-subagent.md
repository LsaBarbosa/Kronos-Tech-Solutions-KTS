# Subagent — Documentation and Contract

## Missão

Atualizar `kronos-business` branch `main` e docs locais dos repositórios para refletir a funcionalidade demo sandbox.

## Documentos a consultar

No `kronos-business`:

```text
README.md
01-visao-negocio.md
03-atores-permissoes.md
04-fluxos-aplicacao.md
05-entradas-saidas-fluxos.md
06-contratos-api.md
10-regras-negocio.md
11-autenticacao-seguranca.md
12-autorizacao-permissoes.md
13-integracoes-externas.md
14-banco-dados-migrations.md
15-lgpd-privacidade.md
17-ambientes-variaveis.md
18-deploy.md
19-troubleshooting.md
ADR-*.md
```

## Atualizações obrigatórias

1. Fluxo CTO:
   - criar demo;
   - deletar demo;
   - validar purge;
   - recovery de sandbox suja.

2. Contrato de API:
   - método;
   - path;
   - role;
   - request;
   - response;
   - erros.

3. Segurança:
   - apenas CTO;
   - usuário sandbox apenas MANAGER;
   - isolamento por tenant;
   - bloqueio de integrações externas;
   - invalidação de sessão/cache.

4. LGPD:
   - dados sintéticos;
   - sem face real;
   - termo biométrico sintético;
   - sem Rekognition real;
   - sem bucket AWS de face.

5. Storage:
   - empresas reais: provider normal;
   - sandbox: storage local VPS;
   - path de purge.

6. Ambiente:
   - variáveis `KRONOS_DEMO_*`;
   - default desativado;
   - kill switch.

7. Troubleshooting:
   - purge parcial;
   - lock preso;
   - pasta antiga;
   - user órfão;
   - documento órfão;
   - cache/sessão residual.

## Regras

- Documentação não deve prometer recurso que o código não implementa.
- Se o contrato mudou, atualizar `06-contratos-api.md`.
- Se há decisão arquitetural nova, criar ADR.
- Não inserir segredo real.
