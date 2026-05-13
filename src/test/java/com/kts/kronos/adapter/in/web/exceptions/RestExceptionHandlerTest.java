package com.kts.kronos.adapter.in.web.exceptions;

import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void shouldHandleDisabledExceptionAsForbidden() {
        var request = request("/auth/login");
        var response = handler.handleDisabledException(new DisabledException("usuario inativo"), request);
        var body = (ProblemDetail) response.getBody();

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", body.getTitle());
        assertEquals("usuario inativo", body.getDetail());
        assertEquals("USER_DISABLED", body.getCode());
        assertEquals("/auth/login", body.getPath());
    }

    @Test
    void shouldHandleTooManyRequests() {
        var request = request("/auth/login");
        var response = handler.handleTooManyRequestsException(new TooManyRequestsException("tente depois"), request);
        var body = (ProblemDetail) response.getBody();

        assertEquals(429, response.getStatusCode().value());
        assertEquals("Too Many Requests", body.getTitle());
        assertEquals("tente depois", body.getDetail());
        assertEquals("RATE_LIMIT_EXCEEDED", body.getCode());
    }

    @Test
    void shouldReturnGenericMessageForUnexpectedException() {
        var response = handler.handleUnexpectedException(new RuntimeException("senha=secret"), request("/internal"));
        var body = (ProblemDetail) response.getBody();

        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", body.getCode());
        assertEquals("Erro inesperado", body.getMessage());
        assertNull(body.getValidationErrors());
    }

    @Test
    void shouldMapInvalidJsonToBadRequestWithoutTechnicalDetail() {
        var response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("Unexpected character at position 1"),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request("/auth/login")
        );
        var body = (ProblemDetail) response.getBody();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("INVALID_REQUEST_BODY", body.getCode());
        assertEquals("JSON inválido ou malformado.", body.getMessage());
    }

    @Test
    void shouldMapInvalidPathParameterToBadRequest() {
        var exception = new MethodArgumentTypeMismatchException(
                "abc",
                UUID.class,
                "id",
                null,
                new IllegalArgumentException("invalid uuid")
        );

        var response = handler.handleMethodArgumentTypeMismatch(exception, request("/users/abc"));
        var body = (ProblemDetail) response.getBody();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("INVALID_PARAMETER", body.getCode());
        assertEquals("Parâmetro inválido.", body.getMessage());
    }

    @Test
    void shouldMapAccessDeniedAndTermsToStandardForbiddenPayloads() {
        var denied = handler.handleAccessDenied(new AccessDeniedException("raw details"), request("/companies"));
        var deniedBody = (ProblemDetail) denied.getBody();

        assertEquals(403, denied.getStatusCode().value());
        assertEquals("ACCESS_DENIED", deniedBody.getCode());
        assertEquals("Acesso negado.", deniedBody.getMessage());

        var terms = handler.handleTermsNotAccepted(
                new TermsNotAcceptedException("Aceite os termos para continuar.", "https://termo.kronossolutions.tech/"),
                request("/documents")
        );
        var termsBody = (ProblemDetail) terms.getBody();

        assertEquals(403, terms.getStatusCode().value());
        assertEquals("TERMS_NOT_ACCEPTED", termsBody.getCode());
        assertEquals("https://termo.kronossolutions.tech/", termsBody.getRedirectUrl());
    }

    private ServletWebRequest request(String path) {
        return new ServletWebRequest(new MockHttpServletRequest("POST", path));
    }
}
