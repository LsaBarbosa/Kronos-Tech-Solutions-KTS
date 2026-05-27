package com.kts.kronos.application.exceptions;

public class DryRunTokenExpiredException extends RuntimeException {
    public DryRunTokenExpiredException(String message) {
        super(message);
    }

    public DryRunTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
