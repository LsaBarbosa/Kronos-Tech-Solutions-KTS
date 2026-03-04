# Fluxos visuais — MessageService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `postMessage`

![Fluxo message.postMessage](./flows/message/postMessage.svg)

- Obter remetente logado.
- Validar lista de destinatários.
- Filtrar destinatários válidos na mesma empresa.
- Criar mensagem individual por destinatário.
- Salvar mensagens.

## `listMessagesForMyCompany`

![Fluxo message.listMessagesForMyCompany](./flows/message/listMessagesForMyCompany.svg)

- Obter colaborador logado.
- Resolver companyId.
- Buscar mensagens visíveis por empresa e colaborador.
- Retornar lista.

## `deleteMessage`

![Fluxo message.deleteMessage](./flows/message/deleteMessage.svg)

- Obter remetente logado.
- Buscar mensagem por ID.
- Validar autoria da mensagem.
- Excluir mensagem do remetente.
