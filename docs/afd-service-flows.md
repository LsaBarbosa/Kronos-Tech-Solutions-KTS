# Fluxos visuais — AfdService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `logMarking`

![Fluxo afd.logMarking](./flows/afd/logMarking.svg)

- Receber dados da marcação (empresa/colaborador/data/nsr).
- Montar linha fiscal AFD.
- Persistir entrada AFD.

## `writeAfdToStream`

![Fluxo afd.writeAfdToStream](./flows/afd/writeAfdToStream.svg)

- Buscar registros AFD da empresa.
- Ordenar e formatar linhas.
- Escrever conteúdo no OutputStream.
