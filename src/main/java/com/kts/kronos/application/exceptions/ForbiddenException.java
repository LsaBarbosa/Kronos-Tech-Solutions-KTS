package com.kts.kronos.application.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super("Sem permissão para utilizar esse recurso");
    }
}
