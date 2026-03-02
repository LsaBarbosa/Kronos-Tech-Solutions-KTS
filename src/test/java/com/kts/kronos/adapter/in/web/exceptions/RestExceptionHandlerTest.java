package com.kts.kronos.adapter.in.web.exceptions;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.InternalServerException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.ServiceUnavailableException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestExceptionHandlerTest {

    private RestExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
        request = new ServletWebRequest(new MockHttpServletRequest());
    }

    @Test
    void shouldHandleBadRequestException() {
        var response = handler.handleBadRequestException(new BadRequestException("invalid"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("invalid", body.getDetail());
    }

    @Test
    void shouldHandleResourceNotFoundException() {
        var response = handler.handleResourceNotFoundException(new ResourceNotFoundException("missing"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("missing", body.getDetail());
    }

    @Test
    void shouldHandleForbiddenException() {
        var response = handler.handleForbiddenException(new ForbiddenException("ignored"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertNotNull(body.getDetail());
    }

    @Test
    void shouldHandleDisabledException() {
        var response = handler.handleDisabledException(new DisabledException("disabled"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("disabled", body.getDetail());
    }

    @Test
    void shouldHandleBadCredentialsExceptionWithCustomMessage() {
        var response = handler.handleBadCredentialsException(new BadCredentialsException("real message"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("Usuário ou senha inválidos", body.getDetail());
    }

    @Test
    void shouldHandleTooManyRequestsException() {
        var response = handler.handleTooManyRequestsException(new TooManyRequestsException("slow down"), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("slow down", body.getDetail());
    }

    @Test
    void shouldHandleServiceUnavailableExceptionWithGenericMessage() {
        var response = handler.handleServiceUnavailableException(new ServiceUnavailableException("sensitive"), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("Serviço temporariamente indisponível. Tente novamente.", body.getDetail());
    }

    @Test
    void shouldHandleInternalServerExceptionWithGenericMessage() {
        var response = handler.handleInternalServerException(new InternalServerException("sensitive"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertEquals("Erro interno inesperado. Tente novamente mais tarde.", body.getDetail());
    }

    @Test
    void shouldHandleMethodArgumentNotValidExceptionAndExposeFieldErrors() throws Exception {
        var target = new DummyRequest();
        var bindingResult = new BeanPropertyBindingResult(target, "dummyRequest");
        bindingResult.addError(new FieldError("dummyRequest", "email", "inválido"));

        Method method = DummyController.class.getDeclaredMethod("submit", DummyRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var body = assertInstanceOf(ProblemDetail.class, response.getBody());
        assertTrue(body.getDetail().contains("Validation failed for argument"));
        assertNotNull(body.getErrors());
        assertEquals(1, body.getErrors().size());
        assertEquals("email", body.getErrors().getFirst().getName());
        assertEquals("inválido", body.getErrors().getFirst().getUserMessage());
    }

    static class DummyController {
        public void submit(DummyRequest request) {
        }
    }

    static class DummyRequest {
        private String email;
    }
}
