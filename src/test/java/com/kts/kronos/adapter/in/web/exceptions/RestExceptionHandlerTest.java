package com.kts.kronos.adapter.in.web.exceptions;

import com.kts.kronos.application.exceptions.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.mock.web.MockHttpServletRequest;

class RestExceptionHandlerTest {

    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void shouldHandleMappedExceptions() {
        var handler = new RestExceptionHandler();

        assertThat(handler.handleBadRequestException(new BadRequestException("erro"), request).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleResourceNotFoundException(new ResourceNotFoundException("nf"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleForbiddenException(new ForbiddenException("forbidden"), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleTooManyRequestsException(new TooManyRequestsException("too many"), request).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldHandleAuthenticationExceptions() {
        var handler = new RestExceptionHandler();

        var disabled = handler.handleDisabledException(new DisabledException("x"), request);
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var badCredentials = handler.handleBadCredentialsException(new BadCredentialsException("raw"), request);
        assertThat(badCredentials.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(((ProblemDetail) badCredentials.getBody()).getDetail()).isEqualTo("Usuário ou senha inválidos");
    }

    @Test
    void shouldReturnGenericMessagesForServiceAndInternalErrors() {
        var handler = new RestExceptionHandler();

        var serviceUnavailable = handler.handleServiceUnavailableException(new ServiceUnavailableException("down"), request);
        assertThat(serviceUnavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(((ProblemDetail) serviceUnavailable.getBody()).getDetail())
                .isEqualTo("Serviço temporariamente indisponível. Tente novamente.");

        var internal = handler.handleInternalServerException(new InternalServerException("boom"), request);
        assertThat(internal.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(((ProblemDetail) internal.getBody()).getDetail())
                .isEqualTo("Erro interno inesperado. Tente novamente mais tarde.");
    }

    @Test
    void shouldHandleValidationErrors() throws Exception {
        var target = new Object();
        var binding = new BeanPropertyBindingResult(target, "target");
        binding.addError(new FieldError("target", "name", "obrigatório"));
        var method = SamplePayload.class.getDeclaredMethod("setName", String.class);
        var parameter = new org.springframework.core.MethodParameter(method, 0);
        var ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<Object> response = new TestableHandler()
                .callHandleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var body = (ProblemDetail) response.getBody();
        assertThat(body.getErrors()).hasSize(1);
        assertThat(body.getErrors().get(0).getName()).isEqualTo("name");
        assertThat(body.getErrors().get(0).getUserMessage()).isEqualTo("obrigatório");
    }

    private static class SamplePayload {
        public void setName(String name) {}
    }

    private static class TestableHandler extends RestExceptionHandler {
        ResponseEntity<Object> callHandleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
            return super.handleMethodArgumentNotValid(ex, headers, status, request);
        }
    }
}
