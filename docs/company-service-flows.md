# Fluxos visuais — CompanyService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `createCompany`

![Fluxo company.createCompany](./flows/company/createCompany.svg)

- Validar CNPJ único.
- Buscar endereço via CEP.
- Montar Company.
- Salvar.

## `getCompany`

![Fluxo company.getCompany](./flows/company/getCompany.svg)

- Buscar empresa por CNPJ.
- Contar colaboradores ativos.
- Contar colaboradores inativos.
- Retornar Company com contadores.

## `listCompanies`

![Fluxo company.listCompanies](./flows/company/listCompanies.svg)

- Listar empresas (todas/por active).
- Para cada empresa contar ativos/inativos.
- Retornar lista mapeada.

## `getCompanyNameById`

![Fluxo company.getCompanyNameById](./flows/company/getCompanyNameById.svg)

- Buscar empresa por ID.
- Retornar nome.

## `updateCompany`

![Fluxo company.updateCompany](./flows/company/updateCompany.svg)

- Buscar empresa por CNPJ.
- Se alterar endereço: exigir geolocalização.
- Atualizar endereço via CEP.
- Mesclar campos.
- Salvar.

## `toggleActivate`

![Fluxo company.toggleActivate](./flows/company/toggleActivate.svg)

- Buscar empresa.
- Inverter active da empresa.
- Salvar empresa.
- Listar employees da empresa.
- Sincronizar status dos usuários.

## `deleteByCnpj`

![Fluxo company.deleteByCnpj](./flows/company/deleteByCnpj.svg)

- Validar existência da empresa.
- Excluir por CNPJ.

## `cnpjExists`

![Fluxo company.cnpjExists](./flows/company/cnpjExists.svg)

- Consultar CNPJ no provider.
- Retornar boolean.
