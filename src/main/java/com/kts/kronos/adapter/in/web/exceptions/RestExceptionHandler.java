package com.kts.kronos.adapter.in.web.exceptions;
import com.kts.kronos.application.exceptions.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "Erro interno inesperado. Tente novamente mais tarde.";
    private static final String GENERIC_SERVICE_UNAVAILABLE_MESSAGE = "Serviço temporariamente indisponível. Tente novamente.";

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST, request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.NOT_FOUND, request, null);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ProblemDetail.Error> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ProblemDetail.Error.builder()
                        .name(fieldError.getField())
                        .userMessage(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST, request, errors);
    }

    private ResponseEntity<Object> buildResponseEntity(Exception ex, HttpStatus status, WebRequest request, List<ProblemDetail.Error> errors) {
        ProblemDetail problemDetail = ProblemDetail.builder()
                .status(status.value())
                .title(status.getReasonPhrase())
                .detail(ex.getMessage())
                .errors(errors)
                .build();

        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.FORBIDDEN, request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleDisabledException(DisabledException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.FORBIDDEN, request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        var customException = new BadCredentialsException("Usuário ou senha inválidos");
        return buildResponseEntity(customException, HttpStatus.UNAUTHORIZED, request, null);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Object> handleServiceUnavailableException(ServiceUnavailableException ex, WebRequest request) {
        return buildResponseEntity(new RuntimeException(GENERIC_SERVICE_UNAVAILABLE_MESSAGE), HttpStatus.SERVICE_UNAVAILABLE, request, null);    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<Object> handleInternalServerException(InternalServerException ex, WebRequest request) {
        return buildResponseEntity(new RuntimeException(GENERIC_INTERNAL_ERROR_MESSAGE), HttpStatus.INTERNAL_SERVER_ERROR, request, null);    }
}