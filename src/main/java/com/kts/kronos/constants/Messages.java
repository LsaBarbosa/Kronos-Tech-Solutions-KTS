package com.kts.kronos.constants;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class Messages {
    private Messages() {}

    // --- FORMATAÇÃO DE DATA/HORA (Padronizada) ---
    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    public static final String DATE_PATTERN = "dd-MM-yyyy";
    public static final String DATE_TIME_PATTERN = "dd-MM-yy 'T' HH:mm:ss";
    public static final LocalDateTime TIME_ZONE_BRAZIL = LocalDateTime.now(SAO_PAULO);

    // Formatadores reutilizáveis
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");
    public static final DateTimeFormatter DATE_FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter AFD_DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyyHHmm");
    public static final DateTimeFormatter RECEIPT_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter GENERATION_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // --- ROLES ---
    public static final String KRONOS = "hasRole('CTO')";
    public static final String ANY_EMPLOYEE = "hasAnyRole('MANAGER', 'PARTNER','CTO')";
    public static final String MANAGER = "hasRole('MANAGER')";
    public static final String ADMINISTRATOR = "hasAnyRole('MANAGER', 'CTO')";

    // --- EXCEPTION MESSAGES (Novas e Existentes) ---
    // User / Auth
    public static final String USER_NOT_FOUND = "Usuário não encontrado";
    public static final String USERNAME_ALREADY_EXIST = "Username já existe";
    public static final String USER_INACTIVE = "Usuário inativo.";
    public static final String NO_USER_LINKED_TO_EMPLOYEE = "Nenhum usuário vinculado a este colaborador.";
    public static final String INVALID_PASSWORD = "Senha atual incorreta.";
    public static final String INVALID_CONFIRM_PASSWORD = "Confirmação de senha não confere.";
    public static final String INVALID_PASSWORD_POLICY = "Política de senha inválida: mínimo de 8 caracteres, deve conter maiúscula, minúscula e dígito.";
    public static final String INVALID_PASSWORD_RESET_TOKEN = "Token de recuperação inválido ou expirado.";
    public static final String FACE_NOT_RECOGNIZED = "Face não reconhecida ou não cadastrada.";
    public static final String INVALID_BASE64_IMAGE = "Imagem inválida (Base64 malformado).";
    public static final String FACE_MISMATCH = "Falha na validação facial: A face não corresponde ao colaborador autenticado.";

    // Employee / Company
    public static final String EMPLOYEE_NOT_FOUND = "Colaborador não encontrado";
    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada: ";
    public static final String COMPANY_ALREADY_EXIST = "Empresa já cadastrada";
    public static final String CPF_ALREADY_EXIST = "CPF já cadastrado";
    public static final String ZIPCODE_NOT_FOUND = "CEP não encontrado:";
    public static final String LOCATION_REQUIRED_ON_ADDRESS_CHANGE = "Location (latitude e longitude) é obrigatório se o endereço for alterado.";
    public static final String USER_ALREADY_LINKED_TO_EMPLOYEE = "Já existe um usuário vinculado a este colaborador.";
    public static final String EMPLOYEE_HAS_LINKED_USER = "Não é possível excluir o colaborador enquanto existir um usuário vinculado. Exclua o usuário primeiro.";

    // Time Record / Validation
    public static final String RECORD_NOT_FOUND = "TimeRecord não encontrado: ";
    public static final String RECORD_NOT_BELONGS_EMPLOYEE = "Desculpe!, Você não é o proprietário do registro. Somente o proprietário pode solicitar atualização de registro";
    public static final String MANAGER_ID_REQUIRED = "O ID do manager é obrigatório para parceiros.";
    public static final String USER_IS_NOT_MANAGER = "O usuário informado não é um manager.";
    public static final String MANAGER_DIFFERENT_COMPANY = "O manager não pertence à mesma empresa.";
    public static final String UNAUTHORIZED_ROLE_OPERATION = "Role não autorizada para esta operação.";
    public static final String APPROVAL_REQUEST_NOT_FOUND = "Solicitação de aprovação não encontrada ou expirada para o registro: ";
    public static final String STATUS_CHANGE_PENDING_APPROVAL = "O status do registro não pode ser alterado, pois está aguardando aprovação.";
    public static final String STATUS_CHANGE_UPDATED = "O status do registro não pode ser alterado, pois o registro foi atualizado após uma solicitação.";
    public static final String VACATION_START_AFTER_END = "A data de início das férias não pode ser posterior à data de fim.";
    public static final String RECORD_ALREADY_EXISTS_FOR_DATE = "Já existe um registro de ponto ou solicitação para o dia: ";
    public static final String ONLY_MANAGER_APPROVE_VACATION = "Apenas Managers ou CTO podem aprovar solicitações de férias.";
    public static final String ONLY_MANAGER_REJECT_VACATION = "Apenas Managers ou CTO podem rejeitar solicitações de férias.";
    public static final String ONLY_OWNER_DELETE_TIME_OFF_DOCS = "Apenas proprietário do documento pode deletá-lo";
    public static final String GEOLOCATION_OUT_OF_RANGE = "Você está fora da área de trabalho permitida.";
    public static final String HOURS_EXCEPTIONS = "Hora de início deve ser menor ou igual a hora de saída";
    public static final String SALARY_MUST_BE_POSITIVE = "Salário deve ser positivo";
    public static final String STATUS_CHECKOUT = "Só é possível fazer checkout de um registro PENDING (atual=";
    public static final String STATUS_UPDATE = "Só é possível editar um registro CREATED/UPDATED (atual=";
    public static final String ERROR_GET_FILE = "Falha ao buscar o arquivo no storage: ";
    public static final String COMPANY_ID_IS_REQUIRED_TO_CREATE_FIRST_MANAGER = "O companyId é obrigatório para a criação de um colaborador por um CTO.";
    public static final String FAILURE_TO_SAVE_AUTO_GENERATED_DOC = "Falha ao salvar documento gerado automaticamente: ";
    public static final String INTERNAL_CLOCK_OUT_OF_SYNC = "Sistema temporariamente indisponível: Relógio interno dessincronizado.";
    public static final String ERROR_GENERATING_TECHNICAL_CERTIFICATE = "Falha na geração do Atestado Técnico";
    public static final String ADDRESS_NOT_REGISTERED = "Endereço não cadastrado";
    public static final String UNAUTHORIZED_ROLE = "Role não autorizada para esta operação.";
    public static final String REQUEST_NOT_FOUND = "Solicitação de aprovação não encontrada ou expirada para o registro: ";
    public static final String AWAITING_APPROVAL = "O status do registro não pode ser alterado, pois está aguardando aprovação.";
    public static final String ALREADY_UPDATED = "O status do registro não pode ser alterado, pois o registro foi atualizado após uma solicitção.";
    public static final String ROLE_IS_NOT_MANAGER = "O usuário informado não é um manager.";
    public static final String START_DATE_BIGGER_THAN_END_DATE = "A data de início das férias não pode ser posterior à data de fim.";
    public static final String ALREADY_REQUESTED = "Já existe um registro de ponto ou solicitação para o dia: ";
    public static final String ONLY_MANAGERS_CAN_GRANT_VACATION = "Apenas Managers ou CTO podem aprovar solicitações de férias.";
    public static final String ONLY_MANAGERS_CAN_REJECT_VACATION = "Apenas Managers ou CTO podem rejeitar solicitações de férias.";
    public static final String MANAGER_NOT_FOUND = "Manager não encontrado.";
    public static final String USER_NOT_IS_MANAGER = "O usuário informado não é um manager.";
    public static final String DOC_NOT_FOUND = "Documento não encontrado após upload para o 1º registro.";
    public static final String FAILED_TO_CREATE_FIRST_RECORD = "Falha ao criar o primeiro registro de abono.";
    public static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/msword");

    public static final String EXIT = "SAIDA";
    public static final String CHECKOUT = "CHECKOUT";
    public static final String CHECKIN = "CHECKIN";
    public static final String CHECKIN_ON_DAY_OFF = "CHECKIN_ON_DAY_OFF";
    public static final String CHECKIN_AFTER_BREAK = "CHECKIN_AFTER_BREAK";
    public static final String PENDING_STATUS = "PENDING";
    public static final String APPROVED_STATUS = "APPROVED";
    public static final String REJECTED_STATUS = "REJECTED";
    public static final String INVALID_RECORD = "O registro não é uma solicitação pendente válida (Status: ";
    public static final String RECORD_IS_NOT_AWAITING_APPROVAL = "O registro não está aguardando aprovação.";
    public static final String COMPANY_NOT_FOUND_FOR_THE_EMPLOYEE = "Empresa não encontrada para o funcionário.";
    public static final String ADDRESS_COMPANY_IS_NOT_REGISTERED = "A localização da empresa não está cadastrada.";
    public static final String NEW_REGISTER_OVERRIDES_AN_EXISTING_WORK_RECORD = "O novo horário se sobrepõe a um registro de trabalho existente (";

    // Documents
    public static final String DOCUMENT_NOT_FOUND = "Documento não encontrado";
    public static final String INVALID_DOCUMENT_TYPE = "Somente arquivos de texto ou imagem são aceitos no sistema (.pdf, .jpg, .jpeg, .png, .docx, .doc).";
    public static final String NOT_ABLE_TO_READ_FILE = "Não foi possível ler o arquivo: ";
    public static final String DOCUMENT_NOT_BELONGS_EMPLOYEE = "Documento não pertence ao Colaborador";
    public static final String IMAGE_DATA_NOT_BLANK = "A imagem da face é obrigatória para o registro de ponto";
    public static final String IMAGE_DATA_REGISTER_NOT_BLANK = "A imagem da face é obrigatória para o cadastro de referência.";
    public static final String NO_FACE_DETECTED = "Nenhuma face válida detectada na imagem fornecida. Tente novamente.";

    // Security / Headers
    public static final String JWT_USER_ID_NOT_FOUND = "JWT sem userId.";
    public static final String JWT_EMPLOYEE_ID_NOT_FOUND = "JWT sem employeeId.";
    public static final String HEADER_AUTHORIZATION_NOT_FOUND = "Token JWT não encontrado no header Authorization.";

    // Messages (Chat/Avisos)
    public static final String MESSAGE_NOT_FOUND = "Mensagem não encontrada";
    public static final String ONLY_MANAGER_CAN_DELETE_MESSAGE = "Apenas o Manager que enviou a mensagem pode deletá-la";
    public static final String CHOOSE_EMPLOYEE = "Necessário escolher os colaboradores que receberão o aviso";
    public static final String INVALID_EMPLOYEE = "Nenhum destinatário válido encontrado na sua empresa.";

    // Validation Constraints (NotBlank, etc) - Mantidos
    public static final String ID_NOT_BLANK = "ID é obrigatório";
    public static final String CPF_NOT_BLANK = "O CPF é obrigatório";
    public static final String CNPJ_NOT_BLANK = "O CNPJ é obrigatório";
    public static final String ROLE_NOT_BLANK = "Role é obrigatória";
    public static final String EMAIL_NOT_BLANK = "O Email é obrigatório";
    public static final String STATUS_NOT_BLANK = "Status é obrigatório";
    public static final String USERNAME_NOT_BLANK = "Username é obrigatório";
    public static final String PASSWORD_NOT_BLANK = "Senha é obrigatória";
    public static final String POSTAL_CODE_NOT_BLANK = "O CEP é obrigatório";
    public static final String COMPANY_NAME_NOT_BLANK = "O nome da empresa é obrigatório";
    public static final String JOB_POSITION_NOT_BLANK = "O cargo do colaborador é obrigatório";
    public static final String EMPLOYEE_NAME_NOT_BLANK = "O nome do colaborador é obrigatório";
    public static final String ADDRESS_NUMBER_NOT_BLANK = "O Número é obrigatório";
    public static final String DATE_NOT_NULL = "Data do registro é obrigatória";
    public static final String TIME_NOT_NULL = "Hora de início é obrigatória";
    public static final String MUST_HAVE_8_CHARACTERES = "Deve ter exatamente 8 dígitos";
    public static final String MUST_HAVE_11_CHARACTERES = "Deve ter exatamente 11 dígitos";
    public static final String MUST_HAVE_14_CHARACTERES = "Deve ter exatamente 14 dígitos";
    public static final String MUST_HAVE_50_CHARACTERES = "Deve ter até 50 dígitos";
    public static final String MUST_HAVE_200_CHARACTERES = "Deve ter até 200 dígitos";
    public static final String INVALID_ROLE = "Role inválida";
    public static final String INVALID_FORMAT = "Formato inválido: use HH:mm";
    public static final String INVALID_EMAIL_FORMAT = "O deve ter o formato correto: 'email@dominio.com' ";
    public static final String INTERNAL_SERVER_ERROR = "Erro inesperado";
    public static final String FAILURE_TO_GENERATE_AFD = "Falha crítica na geração do arquivo AFD";
    public static final String FAILURE_TO_GENERAT_AEJ = "Falha ao gerar arquivo fiscal AEJ: ";
    public static final String ERROR_TO_GENERATE_HASH = "Erro ao calcular Hash SHA-256";
    public static final String ERROR_TO_GENERATE_PDF = "Falha na geração do Termo PDF: ";
    public static final String END_DATE_BEFORE_START_DATE = "A data final não pode ser anterior à data inicial.";
    public static final String EXPORT_PERIOD_TOO_LARGE = "O período informado excede o limite permitido para exportação de %d dias.";


    // Enums de Status (Mantidos)
    public static final StatusRecord CREATED = StatusRecord.CREATED;
    public static final StatusRecord UPDATED = StatusRecord.UPDATED;
    public static final StatusRecord DAY_OFF = StatusRecord.DAY_OFF;
    public static final StatusRecord ABSENCE = StatusRecord.ABSENCE;
    public static final StatusRecord PENDING = StatusRecord.PENDING;
    public static final StatusRecord REQUEST_VACATION = StatusRecord.REQUEST_VACATION;
    public static final StatusRecord VACATION = StatusRecord.VACATION;
    public static final StatusRecord VACATION_REJECTED = StatusRecord.VACATION_REJECTED;
    public static final StatusRecord IMPLICIT_BREAK = StatusRecord.IMPLICIT_BREAK;
    public static final StatusRecord TIME_OFF = StatusRecord.TIME_OFF;
    public static final StatusRecord TIME_OFF_REQUEST = StatusRecord.TIME_OFF_REQUEST;
    public static final StatusRecord TIME_OFF_REJECTED = StatusRecord.TIME_OFF_REJECTED;
    public static final StatusRecord WORK_TIME_REQUEST = StatusRecord.WORK_TIME_REQUEST;
    public static final StatusRecord WORK_TIME_REJECTED = StatusRecord.WORK_TIME_REJECTED;
}
