package com.kts.kronos.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.ClientIpResolution;
import com.kts.kronos.domain.model.enuns.AuditAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @InjectMocks
    private AuditService auditService;

    @Mock
    private AuditLogProvider auditLogProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void register_shouldSanitizeCpfInDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String detailsWithCpf = "Usuario acessou com CPF 12345678901 para operacao";

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            detailsWithCpf
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertNotNull(capturedLog.details());
        assertFalse(capturedLog.details().contains("12345678901"), "CPF bruto não deve estar no log");
        assertTrue(capturedLog.details().contains("123.***.901"), "CPF deve estar mascarado");
    }

    @Test
    void register_shouldSanitizeEmailInDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String detailsWithEmail = "Email do usuario: usuario@dominio.com foi atualizado";

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            detailsWithEmail
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains("usuario@dominio.com"), "Email bruto não deve estar no log");
        assertTrue(capturedLog.details().contains("u***@dominio.com"), "Email deve estar mascarado");
    }

    @Test
    void register_shouldSanitizeJwtTokenInDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        String detailsWithToken = "Token de autenticacao: " + jwtToken + " foi utilizado";

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            detailsWithToken
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains("dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"), "JWT bruto não deve estar no log");
        assertTrue(capturedLog.details().contains("eyJhbGc***"), "JWT deve estar mascarado");
    }

    @Test
    void register_shouldSanitizeBase64LongInDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        // Base64 longo (300+ caracteres sem "==" no meio para triggerar o regex)
        String base64Long = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABuwAAAbsBOuzj4gAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAGKSURBVCiPY/hPAAz/UVKCQ0MDw3+4/P9/kv8M/5G4T/AfhQcMRGlmYGD4j8QdJSAlwMDw/z+ChMQDAz1QmYEBpPj/fwb2f/8Z/jMwMPxnYGBgYPjPwMDAwPDfgYHhPwMDAwPDfwYGhv8M/xn+M/xn+P+fgUHi////2f8z/Gf4z/Cf4T/Df4b/DP8Z/jOwsPxnYGD5z8DB8p+BheU/AwtLCRYW1pIsLKwsJVhYWFpIMLCysrCUYGFhKcHAwsJSgoGFhQQDCwtLCQYWFhYSDCwsJRhYWFhKMLCwsJBgYGEpwVL03/CfgYUVpCjhDwMDAwsraxkW1pKsrKUkGFhZSbCUYGFhZSlRgsXCypKFhYWEBAsLCwkWFhYSLCwsLCQkGFhYSEiwsLCQYGFhKcHCwlKChYWFBAsLCwkWFhYSLKwsxVlYWIqzsJBgYWEpwcLCwkKChYVHSBATJyLBwlJagpkZpJiFhRQzC0sJZpAiZhYSDCwsJBhYWEqwsLCQYGBhIcHCwkKCgYWFRAmWEiwsJBhYWEqwsLCQYGFhKcHCwlKChYWlBAsDCwkGFhYSLCwsJFhYWEiwsLCUYGFhYSnBwsJSgoWFpQQLCwsJFhYWEiwsLCUkWFhYSrCwVACQQQELw/HvjgAAAABJRU5ErkJggg==";
        String detailsWithBase64 = "Imagem enviada: " + base64Long;

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            detailsWithBase64
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains(base64Long), "Base64 longo bruto não deve estar no log");
        assertTrue(capturedLog.details().contains("[BASE64_REDACTED]"), "Base64 deve estar redacted");
    }

    @Test
    void register_shouldSanitizeS3PathInDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String detailsWithS3Path = "Documento armazenado em s3://bucket-name/path/to/document.pdf para operacao";

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            detailsWithS3Path
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains("s3://bucket-name/path/to/document.pdf"), "Caminho S3 bruto não deve estar no log");
        assertTrue(capturedLog.details().contains("[MASKED_PATH]"), "Caminho S3 deve estar mascarado");
    }

    @Test
    void register_shouldHandleNullDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            null
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertNull(capturedLog.details());
    }

    @Test
    void register_shouldHandleBlankDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            "   "
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertEquals("   ", capturedLog.details());
    }

    @Test
    void register_shouldSanitizeMultipleSensitiveDataTypesInSingleDetail() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String complexDetails = "Usuario CPF 98765432100, email joao@empresa.com, token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ e arquivo s3://docs/confidential.pdf foram acessados";

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            complexDetails
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        String sanitized = capturedLog.details();

        // Validar que CPF foi mascarado
        assertFalse(sanitized.contains("98765432100"));

        // Validar que email foi mascarado
        assertFalse(sanitized.contains("joao@empresa.com"));

        // Validar que token foi mascarado
        assertFalse(sanitized.contains("TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"));

        // Validar que S3 path foi mascarado
        assertFalse(sanitized.contains("s3://docs/confidential.pdf"));
    }

    @Test
    void registerLgpd_shouldSanitizeDetails() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String detailsWithCpf = "Solicitacao LGPD para CPF 12345678901";

        auditService.registerLgpd(
            AuditAction.LGPD_DATA_EXPORTED,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "HIGH",
            detailsWithCpf,
            "192.168.1.1",
            "Mozilla/5.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains("12345678901"));
        assertTrue(capturedLog.details().contains("123.***.901"));
    }

    @Test
    void registerSecurity_shouldSanitizeDetails() {
        UUID employeeId = UUID.randomUUID();
        String detailsWithEmail = "Falha de autenticacao para usuario@empresa.com";

        auditService.registerSecurity(
            AuditAction.AUTH_LOGIN_FAILURE,
            employeeId,
            null,
            "HIGH",
            "USER",
            employeeId.toString(),
            detailsWithEmail,
            "192.168.1.1",
            "Mozilla/5.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains("usuario@empresa.com"));
        assertTrue(capturedLog.details().contains("u***@empresa.com"));
    }

    @Test
    void register_shouldPreserveNonSensitiveData() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String detailsWithoutSensitiveData = "Usuario realizou login com sucesso, operacao concluida em 2 segundos";

        auditService.register(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            companyId,
            "USER",
            employeeId.toString(),
            "MEDIUM",
            "192.168.1.1",
            "Mozilla/5.0",
            detailsWithoutSensitiveData
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertEquals(detailsWithoutSensitiveData, capturedLog.details());
    }

    @Test
    void registerLgpd_withClientIpResolution_shouldIncludeIpMetadata() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String details = "Solicitacao LGPD para exportacao de dados";
        ClientIpResolution ipResolution = ClientIpResolution.of(
            "203.0.113.10",
            ClientIpResolution.IpSource.X_FORWARDED_FOR,
            true
        );

        auditService.registerLgpd(
            AuditAction.LGPD_DATA_EXPORTED,
            employeeId,
            null,
            companyId,
            "LGPD_REQUEST",
            "request-123",
            "HIGH",
            details,
            ipResolution,
            "Mozilla/5.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertEquals("203.0.113.10", capturedLog.ipAddress());
        assertTrue(capturedLog.details().contains("ipSource"), "Details deve conter ipSource");
        assertTrue(capturedLog.details().contains("ipTrusted"), "Details deve conter ipTrusted");
    }

    @Test
    void registerSecurity_withClientIpResolution_shouldIncludeIpMetadata() {
        UUID employeeId = UUID.randomUUID();
        String details = "Falha de autenticacao suspeita";
        ClientIpResolution ipResolution = ClientIpResolution.of(
            "198.51.100.7",
            ClientIpResolution.IpSource.REMOTE_ADDR,
            false
        );

        auditService.registerSecurity(
            AuditAction.AUTH_LOGIN_FAILURE,
            employeeId,
            null,
            "HIGH",
            "USER",
            employeeId.toString(),
            details,
            ipResolution,
            "Mozilla/5.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertEquals("198.51.100.7", capturedLog.ipAddress());
        assertTrue(capturedLog.details().contains("ipSource"), "Details deve conter ipSource");
        assertTrue(capturedLog.details().contains("ipTrusted"), "Details deve conter ipTrusted");
    }

    @Test
    void registerLgpd_withClientIpResolution_shouldSanitizeDetailsWithIpMetadata() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String detailsWithCpf = "Exportacao LGPD para CPF 12345678901";
        ClientIpResolution ipResolution = ClientIpResolution.of(
            "127.0.0.1",
            ClientIpResolution.IpSource.X_FORWARDED_FOR,
            true
        );

        auditService.registerLgpd(
            AuditAction.LGPD_DATA_EXPORTED,
            employeeId,
            null,
            companyId,
            "LGPD_REQUEST",
            "request-456",
            "HIGH",
            detailsWithCpf,
            ipResolution,
            "Mozilla/5.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());

        AuditLog capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertFalse(capturedLog.details().contains("12345678901"), "CPF bruto nao deve estar no log");
        assertTrue(capturedLog.details().contains("123.***.901"), "CPF deve estar mascarado");
        assertTrue(capturedLog.details().contains("ipSource"), "Details deve conter ipSource");
    }
    @Test
    void registerSecurity_withNullDetailsAndClientIpResolution_usesEmptyDetailsMap() throws Exception {
        UUID employeeId = UUID.randomUUID();
        ClientIpResolution ipResolution = ClientIpResolution.of(
            "10.0.0.1",
            ClientIpResolution.IpSource.REMOTE_ADDR,
            true
        );
        doReturn("{\"ipSource\":\"REMOTE_ADDR\",\"ipTrusted\":true}").when(objectMapper).writeValueAsString(any());

        auditService.registerSecurity(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            "HIGH",
            "USER",
            employeeId.toString(),
            null,
            ipResolution,
            "Mozilla/5.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void registerSecurity_withBlankDetailsAndClientIpResolution_usesEmptyDetailsMap() throws Exception {
        UUID employeeId = UUID.randomUUID();
        ClientIpResolution ipResolution = ClientIpResolution.of(
            "10.0.0.2",
            ClientIpResolution.IpSource.X_FORWARDED_FOR,
            false
        );
        doReturn("{\"ipSource\":\"X_FORWARDED_FOR\",\"ipTrusted\":false}").when(objectMapper).writeValueAsString(any());

        auditService.registerSecurity(
            AuditAction.AUTH_LOGIN_SUCCESS,
            employeeId,
            null,
            "MEDIUM",
            "USER",
            employeeId.toString(),
            "   ",
            ipResolution,
            "Agent/1.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void registerSecurity_withNullDetailsAndWriteValueThrows_usesFallback() throws Exception {
        UUID employeeId = UUID.randomUUID();
        ClientIpResolution ipResolution = ClientIpResolution.of(
            "10.0.0.3",
            ClientIpResolution.IpSource.REMOTE_ADDR,
            false
        );
        doThrow(new RuntimeException("forced write failure")).when(objectMapper).writeValueAsString(any());

        auditService.registerSecurity(
            AuditAction.AUTH_LOGIN_FAILURE,
            employeeId,
            null,
            "HIGH",
            "USER",
            employeeId.toString(),
            null,
            ipResolution,
            "Agent/1.0"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogProvider).registerLog(captor.capture());
        assertNotNull(captor.getValue());
    }

}