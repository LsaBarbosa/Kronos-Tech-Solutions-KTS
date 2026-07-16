package com.kts.kronos.adapter.in.web.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class JsonAccessDeniedHandlerCoverageTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(objectMapper);

    // ── L33 FALSE + L38 FALSE: non-CSRF AccessDeniedException ─────────────────
    // Both instanceof CsrfException checks return FALSE → detail="Acesso negado." + code="ACCESS_DENIED"

    @Test
    void handle_withPlainAccessDeniedException_returnsAccessDeniedResponse() throws Exception {
        var request = new MockHttpServletRequest("GET", "/admin/resource");
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("user lacks role"));

        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, response.getStatus());
        assertEquals("ACCESS_DENIED", body.get("code").asText());
        assertEquals("Acesso negado.", body.get("message").asText());
        assertEquals("/admin/resource", body.get("path").asText());
    }
}
