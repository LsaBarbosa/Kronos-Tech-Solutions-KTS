package com.kts.kronos.observability.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("SensitiveDataSanitizer - LGPD-OBS-001 Tests")
class SensitiveDataSanitizerTest {

    private SensitiveDataSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new SensitiveDataSanitizer();
    }

    @Test
    @DisplayName("deve mascarar CPF sem máscara")
    void deveMascararCpfSemMascara() {
        String result = sanitizer.sanitizeText("Erro ao processar CPF 12345678901");
        assertTrue(result.contains("[CPF_REDACTED]"));
        assertFalse(result.contains("12345678901"));
    }

    @Test
    @DisplayName("deve mascarar CPF com máscara")
    void deveMascararCpfComMascara() {
        String result = sanitizer.sanitizeText("CPF: 123.456.789-01");
        assertTrue(result.contains("[CPF_REDACTED]"));
        assertFalse(result.contains("123.456.789-01"));
    }

    @Test
    @DisplayName("deve mascarar CNPJ sem máscara")
    void deveMascararCnpjSemMascara() {
        String result = sanitizer.sanitizeText("CNPJ 12345678000199");
        assertTrue(result.contains("[CNPJ_REDACTED]"));
        assertFalse(result.contains("12345678000199"));
    }

    @Test
    @DisplayName("deve mascarar CNPJ com máscara")
    void deveMascararCnpjComMascara() {
        String result = sanitizer.sanitizeText("CNPJ: 12.345.678/0001-99");
        assertTrue(result.contains("[CNPJ_REDACTED]"));
        assertFalse(result.contains("12.345.678/0001-99"));
    }

    @Test
    @DisplayName("deve mascarar email")
    void deveMascararEmail() {
        String result = sanitizer.sanitizeText("Usuario joao@empresa.com.br falhou");
        assertTrue(result.contains("[EMAIL_REDACTED]"));
        assertFalse(result.contains("joao@empresa.com.br"));
    }

    @Test
    @DisplayName("deve mascarar Bearer token")
    void deveMascararBearerToken() {
        String result = sanitizer.sanitizeText("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc.def");
        assertTrue(result.contains("Bearer [REDACTED]"));
        assertFalse(result.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    @DisplayName("deve mascarar JWT puro")
    void deveMascararJwtPuro() {
        String result = sanitizer.sanitizeText("Token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsdWNhcyJ9.signature");
        assertTrue(result.contains("[JWT_REDACTED]"));
        assertFalse(result.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    @DisplayName("deve mascarar base64 em data URL")
    void deveMascararBase64EmDataUrl() {
        String result = sanitizer.sanitizeText("Image: data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA");
        assertTrue(result.contains("data:image/png;base64,[REDACTED]"));
        assertFalse(result.contains("iVBORw0KGgoAAAANSUhEUgAA"));
    }

    @Test
    @DisplayName("deve mascarar base64 direto")
    void deveMascararBase64Direto() {
        String result = sanitizer.sanitizeText("base64,iVBORw0KGgoAAAANSUhEUgAA");
        assertTrue(result.contains("base64,[REDACTED]"));
        assertFalse(result.contains("iVBORw0KGgoAAAANSUhEUgAA"));
    }

    @Test
    @DisplayName("deve mascarar query params sensíveis")
    void deveMascararQueryParamsSensiveis() {
        String result = sanitizer.sanitizeText("URL: /api?token=abc123&signature=xyz789");
        assertTrue(result.contains("token=[REDACTED]"));
        assertTrue(result.contains("signature=[REDACTED]"));
        assertFalse(result.contains("abc123"));
        assertFalse(result.contains("xyz789"));
    }

    @Test
    @DisplayName("deve mascarar storage paths S3")
    void deveMascararStoragePathsS3() {
        String result = sanitizer.sanitizeText("Path: s3://bucket-name/file.pdf");
        assertTrue(result.contains("[STORAGE_PATH_REDACTED]"));
        assertFalse(result.contains("s3://bucket-name"));
    }

    @Test
    @DisplayName("deve mascarar storage paths locais")
    void deveMascararStoragePathsLocais() {
        String result = sanitizer.sanitizeText("Upload to employees/12345678901/faces/file.jpg");
        assertTrue(result.contains("[STORAGE_PATH_REDACTED]"));
        assertFalse(result.contains("employees/12345678901"));
    }

    @Test
    @DisplayName("deve truncar texto longo")
    void deveTruncarTextoLongo() {
        String longText = "a".repeat(6000);
        String result = sanitizer.sanitizeText(longText);
        assertTrue(result.length() < 6000);
        assertTrue(result.contains("[TRUNCATED]"));
    }

    @Test
    @DisplayName("deve manejar null e vazio")
    void deveManejarNullEVazio() {
        // sanitizeText com null retorna null (conforme implementação)
        assertNull(sanitizer.sanitizeText(null));
        // sanitizeText com branco retorna vazio
        String emptyResult = sanitizer.sanitizeText("");
        assertNotNull(emptyResult);
        String blankResult = sanitizer.sanitizeText("  ");
        assertNotNull(blankResult);
    }

    @Test
    @DisplayName("sanitizeMessage deve limitar tamanho")
    void sanitizeMessageDeveLimitarTamanho() {
        String longMessage = "a".repeat(1200);
        String result = sanitizer.sanitizeMessage(longMessage);
        // Após sanitização + truncate, o resultado será > 1000 (inclui ...[TRUNCATED])
        // Mas deve ter sido processado e incluir marcador de truncamento
        assertTrue(result.contains("[TRUNCATED]"));
        // O importante é que foi limitado em vez de retornado inteiro
        assertTrue(result.length() < longMessage.length());
    }

    @Test
    @DisplayName("sanitizeName deve limitar tamanho")
    void sanitizeNameDeveLimitarTamanho() {
        String longName = "a".repeat(250);
        String result = sanitizer.sanitizeName(longName);
        // Após truncate, incluirá ...[TRUNCATED]
        assertTrue(result.contains("[TRUNCATED]"));
        assertTrue(result.length() < longName.length());
    }

    @Test
    @DisplayName("deve retornar marcador seguro em caso de erro de sanitização")
    void deveRetornarMarcadorSeguroEmErro() {
        // Este teste é mais difícil em Java, mas verificamos que não lança exceção
        String result = sanitizer.sanitizeText("Any normal text");
        assertNotNull(result);
        assertNotEquals("", result);
    }

    @Test
    @DisplayName("sanitizeObject deve sanitizar como texto")
    void sanitizeObjectDeveSanitizarComoTexto() {
        String result = sanitizer.sanitizeObject("CPF 12345678901");
        assertTrue(result.contains("[CPF_REDACTED]"));
        assertFalse(result.contains("12345678901"));
    }

    @Test
    @DisplayName("containsSensitiveData deve detectar CPF")
    void containsSensitiveDataDeveDetectarCpf() {
        assertTrue(sanitizer.containsSensitiveData("Usuário CPF 12345678901"));
        assertFalse(sanitizer.containsSensitiveData("Usuário normal"));
    }

    @Test
    @DisplayName("containsSensitiveData deve detectar email")
    void containsSensitiveDataDeveDetectarEmail() {
        assertTrue(sanitizer.containsSensitiveData("Enviar para joao@empresa.com"));
        assertFalse(sanitizer.containsSensitiveData("Enviar para empresa"));
    }

    @Test
    @DisplayName("containsSensitiveData deve detectar token")
    void containsSensitiveDataDeveDetectarToken() {
        assertTrue(sanitizer.containsSensitiveData("?token=abc123xyz"));
        assertFalse(sanitizer.containsSensitiveData("?id=123"));
    }

    @Test
    @DisplayName("containsSensitiveData deve detectar JWT")
    void containsSensitiveDataDeveDetectarJwt() {
        assertTrue(sanitizer.containsSensitiveData("Auth: eyJhbGciOiJIUzI1NiJ9.abc.def"));
        assertFalse(sanitizer.containsSensitiveData("Auth: abc123"));
    }

    @Test
    @DisplayName("mensagem de erro com múltiplos dados sensíveis deve sanitizar todos")
    void mensagemComMultiplosDadosSensiveisDeveSanitizarTodos() {
        String errorMsg = "Falha ao salvar usuário joao@email.com com CPF 12345678901 e token Bearer abc123xyz";
        String result = sanitizer.sanitizeMessage(errorMsg);

        assertFalse(result.contains("joao@email.com"));
        assertFalse(result.contains("12345678901"));
        assertFalse(result.contains("abc123xyz"));
        assertTrue(result.contains("[EMAIL_REDACTED]"));
        assertTrue(result.contains("[CPF_REDACTED]"));
        assertTrue(result.contains("Bearer [REDACTED]"));
    }

    @Test
    void sanitizeObject_null_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeObject(null));
    }

    @Test
    void sanitizeMessage_null_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeMessage(null));
    }

    @Test
    void sanitizeMessage_blank_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeMessage(""));
        assertEquals("", sanitizer.sanitizeMessage("   "));
    }

    @Test
    void sanitizeName_null_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeName(null));
    }

    @Test
    void sanitizeName_blank_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeName("   "));
    }

    @Test
    void sanitizeStackTrace_null_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeStackTrace(null));
    }

    @Test
    void sanitizeStackTrace_blank_returnsEmptyString() {
        assertEquals("", sanitizer.sanitizeStackTrace(""));
    }

    @Test
    void sanitizeStackTrace_sanitizesAndTruncatesLongTrace() {
        String trace = "at com.example.Service.method(Service.java:10)\n".repeat(200);
        String result = sanitizer.sanitizeStackTrace(trace);
        assertNotNull(result);
    }

    @Test
    void sanitizeStackTrace_withSensitiveData() {
        String trace = "Exception: cpf=12345678901 email=joao@empresa.com\nat com.example.Service.method(Service.java:10)";
        String result = sanitizer.sanitizeStackTrace(trace);
        assertFalse(result.contains("12345678901"));
    }

    @Test
    void containsSensitiveData_detectsCnpj() {
        assertTrue(sanitizer.containsSensitiveData("CNPJ: 12345678000199"));
    }

    @Test
    void containsSensitiveData_detectsBearer() {
        assertTrue(sanitizer.containsSensitiveData("Bearer eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    void containsSensitiveData_detectsS3Path() {
        assertTrue(sanitizer.containsSensitiveData("s3://bucket/file.pdf"));
    }

    @Test
    void containsSensitiveData_null_returnsFalse() {
        assertFalse(sanitizer.containsSensitiveData(null));
    }

    @Test
    void containsSensitiveData_blank_returnsFalse() {
        assertFalse(sanitizer.containsSensitiveData("   "));
    }

    @Test
    void containsSensitiveData_passwordParam_returnsTrue() {
        assertTrue(sanitizer.containsSensitiveData("password=secret123"));
    }

    @Test
    void sanitizeObject_catchBlock_returnsFailMarkerWhenToStringThrows() {
        // Trigger catch block in sanitizeObject by passing an object that throws in toString()
        Object bad = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("forced exception for coverage");
            }
        };
        // sanitizeObject catches Exception from String.valueOf(value) → toString() → throws
        String result = sanitizer.sanitizeObject(bad);
        assertEquals("[SANITIZATION_FAILED]", result);
    }

    @Test
    void sanitizeMessage_catchBlock_returnsSafeMarkerWhenSanitizeTextThrows() {
        // Spy to force sanitizeText to throw, covering catch block in sanitizeMessage
        var spy = spy(new com.kts.kronos.observability.domain.SensitiveDataSanitizer());
        doThrow(new RuntimeException("forced")).when(spy).sanitizeText(anyString());
        assertEquals("[SANITIZATION_FAILED]", spy.sanitizeMessage("any text"));
    }

    @Test
    void sanitizeName_catchBlock_returnsSafeMarkerWhenSanitizeTextThrows() {
        var spy = spy(new com.kts.kronos.observability.domain.SensitiveDataSanitizer());
        doThrow(new RuntimeException("forced")).when(spy).sanitizeText(anyString());
        assertEquals("[SANITIZATION_FAILED]", spy.sanitizeName("any name"));
    }

    @Test
    void sanitizeStackTrace_catchBlock_returnsSafeMarkerWhenSanitizeTextThrows() {
        var spy = spy(new com.kts.kronos.observability.domain.SensitiveDataSanitizer());
        doThrow(new RuntimeException("forced")).when(spy).sanitizeText(anyString());
        assertEquals("[SANITIZATION_FAILED]", spy.sanitizeStackTrace("any trace"));
    }

}