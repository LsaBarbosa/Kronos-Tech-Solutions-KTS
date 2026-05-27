package com.kts.kronos.application.exceptions;

public class DryRunTokenInvalidException extends RuntimeException {
    public DryRunTokenInvalidException(String message) {
        super(message);
    }

    public DryRunTokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
