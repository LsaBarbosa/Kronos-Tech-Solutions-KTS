package com.kts.kronos.adapter.in.web.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatedAuthenticationEntryPointTest {

    private final DelegatedAuthenticationEntryPoint entryPoint = new DelegatedAuthenticationEntryPoint();

    @Test
    void shouldReturnGenericUnauthorizedPayloadWithoutLeakingExceptionMessage() throws Exception {
        var request = new MockHttpServletRequest("GET", "/private");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Usuário inativo."));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("Autenticação inválida ou ausente."));
        assertFalse(response.getContentAsString().contains("Usuário inativo"));
    }
}
