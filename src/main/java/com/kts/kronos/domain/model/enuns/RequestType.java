package com.kts.kronos.domain.model.enuns;

public enum RequestType {
    TIME_OFF_REQUEST,          // Justificar ausência (Não conta como trabalho, abona a falta)
    FORGOTTEN_REGISTRATION    // Inserir ponto esquecido (Conta como horas trabalhadas)
}