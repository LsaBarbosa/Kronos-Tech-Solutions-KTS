package com.kts.kronos.application.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForbiddenExceptionTest {

    @Test
    void shouldUseDefaultMessageWhenMessageIsBlank() {
        // message.isBlank() = TRUE → default message
        assertEquals("Sem permissão para utilizar esse recurso", new ForbiddenException(" ").getMessage());
    }

    @Test
    void shouldUseDefaultMessageWhenMessageIsNull() {
        // message == null = TRUE → default message (covers null branch of ||)
        assertEquals("Sem permissão para utilizar esse recurso", new ForbiddenException(null).getMessage());
    }
}
