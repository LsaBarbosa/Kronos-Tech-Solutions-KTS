package com.kts.kronos.constants;

import com.kts.kronos.domain.model.StatusRecord;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Messages {
    private Messages() {
    }

    // Not found
    public static final String USER_NOT_FOUND = "Usuário não encontrado";
    public static final String RECORD_NOT_FOUND = "TimeRecord não encontrado: ";
    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada: ";
    public static final String ZIPCODE_NOT_FOUND = "CEP não encontrado:";
    public static final String EMPLOYEE_NOT_FOUND = "Colaborador não encontrado";
    public static final String DOCUMENT_NOT_FOUND = "Documento não encontrado";

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
    public static final String RECORD_NOT_BELONGS_EMPLOYEE = "Registro não pertence ao Colaborador informado";
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
}