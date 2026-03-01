package com.kts.kronos.application.exceptions;

import static com.kts.kronos.constants.ExceptionMessages.FORBIDDEN_RESOURCE_ACCESS;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(FORBIDDEN_RESOURCE_ACCESS);
    }
}
