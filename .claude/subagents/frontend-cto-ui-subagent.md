# Subagent — Frontend CTO UI

## Missão

Implementar UI segura e objetiva para que somente `CTO` crie/deleta a demo.

## Entregas

- service HTTP;
- hook React Query;
- página ou bloco dentro de Administração;
- rota restrita;
- botões `CRIAR DEMO` e `DELETAR DEMO`;
- confirmação para deleção;
- status da demo;
- banner de sandbox;
- testes.

## Padrões

Usar:

- rotas em `src/config/app-routes.ts`;
- guard existente `RoleRoute`;
- cliente HTTP existente;
- `useMutation`/`useQuery` quando alinhado ao projeto;
- componentes UI já existentes;
- toast/alerta já existente.

## Estados da tela

A tela deve cobrir:

| Estado | Comportamento |
|---|---|
| Carregando status | skeleton/spinner |
| Demo inexistente | botão criar habilitado |
| Demo existente | mostrar usuário demo e botão deletar |
| Criando | desabilitar ações |
| Deletando | desabilitar ações |
| Sucesso create | mostrar credencial e status |
| Sucesso purge | mostrar validação limpa |
| Erro | mostrar erro sanitizado |
| Kill switch/desabilitado | mostrar bloqueio operacional |

## Segurança de UX

- Não renderizar tela para não-CTO.
- Mesmo assim, considerar que back-end é a autorização final.
- Não guardar senha em localStorage/sessionStorage.
- Não mostrar dados sensíveis em console.
- Não expor stack trace.
