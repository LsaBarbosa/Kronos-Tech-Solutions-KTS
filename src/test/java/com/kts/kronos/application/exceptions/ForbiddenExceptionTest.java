package com.kts.kronos.application.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForbiddenExceptionTest {

    @Test
    void shouldUseDefaultMessageWhenMessageIsBlank() {
        assertEquals("Sem permissão para utilizar esse recurso", new ForbiddenException(" ").getMessage());
    }
}
