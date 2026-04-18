package com.kts.kronos.application.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message == null || message.isBlank()
                ? "Sem permissão para utilizar esse recurso"
                : message);
    }
}
