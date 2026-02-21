package com.kts.kronos.constants;

public class Logs {
    private Logs() {
    }
    public static final String ERR_CHECKOUT_STATUS = "Não é possível realizar checkout. Status atual: ";

    // Sucesso (Templates para String.format ou concatenação controlada)
    public static final String MSG_CHECKOUT = "Saída às %s! (NSR: %s)";
    public static final String MSG_CHECKIN = "Entrada às %s! (NSR: %s)";
    public static final String MSG_CHECKIN_GAP = "Entrada após pausa às %s! (NSR: %s)";
    public static final String MSG_CHECKIN_DAYOFF = "Registro de folga convertido para trabalho às %s! (NSR: %s)";

    // Logs
    public static final String LOG_START_REQ = "Iniciando registro de ponto. EmployeeId: {}, Geo: [{}, {}]";
    public static final String LOG_VALIDATION_OK = "Validações de segurança (Biometria/Geo) concluídas para EmployeeId: {}";
    public static final String LOG_CHECKOUT_ATTEMPT = "Tentativa de Checkout detectada. Registro Aberto ID: {}";
    public static final String LOG_CHECKOUT_SUCCESS = "Checkout realizado com sucesso. ID: {}, NSR: {}, Hora: {}";
    public static final String LOG_CHECKOUT_IGNORE = "Registro aberto ID: {} ignorado (Data diferente da atual). Iniciando fluxo de Check-in.";
    public static final String LOG_CHECKIN_CONVERT = "Convertendo registro de FOLGA/FALTA (ID: {}) para TRABALHO. NSR: {}";
    public static final String LOG_BREAK_DETECTED = "Pausa implícita detectada e registrada. Início: {}, Fim: {}";
    public static final String LOG_CHECKIN_SUCCESS = "Check-in realizado com sucesso. Novo ID: {}, NSR: {}, Tipo: {}";
    public static final String INVALID_CHECKOUT = "Tentativa inválida de checkout. Status atual: {}";
    public static final String ERR_TIME_INCONSISTENCY = "O horário final não pode ser anterior ao inicial no mesmo dia.";
    public static final String ERR_MANAGER_REQUIRED = "ID do gestor é obrigatório para esta operação.";
    public static final String ERR_MANAGER_NOT_FOUND = "Gestor não encontrado na base de usuários.";
    public static final String ERR_USER_NOT_MANAGER = "O usuário informado não possui perfil de Gestor.";
    public static final String ERR_MANAGER_DIFF_COMPANY = "O gestor pertence a uma empresa diferente.";
    public static final String ERR_UNAUTHORIZED_ROLE = "Perfil de usuário não autorizado para esta operação.";

    // Logs de Atualização
    public static final String LOG_UPDATE_REQ = "Solicitação de atualização de ponto recebida. RecordID: {}, UserRole: {}";
    public static final String LOG_PARTNER_APPROVAL = "Alteração enviada para aprovação. Employee: {}, Manager: {}";
    public static final String LOG_MANAGER_UPDATE = "Alteração direta realizada por Gestor. RecordID: {}";
    public static final String LOG_DATE_VALIDATION_ERR = "Tentativa de atualização com datas inconsistentes. RecordID: {}";

    // Erros de Aprovação
    public static final String ERR_APPROVAL_REQ_NOT_FOUND = "Solicitação de aprovação não encontrada para o registro ID: ";

    // Logs de Fluxo de Aprovação
    public static final String LOG_APPROVAL_START = "Iniciando processo de aprovação para o registro ID: {}";
    public static final String LOG_APPROVAL_NOT_FOUND = "Falha na aprovação: Solicitação não encontrada para o registro ID: {}";
    public static final String LOG_ADJUSTING_ADJACENT = "Ajustando registros adjacentes. EmployeeID: {}, RecordID: {}";
    public static final String LOG_APPROVAL_CLEANUP = "Limpeza: Dados de solicitação removidos da tabela de aprovação para o registro ID: {}";
    public static final String LOG_APPROVAL_SUCCESS = "Solicitação APROVADA com sucesso. RecordID: {}, EmployeeID: {}";

    // Logs de Rejeição
    public static final String LOG_REJECT_START = "Iniciando processo de REJEIÇÃO de ajuste. RecordID: {}";
    public static final String LOG_REJECT_SUCCESS = "Solicitação REJEITADA com sucesso. O registro retornou ao estado original. RecordID: {}";
    public static final String LOG_REJECT_VALIDATION = "Validação: Solicitação de aprovação pendente localizada para RecordID: {}";

    // Logs de Exclusão
    public static final String LOG_DELETE_INIT = "Solicitação de EXCLUSÃO recebida. RecordID: {}, EmployeeUUID: {}";
    public static final String LOG_DELETE_VALIDATION = "Validação: Registro pertence ao funcionário e está em status permitível. Status: {}";
    public static final String LOG_DELETE_DEPENDENCIES = "Limpando dependências: Removendo solicitações de aprovação vinculadas ao RecordID: {}";
    public static final String LOG_DELETE_SUCCESS = "Registro excluído permanentemente com sucesso. RecordID: {}, Data Original: {}";

    // Erros de Exclusão
    public static final String ERR_DELETE_CLOSED_RECORD = "Operação negada: Não é permitido excluir registros já fechados ou processados (Status: %s).";
    public static final String DELETE_BLOCKED = "Tentativa de exclusão de registro bloqueado. RecordID: {}, Status: {}";

    // Logs de Alternância de Estado (Toggle)
    public static final String LOG_TOGGLE_INIT = "Iniciando alternância de ativação (Soft Delete/Restore). RecordID: {}, EmployeeUUID: {}";
    public static final String LOG_TOGGLE_SUCCESS = "Status do registro alterado com sucesso. RecordID: {}, Status do Registro Anterior Ativo: {}, Status do Registro Atual Ativo: {}";

    // Erros de Alternância
    public static final String ERR_TOGGLE_CLOSED = "Operação negada: Não é permitido inativar/ativar um registro já processado (Status: %s).";
    public static final String TOGGLE_BLOCKED = "Tentativa de alternância de ativação em registro bloqueado. RecordID: {}, Status: {}";

    // Logs de Atualização de Status
    public static final String LOG_UPDATE_STATUS_INIT = "Iniciando alteração manual de status. RecordID: {}, Novo Status Solicitado: {}";
    public static final String LOG_UPDATE_STATUS_IDEMPOTENT = "O status atual já é {}. Nenhuma alteração realizada para o RecordID: {}";
    public static final String LOG_UPDATE_STATUS_SUCCESS = "Status do registro alterado com sucesso. RecordID: {}, Transição: [{}] -> [{}]";

    // Erros de Validação de Status
    public static final String ERR_STATUS_CLOSED = "Operação negada: Não é possível alterar o status de um registro já fechado ou processado na folha.";
    public static final String UPDATE_STATUS_BLOCKED = "Tentativa de alterar status de um registro bloqueado (Pendente). RecordID: {}";
    public static final String RECORD_ALREADY_UPDATED_BLOCKED = "Tentativa de alterar status de um registro já atualizado. RecordID: {}";
    public static final String UPDATE_RECORD_CLOSED_BLOCKED = "Tentativa de alterar status de um registro fechado. RecordID: {}";

    // Logs de Relatórios
    public static final String LOG_REPORT_INIT = "Iniciando geração de relatório simples. TargetEmployeeID: {}, Datas Solicitadas: {}";
    public static final String LOG_REPORT_EMPTY = "Nenhum registro encontrado para o TargetEmployeeID: {} nas datas informadas.";
    public static final String LOG_REPORT_SUCCESS = "Relatório gerado com sucesso para TargetEmployeeID: {}. Dias processados: {}";

    // Erros de Relatórios
    public static final String ERR_INVALID_REFERENCE = "O formato da hora de referência é inválido. Esperado: HH:mm";
    public static final String PARSE_ERROR = "Erro ao fazer parse da referência de jornada: {}";

    // Logs do Relatório Detalhado (ListReport)
    public static final String LOG_LIST_REPORT_INIT = "Iniciando geração de relatório detalhado. TargetEmployeeID: {}, Datas Solicitadas: {}";
    public static final String LOG_LIST_REPORT_EMPTY_DATES = "Geração abortada: Nenhuma data fornecida para o TargetEmployeeID: {}";
    public static final String LOG_LIST_REPORT_FETCH_DOCS = "Buscando documentos em lote para {} registros.";
    public static final String LOG_LIST_REPORT_SUCCESS = "Relatório detalhado gerado com sucesso para TargetEmployeeID: {}. Registros processados: {}";

    // Logs de Listagem de Aprovações
    public static final String LOG_LIST_APPROVALS_INIT = "Iniciando listagem de aprovações pendentes. ManagerID: {}, CompanyID: {}, Página: {}";
    public static final String LOG_LIST_APPROVALS_BULK = "Realizando Bulk Fetching para {} solicitações de aprovação na página {}.";
    public static final String LOG_LIST_APPROVALS_WARN = "Inconsistência referencial: Dados omitidos para a aprovação do TimeRecordID: {} devido a vínculos ausentes (Employee, User ou Record).";
    public static final String LOG_LIST_APPROVALS_SUCCESS = "Listagem de aprovações concluída. Retornando {} registros para o ManagerID: {}";

    // Logs de Solicitação de Férias
    public static final String LOG_VACATION_REQ_INIT = "Iniciando solicitação de férias. EmployeeID: {}, Início: {}, Fim: {}";
    public static final String LOG_VACATION_VALIDATION_OK = "Validações de regras de negócio e conflitos de agenda aprovadas para EmployeeID: {}";
    public static final String LOG_VACATION_SUCCESS = "Solicitação de férias processada com sucesso. {} dias registrados para EmployeeID: {}";

    // Erros de Solicitação de Férias
    public static final String ERR_VACATION_INVALID_DATES = "A data de início das férias não pode ser posterior à data de fim.";
    public static final String ERR_VACATION_CONFLICT = "Conflito de agenda: Já existem registros de ponto no período solicitado.";
    public static final String ERROR_REQUEST_VACATION = "Tentativa de solicitar férias em período com registros existentes. EmployeeID: {}";

    // Logs de Aprovação de Férias
    public static final String LOG_VACATION_APPROVE_INIT = "Iniciando aprovação em lote de {} dias de férias.";
    public static final String LOG_VACATION_APPROVE_SUCCESS = "Aprovação em lote concluída. {} dias alterados para VACATION.";
    public static final String LOG_VACATION_APPROVE_INVALID = "Tentativa de aprovar registro (ID: {}) ignorada. Status inválido atual: {}";
    public static final String LOG_VACATION_APPROVE_MISMATCH = "Discrepância: {} IDs solicitados, mas apenas {} encontrados no banco.";

    // Erros de Aprovação de Férias
    public static final String ERR_ONLY_MANAGERS_CAN_GRANT = "Acesso negado: Apenas gestores (MANAGER) podem aprovar.";
    public static final String ERR_RECORDS_NOT_FOUND_BATCH = "Um ou mais registros solicitados não foram encontrados na base de dados.";

    // Logs de Rejeição de Férias
    public static final String LOG_VACATION_REJECT_INIT = "Iniciando rejeição em lote de {} dias de férias.";
    public static final String LOG_VACATION_REJECT_SUCCESS = "Rejeição em lote concluída. {} dias alterados para VACATION_REJECTED.";
    public static final String LOG_VACATION_REJECT_INVALID = "Tentativa de rejeitar registro (ID: {}) ignorada. Status atual não permite rejeição: {}";
    public static final String LOG_VACATION_REJECT_MISMATCH = "Discrepância na rejeição: {} IDs solicitados, mas apenas {} encontrados.";
    public static final String AUTHORIZATION_ERROR = "Tentativa de aprovação/rejeição de férias por perfil não autorizado: {}";

    // Logs de Listagem de Férias
    public static final String LOG_VACATION_LIST_INIT = "Iniciando listagem de solicitações de férias. CompanyID: {}, Filtro de Status: {}";
    public static final String LOG_VACATION_LIST_FETCH = "Buscando registros de férias em lote para {} funcionários.";
    public static final String LOG_VACATION_LIST_SUCCESS = "Listagem de férias concluída. {} períodos consolidados encontrados.";

    // Logs de Abono/Esquecimento
    public static final String LOG_TIME_OFF_INIT = "Iniciando solicitação de abono/esquecimento. EmployeeID: {}, Início: {}, Fim: {}";
    public static final String LOG_TIME_OFF_VALIDATION = "Validações de abono concluídas. Gerando {} registros.";
    public static final String LOG_TIME_OFF_DOC_LINK = "Documento anexado e vinculado ao registro base ID: {}. Replicando para os demais dias.";
    public static final String LOG_TIME_OFF_SUCCESS = "Solicitação processada com sucesso. {} registros gerados. Primeiro ID: {}";
    public static final String ERROR_SAVE_DOCUMENT = "Falha ao salvar o documento de abono para o registro {}: {}";
    public static final String DOCS_SAVED = "Replicados {} vínculos de documentos em lote com sucesso.";
    // Logs de Aprovação de Abono/Esquecimento
    public static final String LOG_TIME_OFF_APPROVE_INIT = "Iniciando aprovação de solicitação de abono/esquecimento. RecordID: {}";

    // Erros de Aprovação
    public static final String LOG_TIME_OFF_BATCH_MISMATCH = "Discrepância na busca: {} IDs solicitados, mas apenas {} encontrados no banco.";
    public static final String LOG_TIME_OFF_BATCH_INVALID = "Tentativa de aprovação ignorada. O registro ID: {} possui um status inválido para esta operação: {}";
    public static final String LOG_TIME_OFF_BATCH_SUCCESS = "Aprovação em lote concluída com sucesso. {} registros tiveram seus status atualizados.";

    public static final String LOG_TIME_OFF_REJECT_BATCH_INIT = "Iniciando rejeição em lote de abonos/esquecimentos. Quantidade de IDs: {}";
    public static final String LOG_TIME_OFF_REJECT_BATCH_INVALID = "Tentativa de rejeição ignorada. O registro ID: {} não está pendente. Status atual: {}";
    public static final String LOG_TIME_OFF_REJECT_BATCH_SUCCESS = "Rejeição em lote concluída com sucesso. {} registros foram negados.";

    public static final String LOG_TIME_OFF_LIST_INIT = "Iniciando listagem de abonos/esquecimentos. CompanyID: {}, Status: {}";
    public static final String LOG_TIME_OFF_LIST_FETCH = "Buscando registros em lote para {} funcionários filtrados.";
    public static final String LOG_TIME_OFF_LIST_PAGINATION = "Paginação calculada: Total de {} registros. Buscando documentos apenas para os {} itens da página atual.";
    public static final String LOG_TIME_OFF_LIST_SUCCESS = "Listagem de abonos concluída com sucesso. Página {}/{} retornada.";
}