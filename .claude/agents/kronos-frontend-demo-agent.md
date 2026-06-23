# Agent — Kronos Frontend Demo Agent

## Papel

Implementar no front-end `Kronos-Tech-Solution-User-Plataform` branch `homolog` a interface CTO para criar/deletar demo, consumir endpoints reais e exibir estado de sandbox sem duplicar regra sensível.

## Regras

- Trabalhar somente depois do back-end definir contrato.
- Não inventar endpoint.
- Não expor token em storage.
- Não duplicar autorização final do backend.
- Usar rotas e metadata centralizadas.
- Usar service HTTP centralizado.
- Preservar UX desktop/mobile do shell existente.

## Arquivos a ler antes de alterar

```bash
cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform

sed -n '1,240p' src/App.tsx
sed -n '1,260p' src/config/app-routes.ts
rg -n "Administracao|Empresa|Dashboard|CTO|RoleRoute|ProtectedRoute|APP_ROUTE_META|APP_PATHS" src
rg -n "axios|apiClient|http|service|useMutation|useQuery|queryClient" src
rg -n "toast|Button|Card|AlertDialog|Dialog|Badge|Banner|Skeleton|Loading" src
rg -n "AuthContext|user.role|role|company|activeCompany|selectedCompany|isSandbox" src
```

## Componentes esperados

Preferir nomes alinhados ao padrão do projeto:

```text
src/service/demoSandboxService.ts
src/types/demoSandbox.ts
src/hooks/useDemoSandbox.ts
src/pages/DemoSandboxAdmin.tsx
src/components/demo/DemoSandboxActions.tsx
src/components/demo/DemoSandboxStatusCard.tsx
src/components/demo/DemoSandboxBanner.tsx
```

Adaptar à estrutura real se o projeto usar outro padrão.

## Rotas

Adicionar rota CTO:

```text
/demo-sandbox
```

ou rota administrativa equivalente:

```text
/administracao/demo-sandbox
```

Preferência: manter dentro da área `Administracao` se já existir menu/área CTO.

Atualizar:

```text
APP_PATHS
APP_ROUTE_META
App.tsx
menu/sidebar se existir
```

Allowed roles:

```ts
allowedRoles: ["CTO"]
```

## UI

Tela CTO deve conter:

1. Card de status da demo.
2. Botão `CRIAR DEMO`.
3. Botão `DELETAR DEMO`.
4. Confirmação explícita antes de deletar.
5. Loading state.
6. Estado de sucesso.
7. Estado de erro.
8. Resultado da validação pós-purge.
9. Contadores retornados pelo backend.
10. Aviso de que os dados são sintéticos e apagáveis.

Texto recomendado:

```text
Empresa demo: Kronos Teste
Usuário: kronos_teste
Senha padrão: kronos_teste#
A empresa demo usa dados sintéticos e pode ser apagada automaticamente pelo CTO.
```

Não exibir senha em telas que possam ser acessadas por não-CTO.

## Banner sandbox

Quando a sessão indicar empresa sandbox ativa:

```text
Ambiente de demonstração — dados sintéticos. Tudo pode ser apagado no próximo reset.
```

O banner deve aparecer em páginas autenticadas, sem quebrar layout mobile.

## Ações bloqueadas visualmente

Para empresa sandbox, bloquear/desabilitar ou sinalizar:

- alteração de username;
- alteração de senha;
- integrações externas;
- configurações globais.

A autorização real deve continuar no back-end.

## Service HTTP

Contrato conceitual:

```ts
POST   /api/cto/demo/create
DELETE /api/cto/demo
GET    /api/cto/demo/status
POST   /api/cto/demo/validate
```

Usar cliente HTTP existente e tratamento centralizado de erro.

## Testes

Criar ou atualizar testes para:

- renderiza botões para CTO;
- não renderiza para MANAGER/PARTNER;
- chama create;
- chama delete após confirmação;
- mostra loading;
- mostra erro;
- mostra validação pós-purge;
- mostra banner quando `activeCompany.isSandbox === true`.

## Comandos finais

```bash
npm run lint
npm run test
npm run build
```
