package com.kts.kronos.adapter.in.web.exceptions;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.CodedForbiddenException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.DigitalSignatureException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.infrastructure.security.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @Value("${spring.profiles.active:development}")
    private String activeProfile;

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(ConflictException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String maskedMessage = SensitiveDataMasker.maskSensitiveData(ex.getMessage());
        log.warn("event=http_error result=failure reason=data_integrity_conflict path={} detail={}", path(request), maskedMessage);
        return buildResponseEntity(
                ex,
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_CONFLICT",
                "Registro duplicado ou conflito de integridade.",
                request,
                null,
                null
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<ProblemDetail.Error> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ProblemDetail.Error.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .name(fieldError.getField())
                        .userMessage(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dados inválidos.", request, errors, null);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String maskedDetail = SensitiveDataMasker.maskSensitiveData(ex.getMostSpecificCause().getMessage());
        log.warn(
            "event=http_error result=failure reason=invalid_request_body path={} detail={}",
            path(request),
            maskedDetail
        );
        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "JSON inválido ou malformado.", request, null, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parâmetro inválido.", request, null, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        String code = ex instanceof CodedForbiddenException coded ? coded.getCode() : "FORBIDDEN";
        return buildResponseEntity(ex, HttpStatus.FORBIDDEN, code, ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(TermsNotAcceptedException.class)
    public ResponseEntity<Object> handleTermsNotAccepted(TermsNotAcceptedException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.FORBIDDEN, "TERMS_NOT_ACCEPTED", ex.getMessage(), request, null, ex.getRedirectUrl());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso negado.", request, null, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleDisabledException(DisabledException ex, WebRequest request) {
        // Generic message prevents user enumeration: "account disabled" would reveal the username exists
        return buildResponseEntity(ex, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Usuário ou senha inválidos", request, null, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Usuário ou senha inválidos", request, null, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Autenticação requerida ou inválida.", request, null, null);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Object> handleTooManyRequestsException(TooManyRequestsException ex, WebRequest request) {
        return buildResponseEntity(ex, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(DigitalSignatureException.class)
    public ResponseEntity<Object> handleDigitalSignatureException(DigitalSignatureException ex, WebRequest request) {
        // Log já feito no DigitalSignatureService/AejService. Aqui apenas registramos o path
        // afetado para correlação operacional, sem expor causa/path/senha ao usuário.
        log.error("event=http_error result=failure reason=digital_signature_unavailable path={} exception_type={}",
                path(request),
                ex.getClass().getSimpleName());
        // 503 sinaliza problema infraestrutural transitório (certificado/keystore) — não é
        // sessão expirada e não deve disparar redirecionamento para login no front-end.
        return buildResponseEntity(
                ex,
                HttpStatus.SERVICE_UNAVAILABLE,
                "DIGITAL_SIGNATURE_UNAVAILABLE",
                "Serviço de assinatura digital indisponível no momento. Tente novamente em alguns minutos ou contate o administrador.",
                request,
                null,
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        String maskedMessage = SensitiveDataMasker.maskSensitiveData(ex.getMessage());
        log.error("event=http_error result=failure reason=unexpected path={} exception_type={} message={}",
                path(request),
                ex.getClass().getSimpleName(),
                maskedMessage);
        return buildResponseEntity(ex, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", INTERNAL_SERVER_ERROR, request, null, null);
    }

    private ResponseEntity<Object> buildResponseEntity(
            Exception ex,
            HttpStatus status,
            String code,
            String message,
            WebRequest request,
            List<ProblemDetail.Error> errors,
            String redirectUrl
    ) {
        String maskedMessage = SensitiveDataMasker.maskSensitiveData(message);
        boolean isProd = "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);

        ProblemDetail problemDetail = ProblemDetail.builder()
                .code(code)
                .message(maskedMessage)
                .status(status.value())
                .path(path(request))
                .validationErrors(errors)
                .redirectUrl(redirectUrl)
                .title(status.getReasonPhrase())
                .detail(isProd ? maskedMessage : maskDetailIfSensitive(maskedMessage))
                .errors(errors)
                .build();

        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), status, request);
    }

    private String maskDetailIfSensitive(String detail) {
        if (SensitiveDataMasker.containsSensitiveData(detail)) {
            return SensitiveDataMasker.maskSensitiveData(detail);
        }
        return detail;
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest servletRequest = servletWebRequest.getRequest();
            return servletRequest.getRequestURI();
        }
        String description = request == null ? null : request.getDescription(false);
        if (description != null && description.startsWith("uri=")) {
            return description.substring("uri=".length());
        }
        return "unknown";
    }
}
