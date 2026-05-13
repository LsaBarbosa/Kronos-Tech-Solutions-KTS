package com.kts.kronos.adapter.in.web.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(objectMapper);

    @Test
    void shouldReturnStandardProblemDetailForCsrfFailures() throws Exception {
        var request = new MockHttpServletRequest("POST", "/companies");
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new CsrfException("raw csrf failure"));

        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("CSRF_TOKEN_INVALID", body.get("code").asText());
        assertEquals("Token CSRF ausente ou inválido.", body.get("message").asText());
        assertEquals("/companies", body.get("path").asText());
    }
}
