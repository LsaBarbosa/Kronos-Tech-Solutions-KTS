package com.kts.kronos.domain.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("Usuário não encontrado: " + username);
    }
}
