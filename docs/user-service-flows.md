# Fluxos visuais — UserService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `createUser`

![Fluxo user.createUser](./flows/user/createUser.svg)

- Validar username único.
- Validar employeeId existente.
- Gerar senha aleatória e hash.
- Criar User.
- Salvar.

## `getUserByUsername`

![Fluxo user.getUserByUsername](./flows/user/getUserByUsername.svg)

- Ler empresa do usuário autenticado.
- Buscar usuário alvo por username.
- Se CTO: retorna.
- Se não CTO: validar empresa do alvo.
- Retornar usuário.

## `getUserById`

![Fluxo user.getUserById](./flows/user/getUserById.svg)

- Aplicar regra de escopo (isWithEmployeeId).
- Buscar user por ID.
- Retornar usuário.

## `listUsers`

![Fluxo user.listUsers](./flows/user/listUsers.svg)

- Ler empresa do usuário autenticado.
- Listar usuários (todos/por active).
- Se CTO: retorna todos.
- Se não CTO: filtrar por empresa.
- Retornar lista.

## `updateUser`

![Fluxo user.updateUser](./flows/user/updateUser.svg)

- Buscar usuário existente.
- Mesclar username/role/active.
- Validar política de senha (se enviada).
- Hash da nova senha (se enviada).
- Salvar.

## `deleteUser`

![Fluxo user.deleteUser](./flows/user/deleteUser.svg)

- Buscar usuário.
- Excluir documentos do employee.
- Excluir registros de ponto.
- Excluir usuário.
- Excluir employee.

## `toggleActivate`

![Fluxo user.toggleActivate](./flows/user/toggleActivate.svg)

- Buscar usuário.
- Inverter active do usuário.
- Salvar usuário.
- Sincronizar toggle no employee.

## `changeOwnPassword`

![Fluxo user.changeOwnPassword](./flows/user/changeOwnPassword.svg)

- Ler userId do token.
- Validar senha atual.
- Validar confirmação.
- Validar política.
- Salvar novo hash.

## `getOwnProfile`

![Fluxo user.getOwnProfile](./flows/user/getOwnProfile.svg)

- Ler userId do token.
- Buscar usuário.
- Retornar perfil.

## `usernameExists`

![Fluxo user.usernameExists](./flows/user/usernameExists.svg)

- Consultar username em minúsculo.
- Retornar boolean.
