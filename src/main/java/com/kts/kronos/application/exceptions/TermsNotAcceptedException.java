package com.kts.kronos.application.exceptions;

public class TermsNotAcceptedException extends ForbiddenException {
    private final String redirectUrl;

    public TermsNotAcceptedException(String message, String redirectUrl) {
        super(message);
        this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}
