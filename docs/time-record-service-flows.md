# Fluxos visuais — TimeRecordService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `registerTime`

![Fluxo time-record.registerTime](./flows/time-record/registerTime.svg)

- Validar horário do sistema (NTP).
- Identificar colaborador logado.
- Validar biometria e geolocalização.
- Verificar registro aberto do dia.
- Gerar NSR e salvar entrada/saída.
- Gerar auditoria AFD e comprovante.
- Retornar ActionResponse.

## `updateTimeRecord`

![Fluxo time-record.updateTimeRecord](./flows/time-record/updateTimeRecord.svg)

- Identificar role e colaborador.
- Buscar registro alvo.
- Validar pertencimento e horários.
- Se PARTNER: criar solicitação de aprovação.
- Se MANAGER/CTO: aplicar ajuste direto.
- Salvar mudanças de status/horas.

## `approveTimeRecordChange`

![Fluxo time-record.approveTimeRecordChange](./flows/time-record/approveTimeRecordChange.svg)

- Buscar registro pendente de aprovação.
- Carregar dados da solicitação.
- Ajustar registros adjacentes de pausa.
- Aplicar novos horários e status UPDATED.
- Salvar e remover solicitação.

## `rejectTimeRecordChange`

![Fluxo time-record.rejectTimeRecordChange](./flows/time-record/rejectTimeRecordChange.svg)

- Buscar registro pendente.
- Marcar status UPDATE_REJECTED.
- Salvar registro.
- Remover solicitação de aprovação.

## `deleteTimeRecord`

![Fluxo time-record.deleteTimeRecord](./flows/time-record/deleteTimeRecord.svg)

- Buscar colaborador e registro.
- Validar pertencimento do registro.
- Excluir registro.

## `toggleActivate`

![Fluxo time-record.toggleActivate](./flows/time-record/toggleActivate.svg)

- Buscar registro por colaborador e ID.
- Inverter campo active.
- Salvar registro.

## `updateStatus`

![Fluxo time-record.updateStatus](./flows/time-record/updateStatus.svg)

- Buscar registro.
- Validar que não está em PENDING_APPROVAL/UPDATED.
- Aplicar novo status.
- Salvar registro.

## `simpleReport`

![Fluxo time-record.simpleReport](./flows/time-record/simpleReport.svg)

- Validar escopo do colaborador alvo.
- Carregar registros válidos por datas.
- Agrupar registros por dia.
- Calcular horas trabalhadas/pausas/saldo.
- Montar e retornar SimpleReportResponse.

## `listReport`

![Fluxo time-record.listReport](./flows/time-record/listReport.svg)

- Validar escopo do colaborador alvo.
- Carregar registros no período solicitado.
- Calcular saldos diários.
- Anexar caminho de documento quando existir.
- Mapear para TimeRecordResponse.

## `listPendingApprovals`

![Fluxo time-record.listPendingApprovals](./flows/time-record/listPendingApprovals.svg)

- Identificar empresa do manager logado.
- Buscar aprovações paginadas por empresa.
- Montar DTO com dados de registro/funcionário/manager.
- Retornar TimeRecordApprovalPageResponse.

## `requestVacation`

![Fluxo time-record.requestVacation](./flows/time-record/requestVacation.svg)

- Validar manager e empresa.
- Validar intervalo de datas.
- Criar registros REQUEST_VACATION por dia.
- Validar duplicidade diária.
- Salvar e retornar IDs criados.

## `approveVacation`

![Fluxo time-record.approveVacation](./flows/time-record/approveVacation.svg)

- Validar role MANAGER/CTO.
- Percorrer IDs solicitados.
- Se status REQUEST_VACATION: mudar para VACATION.
- Salvar registros aprovados.

## `rejectVacation`

![Fluxo time-record.rejectVacation](./flows/time-record/rejectVacation.svg)

- Validar role MANAGER/CTO.
- Percorrer IDs solicitados.
- Se status REQUEST_VACATION: mudar para VACATION_REJECTED.
- Salvar registros rejeitados.

## `listVacationRequests`

![Fluxo time-record.listVacationRequests](./flows/time-record/listVacationRequests.svg)

- Obter empresa do usuário logado.
- Definir status alvo pelo filtro.
- Filtrar funcionários por nome (opcional).
- Carregar e consolidar períodos contínuos.
- Paginar e retornar respostas.

## `requestTimeOff`

![Fluxo time-record.requestTimeOff](./flows/time-record/requestTimeOff.svg)

- Validar colaborador, datas e horários.
- Validar manager da mesma empresa.
- Definir tipo/status inicial da solicitação.
- Criar registros de solicitação por período.
- Fazer upload do documento quando enviado.
- Salvar e retornar ID principal.

## `approveTimeOff`

![Fluxo time-record.approveTimeOff](./flows/time-record/approveTimeOff.svg)

- Buscar registro de solicitação.
- Validar status e tipo elegível.
- Atualizar status para aprovado (TIME_OFF/CREATED).
- Salvar registro.

## `rejectTimeOff`

![Fluxo time-record.rejectTimeOff](./flows/time-record/rejectTimeOff.svg)

- Buscar registro de solicitação.
- Validar status elegível.
- Atualizar status para rejeitado.
- Salvar registro.

## `listTimeOffRequests`

![Fluxo time-record.listTimeOffRequests](./flows/time-record/listTimeOffRequests.svg)

- Identificar empresa do usuário logado.
- Definir conjunto de status pelo filtro.
- Filtrar por nome do colaborador (opcional).
- Carregar e ordenar solicitações.
- Paginar e retornar TimeRecordPageResponse.
