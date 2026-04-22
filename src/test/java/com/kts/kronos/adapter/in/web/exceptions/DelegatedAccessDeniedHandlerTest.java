package com.kts.kronos.adapter.in.web.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatedAccessDeniedHandlerTest {

    private final DelegatedAccessDeniedHandler handler = new DelegatedAccessDeniedHandler();

    @Test
    void shouldReturnGenericForbiddenPayloadWithoutLeakingExceptionMessage() throws Exception {
        var request = new MockHttpServletRequest("GET", "/admin");
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Role não autorizada para esta operação."));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("Acesso negado."));
        assertFalse(response.getContentAsString().contains("Role não autorizada"));
    }
}
