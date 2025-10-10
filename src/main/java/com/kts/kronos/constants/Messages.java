package com.kts.kronos.constants;

import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Messages {
    private Messages() {
    }
    // Roles
    public static final String KRONOS = "hasRole('CTO')";
    public static final String ANY_EMPLOYEE = "hasAnyRole('MANAGER', 'PARTNER','CTO')";
    public static final String MANAGER = "hasRole('MANAGER')";
    public static final String ADMINISTRATOR = "hasAnyRole('MANAGER', 'CTO')";
    // Not found
    public static final String USER_NOT_FOUND = "Usuário não encontrado";
    public static final String RECORD_NOT_FOUND = "TimeRecord não encontrado: ";
    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada: ";
    public static final String ZIPCODE_NOT_FOUND = "CEP não encontrado:";
    public static final String EMPLOYEE_NOT_FOUND = "Colaborador não encontrado";
    public static final String DOCUMENT_NOT_FOUND = "Documento não encontrado";
    public static final String JWT_USER_ID_NOT_FOUND = "JWT sem userId.";
    public static final String JWT_EMPLOYEE_ID_NOT_FOUND = "JWT sem employeeId.";
    public static final String HEADER_AUTHORIZATION_NOT_FOUND = "Token JWT não encontrado no header Authorization.";
    public static final String PARTNER_NOT_FOUND = "Funcionário (parceiro) não encontrado: ";
    public static final String USER_MANAGER_NOT_FOUND = "Usuário (manager) não encontrado: ";
    public static final String MANAGER_NOT_FOUND = "Funcionário (manager) não encontrado: ";
    public static final String MESSAGE_NOT_FOUND = "Mensagem não encontrada";
    public static final String ONLY_MANAGER_CAN_DELETE_MESSAGE = "Apenas o Manager que enviou a mensagem pode deletá-la";


    // Exists
    public static final String CPF_ALREADY_EXIST = "CPF já cadastrado";
    public static final String COMPANY_ALREADY_EXIST = "Empresa já cadastrada";
    public static final String USERNAME_ALREADY_EXIST = "Username já existe";

    // Characteres
    public static final String MUST_HAVE_8_CHARACTERES = "Deve ter exatamente 8 dígitos";
    public static final String MUST_HAVE_11_CHARACTERES = "Deve ter exatamente 11 dígitos";
    public static final String MUST_HAVE_14_CHARACTERES = "Deve ter exatamente 14 dígitos";
    public static final String MUST_HAVE_50_CHARACTERES = "Deve ter até 50 dígitos";
    public static final String MUST_HAVE_200_CHARACTERES = "Deve ter até 200 dígitos";

    // Invalid Format
    public static final String INVALID_ROLE = "Role inválida";
    public static final String INVALID_FORMAT = "Formato inválido: use HH:mm";
    public static final String INVALID_EMAIL_FORMAT = "O deve ter o formato correto: 'email@dominio.com' ";
    public static final String INVALID_PASSWORD = "Senha atual incorreta.";
    public static final String INVALID_CONFIRM_PASSWORD = "Confirmação de senha não confere.";
    public static final String INVALID_PASSWORD_POLICY = "Política de senha inválida: mínimo de 8 caracteres, deve conter maiúscula, minúscula e dígito.";
    public static final String INVALID_DOCUMENT_TYPE = "Somente arquivos de texto ou imagem são aceitos no sistema (.pdf, .jpg, .jpeg, .png, .docx, .doc).";
    public static final String INVALID_PASSWORD_RESET_TOKEN = "Token de recuperação inválido ou expirado.";


    // Not Blank
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

    // Not Null
    public static final String DATE_NOT_NULL = "Data do registro é obrigatória";
    public static final String TIME_NOT_NULL = "Hora de início é obrigatória";

    // Belongs
public static final String RECORD_NOT_BELONGS_EMPLOYEE = "Desculpe!, Você não é o proprietário do registro. Somente o prorpietário pode solicitar atualização de registro";
    public static final String DOCUMENT_NOT_BELONGS_EMPLOYEE = "Documento não pertence ao Colaborador";

    //500
    public static final String INTERNAL_SERVER_ERROR = "Erro inesperado";

    // TIME
    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    public static final String DATE_PATTERN = "dd-MM-yyyy";
    public static final String DATE_TIME_PATTERN = "dd-MM-yy 'T' HH:mm:ss";
    public static final LocalDateTime TIME_ZONE_BRAZIL = LocalDateTime.now(SAO_PAULO);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final String DATE_TIME = "dd/MM/yyyy 'às' HH:mm";

    // Time exceptions
    public static final String HOURS_EXCEPTIONS = "Hora de início deve ser menor ou igual a hora de saída";
    public static final String CHECKIN_EXCEPTION = "Realize a saída antes de realizar uma nova entrada";
    public static final String CHECKOUT_EXCEPTION = "Realize a entrada antes de realizar uma nova saída";
    public static final String FUTURE_TIME_EXCEPTION = "Não é possível usar data/hora futura";
    public static final String SALARY_MUST_BE_POSITIVE = "Salário deve ser positivo";
    //Status
    public static final String STATUS_CHECKOUT = "Só é possível fazer checkout de um registro PENDING (atual=";
    public static final String STATUS_UPDATE = "Só é possível editar um registro CREATED/UPDATED (atual=";
    public static final StatusRecord CREATED = StatusRecord.CREATED;
    public static final StatusRecord UPDATED = StatusRecord.UPDATED;
    public static final StatusRecord DAY_OFF = StatusRecord.DAY_OFF;
    public static final StatusRecord ABSENCE = StatusRecord.ABSENCE;
    public static final StatusRecord PENDING = StatusRecord.PENDING;
    public static final StatusRecord DOCTOR_APPOINTMENT = StatusRecord.DOCTOR_APPOINTMENT;

    //RabbitMQ & REDIS
    public static final String TIME_RECORD_APPROVAL_TOPIC = "time-record-approval-topic";
    public static final String TIME_RECORD_APPROVAL_SUBSCRIPTION = "time-record-approval-subscription";
    public static final String APPROVAL_KEY_PREFIX = "timerecord:approval:";
    public static final String PASSWORD_RESET_TOPIC = "password-reset-topic";
    public static final String PASSWORD_RESET_KEY_PREFIX = "password:reset:";
    public static final String PASSWORD_RESET_SUBSCRIPTION = "password-reset-subscription";

    // **
    public static final String NOT_ABLE_TO_READ_FILE = "Não foi possível ler o arquivo: ";
    public static final String EMAIL_CONTENT = "<!DOCTYPE html>" +
            "<html lang='pt-BR'>" +
            "<head><meta charset='UTF-8'></head>" +
            "<body style='font-family: Arial, sans-serif; color: #333; line-height: 1.6;'>" +

            // APRESENTAÇÃO
            "<h1 style='color: #0056b3; font-size: 24px;'>Olá, %s! Sua Segurança é Nossa Prioridade.</h1>" +
            "<hr style='border: 0; border-top: 1px solid #eee;'>" +

            // CORPO
            "<p style='font-size: 16px;'>Recebemos uma solicitação para **redefinir a senha** da sua conta.</p>" +
            "<p style='font-size: 16px;'>Para criar uma nova senha, clique no botão abaixo. Se você não consegue clicar, copie e cole o link no seu navegador.</p>" +

            // DESTAQUE E BOTÃO
            "<p style='margin: 25px 0;'>" +
            // Botão com fundo azul, texto branco, fonte maior e negrito
            "<a href='%s' target='_blank' style='" +
            "display: inline-block; " +
            "padding: 12px 25px; " +
            "background-color: #007bff; " +
            "color: #ffffff; " +
            "text-decoration: none; " +
            "border-radius: 5px; " +
            "font-size: 18px; " +
            "font-weight: bold;" +
            "'>Criar Nova Senha</a>" +
            "</p>" +

            // INFORMAÇÃO DE EXPIRAÇÃO (ALERTA EM LARANJA)
            "<p style='font-size: 14px; color: #ff9900; margin-top: 20px;'>" +
            "**Lembre-se:** Este link de redefinição **expira em 30 minutos** por questões de segurança." +
            "</p>" +

            // CLÁUSULA DE SEGURANÇA
            "<p style='font-size: 14px; color: #666; margin-top: 30px;'>" +
            "Se você **não solicitou** a redefinição de senha, por favor, **ignore** este e-mail. Nenhuma ação será tomada na sua conta." +
            "</p>" +

            // FECHAMENTO
            "<p style='font-size: 14px; margin-top: 40px;'>Atenciosamente,<br>O Time de Suporte [Nome da Sua Empresa]</p>" +

            "</body>" +
            "</html>";
}
