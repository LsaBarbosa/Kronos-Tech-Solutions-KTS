# Fluxos visuais — EmployeeService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `createEmployee`

![Fluxo employee.createEmployee](./flows/employee/createEmployee.svg)

- Validar role (CTO/MANAGER).
- Resolver companyId.
- Validar CPF existente/orfão.
- Montar + salvar Employee.
- Registrar face (opcional).
- Retornar colaborador.

## `listEmployees`

![Fluxo employee.listEmployees](./flows/employee/listEmployees.svg)

- Obter companyId do usuário logado.
- Se active nulo: listar por empresa.
- Se active definido: filtrar por ativo.
- Retornar lista.

## `getEmployee`

![Fluxo employee.getEmployee](./flows/employee/getEmployee.svg)

- Obter companyId do usuário logado.
- Buscar colaborador por ID.
- Validar escopo da empresa.
- Retornar colaborador.

## `updateEmployee`

![Fluxo employee.updateEmployee](./flows/employee/updateEmployee.svg)

- Buscar colaborador alvo.
- Mesclar campos do request.
- Atualizar endereço via CEP (opcional).
- Atualizar face (opcional).
- Salvar colaborador.

## `deleteEmployee`

![Fluxo employee.deleteEmployee](./flows/employee/deleteEmployee.svg)

- Buscar colaborador alvo.
- Excluir por ID.
- Finalizar.

## `getOwnProfile`

![Fluxo employee.getOwnProfile](./flows/employee/getOwnProfile.svg)

- Ler employeeId do token.
- Buscar colaborador.
- Buscar usuário por employeeId.
- Retornar EmployeeProfile.

## `updateOwnProfile`

![Fluxo employee.updateOwnProfile](./flows/employee/updateOwnProfile.svg)

- Ler employeeId do token.
- Buscar colaborador.
- Atualizar endereço (opcional).
- Atualizar email/telefone.
- Salvar.

## `markMessagesAsSeen`

![Fluxo employee.markMessagesAsSeen](./flows/employee/markMessagesAsSeen.svg)

- Ler employeeId do token.
- Buscar colaborador.
- Atualizar lastSeenMessageTimestamp.
- Salvar.

## `cpfExists`

![Fluxo employee.cpfExists](./flows/employee/cpfExists.svg)

- Consultar cpfExists no provider.
- Retornar boolean.

## `toggleActivate`

![Fluxo employee.toggleActivate](./flows/employee/toggleActivate.svg)

- Buscar colaborador.
- Inverter status active.
- Salvar.
