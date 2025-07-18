package com.kts.kronos.constants;

import com.kts.kronos.domain.model.StatusRecord;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Messages {
    private Messages() {}

    //404
    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada: ";
    public static final String EMPLOYEE_NOT_FOUND = "Colaborador não encontrado";
    public static final String USER_NOT_FOUND = "Usuário não encontrado";
    public static final String DOCUMENT_NOT_FOUND = "Documento não encontrado";
    public static final String ZIPCODE_NOT_FOUND = "CEP não encontrado:";

    //400
    public static final String COMPANY_ALREADY_EXIST = "Empresa já cadastrada";
    public static final String DOCUMENT_AND_EMPLOYEE_NOT_SAME = "Documento não pertence ao Colaborador";
    public static final String USERNAME_ALREADY_EXIST = "Username já existe";
    public static final String CPF_ALREADY_EXIST = "CPF já cadastrado";

    //500
    public static final String INTERNAL_SERVER_ERROR = "Erro inesperado";

    // TIME
    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    public static final LocalDateTime TIME_ZONE_BRAZIL = LocalDateTime.now(SAO_PAULO);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final String CHECKIN_EXCEPTION = "Realize a saída antes de realizar uma nova entrada";
    public static final String IS_RECORD_BELONG_EMPLOYEE = "Registro não pertence ao Colaborador informado";
    public static final String RECORD_NOT_FOUND = "TimeRecord não encontrado: ";
    public static final String FUTURE_TIME_EXCEPTION = "Não é possível usar data/hora futura";
    public static final String HOURS_EXCEPTIONS = "Hora de início deve ser menor ou igual a hora de saída";

    //Status
    public static final StatusRecord CREATED = StatusRecord.CREATED;
    public static final StatusRecord UPDATED = StatusRecord.UPDATED;
    public static final StatusRecord DAY_OFF = StatusRecord.DAY_OFF;
    public static final StatusRecord DOCTOR_APPOINTMENT = StatusRecord.DOCTOR_APPOINTMENT;
    public static final StatusRecord ABSENCE = StatusRecord.ABSENCE;
    public static final StatusRecord PENDING = StatusRecord.PENDING;
}
