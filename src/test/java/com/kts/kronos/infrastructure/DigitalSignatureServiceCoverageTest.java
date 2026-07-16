package com.kts.kronos.infrastructure;

import com.kts.kronos.application.exceptions.DigitalSignatureException;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalSignatureServiceCoverageTest {

    @TempDir
    Path tempDir;

    // ── validateCertificateConfig — certificatePath nulo lança DSE ────────────

    @Test
    @DisplayName("validateCertificateConfig: certificatePath=null → lança DSE 'não configurado'")
    void validateCertificateConfig_nullCertificatePath_throwsDSE() {
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", null);
        ReflectionTestUtils.setField(svc, "certificatePassword", "secret");

        assertThatThrownBy(() -> svc.signData("data".getBytes()))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não configurado");
    }

    // ── signData lambda — FileNotFoundException catch ─────────────────────────

    @Test
    @DisplayName("signData: certificatePath é diretório → FileNotFoundException na lambda → DSE 'não localizado'")
    void signData_certIsDirectory_firesFileNotFoundCatch() throws Exception {
        Path certDir = Files.createTempDirectory(tempDir, "certdir");

        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certDir.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "secret");

        assertThatThrownBy(() -> svc.signData("data".getBytes()))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não localizado");
    }

    // ── signData outer RuntimeException catch ─────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("signData: kronosTracing.observe() lança NullPointerException → outer catch embrulha em DSE")
    void signData_outerRuntimeExceptionCatch_mockedTracing() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("dummy.p12"));

        KronosTracing mockTracing = mock(KronosTracing.class);
        when(mockTracing.observe(anyString(), any(Supplier.class)))
                .thenThrow(new NullPointerException("tracing fault"));

        DigitalSignatureService svc = new DigitalSignatureService(mockTracing);
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "secret");

        assertThatThrownBy(() -> svc.signData("data".getBytes()))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("Falha ao assinar documento digitalmente.")
                .hasCauseInstanceOf(NullPointerException.class);
    }

    // ── signPdf lambda — FileNotFoundException catch ──────────────────────────

    @Test
    @DisplayName("signPdf: certificatePath é diretório → FileNotFoundException na lambda → DSE 'não localizado'")
    void signPdf_certIsDirectory_firesFileNotFoundCatch() throws Exception {
        Path certDir = Files.createTempDirectory(tempDir, "certdir2");

        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certDir.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "secret");

        assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não localizado");
    }

    // ── signPdf outer RuntimeException catch ──────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("signPdf: kronosTracing.observe() lança NullPointerException → outer catch embrulha em DSE")
    void signPdf_outerRuntimeExceptionCatch_mockedTracing() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("dummy2.p12"));

        KronosTracing mockTracing = mock(KronosTracing.class);
        when(mockTracing.observe(anyString(), any(Supplier.class)))
                .thenThrow(new NullPointerException("tracing fault pdf"));

        DigitalSignatureService svc = new DigitalSignatureService(mockTracing);
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "secret");

        assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("Falha ao assinar PDF digitalmente.")
                .hasCauseInstanceOf(NullPointerException.class);
    }
}
