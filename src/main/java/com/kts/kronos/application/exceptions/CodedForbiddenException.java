package com.kts.kronos.application.exceptions;

public class CodedForbiddenException extends ForbiddenException {
    private final String code;

    public CodedForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
