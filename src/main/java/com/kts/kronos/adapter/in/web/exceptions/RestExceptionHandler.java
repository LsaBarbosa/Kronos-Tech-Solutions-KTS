package com.kts.kronos.adapter.in.web.exceptions;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        logSanitized(HttpStatus.FORBIDDEN, ex, request);
        return buildResponseEntity(
                ex,
                HttpStatus.FORBIDDEN,
                request,
                null,
                AuthenticationProblemResponses.AUTHORIZATION_DETAIL
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleDisabledException(DisabledException ex, WebRequest request) {
        return handleAuthenticationException(ex, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return handleAuthenticationException(ex, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logSanitized(HttpStatus.UNAUTHORIZED, ex, request);
        return buildResponseEntity(
                ex,
                HttpStatus.UNAUTHORIZED,
                request,
                null,
                AuthenticationProblemResponses.AUTHENTICATION_DETAIL
        );
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Object> handleTooManyRequestsException(TooManyRequestsException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.TOO_MANY_REQUESTS, request, null);
    }

    private ResponseEntity<Object> buildResponseEntity(Exception ex, HttpStatus status, WebRequest request, List<ProblemDetail.Error> errors) {
        return buildResponseEntity(ex, status, request, errors, ex.getMessage());
    }

    private ResponseEntity<Object> buildResponseEntity(
            Exception ex,
            HttpStatus status,
            WebRequest request,
            List<ProblemDetail.Error> errors,
            String detail
    ) {
        ProblemDetail problemDetail = ProblemDetail.builder()
                .status(status.value())
                .title(status.getReasonPhrase())
                .detail(detail)
                .errors(errors)
                .build();

        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), status, request);
    }

    private void logSanitized(HttpStatus status, Exception ex, WebRequest request) {
        log.warn("Resposta {} sanitizada. request={}, exceptionType={}, message={}",
                status.value(),
                request.getDescription(false),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }
}
