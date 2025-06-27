package com.kts.kronos.domain.exceptions;

public class TooManyRequestsException extends RuntimeException{
    public TooManyRequestsException() {
        super("Muitas requisições. Tente novamente mais tarde.");
    }
}
