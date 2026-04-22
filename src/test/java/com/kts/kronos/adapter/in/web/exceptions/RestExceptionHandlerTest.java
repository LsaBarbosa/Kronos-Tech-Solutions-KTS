package com.kts.kronos.adapter.in.web.exceptions;

import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();
    private final ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void shouldHandleDisabledExceptionAsForbidden() {
        var response = handler.handleDisabledException(new DisabledException("usuario inativo"), request);
        var body = (ProblemDetail) response.getBody();

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", body.getTitle());
        assertEquals("usuario inativo", body.getDetail());
    }

    @Test
    void shouldHandleTooManyRequests() {
        var response = handler.handleTooManyRequestsException(new TooManyRequestsException("tente depois"), request);
        var body = (ProblemDetail) response.getBody();

        assertEquals(429, response.getStatusCode().value());
        assertEquals("Too Many Requests", body.getTitle());
        assertEquals("tente depois", body.getDetail());
    }
}
