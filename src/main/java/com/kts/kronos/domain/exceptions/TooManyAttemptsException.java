package com.kts.kronos.domain.exceptions;

public class TooManyAttemptsException extends RuntimeException{
    public TooManyAttemptsException() {
        super("Muitas tentativas de login sem sucesso.");
    }
}
