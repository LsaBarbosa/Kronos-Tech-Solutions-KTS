package com.kts.kronos.constants;

import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class Messages {
    private Messages() {
    }

    // --- FORMATAÇÃO DE DATA/HORA (Padronizada) ---
    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    public static final String DATE_PATTERN = "dd-MM-yyyy";
    public static final String DATE_TIME_PATTERN = "dd-MM-yy 'T' HH:mm:ss";
    public static final LocalDateTime TIME_ZONE_BRAZIL = LocalDateTime.now(SAO_PAULO);

    // Formatadores reutilizáveis
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

    // Time Record / Validation
    public static final String RECORD_NOT_FOUND = "TimeRecord não encontrado: ";
    public static final String RECORD_NOT_BELONGS_EMPLOYEE = "Desculpe!, Você não é o proprietário do registro. Somente o proprietário pode solicitar atualização de registro";
    public static final String MANAGER_DIFFERENT_COMPANY = "O manager não pertence à mesma empresa.";
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
    public static final String AWAITING_APPROVAL = "O status do registro não pode ser alterado, pois está aguardando aprovação.";
    public static final String ALREADY_UPDATED = "O status do registro não pode ser alterado, pois o registro foi atualizado após uma solicitção.";
    public static final String ROLE_IS_NOT_MANAGER = "O usuário informado não é um manager.";
    public static final String START_DATE_BIGGER_THAN_END_DATE = "A data de início das férias não pode ser posterior à data de fim.";
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
    public static final String PROOF_PDF = "comprovante_%d_%s_%s.pdf";
    public static final String EXIT = "SAIDA";
    public static final String CHECKOUT = "CHECKOUT";
    public static final String CHECKIN = "CHECKIN";
    public static final String CHECKIN_ON_DAY_OFF = "CHECKIN_ON_DAY_OFF";
    public static final String CHECKIN_AFTER_BREAK = "CHECKIN_AFTER_BREAK";
    public static final String PENDING_STATUS = "PENDING";
    public static final String APPROVED_STATUS = "APPROVED";
    public static final String REJECTED_STATUS = "REJECTED";
    public static final String RECORD_IS_NOT_AWAITING_APPROVAL = "O registro não está aguardando aprovação.";
    public static final String COMPANY_NOT_FOUND_FOR_THE_EMPLOYEE = "Empresa não encontrada para o funcionário.";
    public static final String ADDRESS_COMPANY_IS_NOT_REGISTERED = "A localização da empresa não está cadastrada.";
    public static final String NEW_REGISTER_OVERRIDES_AN_EXISTING_WORK_RECORD = "O novo horário se sobrepõe a um registro de trabalho existente (";

    // Documents
    public static final String DOCUMENT_NOT_FOUND = "Documento não encontrado";
    public static final String INVALID_DOCUMENT_TYPE = "Somente arquivos de texto ou imagem são aceitos no sistema (.pdf, .jpg, .jpeg, .png, .docx, .doc).";
    public static final String NOT_ABLE_TO_READ_FILE = "Não foi possível ler o arquivo: ";
    public static final String DOCUMENT_NOT_BELONGS_EMPLOYEE = "Documento não pertence ao Colaborador";
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
    public static final String MUST_HAVE_8_CHARACTERES = "Deve ter exatamente 8 dígitos";
    public static final String MUST_HAVE_11_CHARACTERES = "Deve ter exatamente 11 dígitos";
    public static final String MUST_HAVE_14_CHARACTERES = "Deve ter exatamente 14 dígitos";
    public static final String MUST_HAVE_50_CHARACTERES = "Deve ter até 50 dígitos";
    public static final String MUST_HAVE_200_CHARACTERES = "Deve ter até 200 dígitos";
    public static final String INVALID_ROLE = "Role inválida";
    public static final String INVALID_EMAIL_FORMAT = "O deve ter o formato correto: 'email@dominio.com' ";
    public static final String INTERNAL_SERVER_ERROR = "Erro inesperado";
    public static final String FAILURE_TO_GENERATE_AFD = "Falha crítica na geração do arquivo AFD";
    public static final String FAILURE_TO_GENERAT_AEJ = "Falha ao gerar arquivo fiscal AEJ: ";
    public static final String ERROR_TO_GENERATE_HASH = "Erro ao calcular Hash SHA-256";
    public static final String ERROR_TO_GENERATE_PDF = "Falha na geração do Termo PDF: ";


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

    public static final DateTimeFormatter S3_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // --- PDF CONTENT CONSTANTS (MAGIC STRINGS REMOVED) ---
    public static final String TITLE_TEXT = "TERMO DE CONSENTIMENTO PARA\nTRATAMENTO DE DADOS BIOMÉTRICOS";
    public static final String LABEL_EMPLOYER = "EMPREGADOR:";
    public static final String LABEL_EMPLOYEE = "COLABORADOR:";
    public static final String LEGAL_PARAGRAPH = "O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, especificamente sua IMAGEM FACIAL (Biometria), para a finalidade exclusiva de REGISTRO E CONTROLE DE JORNADA DE TRABALHO, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e a Portaria 671/2021 do Ministério do Trabalho e Previdência.";
    public static final String BULLET_PURPOSE = "FINALIDADE: Autenticação segura da identidade no momento do registro de ponto eletrônico, prevenindo fraudes.";
    public static final String BULLET_STORAGE = "ARMAZENAMENTO: Os dados serão armazenados em ambiente seguro de computação em nuvem (SaaS) provido pela KRONOS TECH SOLUTIONS.";
    public static final String BULLET_REVOCATION = "REVOGAÇÃO: Este consentimento poderá ser revogado a qualquer momento pelo Titular, mediante solicitação expressa ao departamento de Recursos Humanos.";
    public static final String BOX_HEADER_TEXT = "REGISTRO DE ACEITE ELETRÔNICO (Assinatura Eletrônica Avançada)";
    public static final String BOX_BODY_DISCLAIMER = "Este documento foi assinado digitalmente através da plataforma KRONOS, garantindo autenticidade e integridade conforme MP 2.200-2/2001.";
    public static final String LABEL_DATE = "Data/Hora do Aceite: ";
    public static final String LABEL_IP = "Endereço IP de Origem: ";
    public static final String LABEL_DEVICE = "Dispositivo/Navegador: ";
    public static final String LABEL_USER_ID = "ID Único do Usuário: ";
    public static final String LABEL_HASH = "\nCÓDIGO DE VALIDAÇÃO (HASH SHA-256):";
    public static final String FOOTER_TEXT = "Kronos Tech Solutions - Tecnologia em Gestão de Ponto";

    public static final String UNKNOWN_IP = "IP Não Identificado";
    public static final String UNKNOWN_DEVICE = "Dispositivo Desconhecido";
    public static final String UNKNOWN_USER_AGENT = "Desconhecido";
    public static final String BULLET_SYMBOL = "\u2022";

    public static final String DOCUMENT_ACCESS_DENIED = "Você não possui permissão para acessar este documento.";
    public static final String ERROR_FETCHING_STORAGE = "Falha ao buscar o arquivo no storage: ";
    public static final String ONLY_OWNER = "Apenas o proprietário exclui justificativas de abono.";

    public static final String GEOLOCATION_REQUIRED = "Location (latitude e longitude) é obrigatória se o endereço for alterado.";

    public static final String RESET_PASSWORD_SUBJECT = "🔒 Kronos Suporte - Redefinição de Senha";
    public static final String FACE_IMAGE_REQUIRED = "A imagem da face é obrigatória";

    // --- Security / Filters ---
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String RESPONSE_UNAUTHORIZED_TEMPLATE = "{\"status\":401,\"title\":\"Não autorizado\",\"detail\":\"%s\"}";
    public static final String JWT_INVALID_OR_EXPIRED = "Token JWT inválido ou expirado.";
    public static final String JWT_USER_NOT_FOUND = "Usuário do token não encontrado.";

    public static final String TERMS_NOT_ACCEPTED_TYPE = "TERMS_NOT_ACCEPTED";
    public static final String TERMS_NOT_ACCEPTED_DETAIL = "Aceite os termos para continuar.";
    public static final String TERMS_SYSTEM_URL = "https://termo.kronossolutions.tech/";
    public static final String TERMS_NOT_ACCEPTED_RESPONSE_TEMPLATE =
            "{\"type\":\"%s\",\"redirect_url\":\"%s\",\"detail\":\"%s\"}";

    public static final String AUTH_LOGIN_PATH = "/auth/login";
    public static final String AUTH_LOGIN_FACE_PATH = "/auth/login-face";
    public static final String AUTH_RECOVER_PASSWORD_PATH = "/auth/recover-password";
    public static final String AUTH_RESET_PASSWORD_PATH = "/auth/reset-password";
    public static final String TERMS_ACCEPT_BIOMETRIC_PATH = "/terms/accept-biometric";
    public static final String TERMS_STATUS_PATH = "/terms/status";
    public static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    public static final String ACTUATOR_INFO_PATH = "/actuator/info";
    public static final String API_DOCS_PREFIX = "/v3/api-docs";
    public static final String SWAGGER_UI_PREFIX = "/swagger-ui";

    public static final String MASKED_EMAIL_UNAVAILABLE = "indisponível";
    public static final String MASKED_VALUE = "***";


    public static final String RESET_PASSWORD_HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html lang='pt-BR'>
        <head>
        <meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        </head>
        <body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; text-align: center;'>

        <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); text-align: left;'>

            <h1 style='color: #1a73e8; font-size: 26px; border-bottom: 2px solid #eee; padding-bottom: 10px;'>👋 Olá, %s! Sua Segurança é Nossa Prioridade!</h1>

            <p style='font-size: 18px; color: #333;'>Esperamos que esteja tudo bem.</p>
        
            <p style='font-size: 16px; color: #333;'> Recebemos uma solicitação para redefinir a senha da sua conta.</p>
            <p style='font-size: 16px; color: #333;'>Para prosseguir e criar uma nova senha, é só clicar no botão azul logo abaixo. Rápido e fácil!</p>

            <p style='margin: 30px 0; text-align: center;'>
            <a href='%s' target='_blank' style='
            display: inline-block;
            padding: 15px 30px;
            background-color: #1a73e8;
            color: #ffffff;
            text-decoration: none;
            border-radius: 50px;
            font-size: 18px;
            font-weight: bold;
            border: 1px solid #1a73e8;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
            '>🔒 CLIQUE AQUI PARA NOVA SENHA</a>
            </p>

            <div style='background-color: #fff3e0; border-left: 5px solid #ff9900; padding: 15px; margin-top: 25px; border-radius: 4px;'>
                <p style='font-size: 15px; color: #ff9900; margin: 0;'>
                &#x26A0;&#xFE0F; Atenção: Este link de redefinição é sensível ao tempo e expira em 30 minutos por motivos de segurança.
                </p>
            </div>
            
            <p style='font-size: 14px; color: #666; text-align: center; margin-top: 20px;'>Se o botão não funcionar, copie e cole o link abaixo em seu navegador:<br>
            <a href='%s' style='color: #1a73e8; word-break: break-all;'>%s</a></p>

            <p style='font-size: 14px; color: #1e8449; margin-top: 30px; border-top: 1px solid #eee; padding-top: 15px; text-align: center;'>
            Não solicitou esta redefinição? Relaxe! 
            <p style='font-size: 14px; color: #1e8449; margin-top: 30px; border-top: 1px solid #eee;'>
            Se você não fez esta solicitação, pode simplesmente ignorar este e-mail. Sua senha antiga permanecerá segura e nenhuma alteração será feita na sua conta.
            </p>

            <p style='font-size: 16px; color: #333; margin-top: 40px;'>Conte sempre conosco para manter sua conta segura!</p>
            <p style='font-size: 14px; color: #555;'>Atenciosamente,<br>Time de Suporte Kronos Solutions</p>
        </div>
        </body>
        </html>
        """;

}
