package com.kts.kronos.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Supplemental coverage for SensitiveDataMasker:
 * - Constructor instantiation (L9)
 * - maskCpf: non-null but length < 11 (L76 length<11 TRUE)
 * - maskEmail: non-null but no "@" (L86 contains=false TRUE)
 * - maskPhone: non-null but length < 10 (L96 length<10 TRUE)
 * - maskJwt: non-null but length < 20 (L106 length<20 TRUE)
 * - containsSensitiveData(null) → false (L116/L117)
 * - containsSensitiveData with JWT pattern → JWT TRUE branch (L122)
 * - containsSensitiveData with BASE64 face → BASE64 TRUE branch (L124)
 * - containsSensitiveData with storage path → STORAGE TRUE branch (L127)
 */
class SensitiveDataMaskerCoverageTest {

    @Test
    void constructor_instantiation_doesNotThrow() {
        // Covers the implicit default constructor (L9 class declaration)
        assertDoesNotThrow(() -> new SensitiveDataMasker());
    }

    @Test
    void maskCpf_shortNonNullValue_returnsInvalidCpf() {
        // cpf != null but length=3 < 11 → length<11 TRUE branch
        assertEquals("[INVALID_CPF]", SensitiveDataMasker.maskCpf("123"));
    }

    @Test
    void maskEmail_noAtSign_returnsInvalidEmail() {
        // email != null but no "@" → contains("@") FALSE branch
        assertEquals("[INVALID_EMAIL]", SensitiveDataMasker.maskEmail("noemail"));
    }

    @Test
    void maskPhone_shortNonNullValue_returnsInvalidPhone() {
        // phone != null but length=3 < 10 → length<10 TRUE branch
        assertEquals("[INVALID_PHONE]", SensitiveDataMasker.maskPhone("123"));
    }

    @Test
    void maskJwt_shortNonNullValue_returnsInvalidJwt() {
        // jwt != null but length=5 < 20 → length<20 TRUE branch
        assertEquals("[INVALID_JWT]", SensitiveDataMasker.maskJwt("short"));
    }

    @Test
    void containsSensitiveData_null_returnsFalse() {
        // message == null → TRUE branch → return false (L116/L117)
        assertFalse(SensitiveDataMasker.containsSensitiveData(null));
    }

    @Test
    void containsSensitiveData_withJwtToken_returnsTrue() {
        // JWT_PATTERN TRUE branch (L122): CPF=F, PIS=F, JWT=T
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.signaturePart";
        assertTrue(SensitiveDataMasker.containsSensitiveData(jwt));
    }

    @Test
    void containsSensitiveData_withBase64FaceImage_returnsTrue() {
        // BASE64_FACE_PATTERN TRUE branch (L124): no CPF/PIS/JWT/PASSWORD, but faceImageBase64
        String msg = "faceImageBase64=" + "A".repeat(110);
        assertTrue(SensitiveDataMasker.containsSensitiveData(msg));
    }

    @Test
    void containsSensitiveData_withStoragePath_returnsTrue() {
        // STORAGE_PATH_PATTERN TRUE branch (L127): no CPF/PIS/JWT/PASSWORD/BASE64/RESET/API
        String msg = "file uploaded to s3://my-bucket/uploads/document.pdf";
        assertTrue(SensitiveDataMasker.containsSensitiveData(msg));
    }
    // ── null inputs: cover the short-circuit TRUE branch of (a == null || ...) ──

    @Test
    void maskCpf_null_returnsInvalidCpf() {
        assertEquals("[INVALID_CPF]", SensitiveDataMasker.maskCpf(null));
    }

    @Test
    void maskEmail_null_returnsInvalidEmail() {
        assertEquals("[INVALID_EMAIL]", SensitiveDataMasker.maskEmail(null));
    }

    @Test
    void maskPhone_null_returnsInvalidPhone() {
        assertEquals("[INVALID_PHONE]", SensitiveDataMasker.maskPhone(null));
    }

    @Test
    void maskJwt_null_returnsInvalidJwt() {
        assertEquals("[INVALID_JWT]", SensitiveDataMasker.maskJwt(null));
    }
}
