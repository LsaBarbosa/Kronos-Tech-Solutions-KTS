package com.kts.kronos.domain.exceptions;

public class UserDisabledException extends RuntimeException {
    public UserDisabledException() {
        super("Usuário desabilitado.");
    }
}
