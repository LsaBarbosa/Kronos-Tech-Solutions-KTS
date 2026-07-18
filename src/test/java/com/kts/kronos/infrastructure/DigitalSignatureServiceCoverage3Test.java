package com.kts.kronos.infrastructure;

import com.kts.kronos.application.exceptions.DigitalSignatureException;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DigitalSignatureServiceCoverage3Test {

    @TempDir
    Path tempDir;

    // ── signData L100: keystore has no aliases ────────────────────────────────
    @Test
    void signData_emptyAliases_throwsDseNoAlias() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.emptyEnumeration());

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signData("data".getBytes()))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("sem alias");
        }
    }

    // ── signData L125-126: UnrecoverableKeyException from getKey ─────────────
    @Test
    void signData_unrecoverableKey_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert2.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
        doThrow(new UnrecoverableKeyException("bad password"))
                .when(mockKs).getKey(eq("alias1"), any(char[].class));

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signData("data".getBytes()))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("chave privada");
        }
    }

    // ── signData L134-135: OperatorCreationException caught as GeneralSecurityException|CMSException|OperatorCreationException
    @Test
    void signData_operatorCreationException_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert3.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        PrivateKey mockPK = mock(PrivateKey.class);
        X509Certificate mockCert = mock(X509Certificate.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
        when(mockKs.getKey(eq("alias1"), any(char[].class))).thenReturn(mockPK);
        when(mockKs.getCertificate("alias1")).thenReturn(mockCert);

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS);
             MockedConstruction<JcaCertStore> ignoredCertStore = mockConstruction(JcaCertStore.class);
             MockedConstruction<JcaContentSignerBuilder> mockBuilder = mockConstruction(
                     JcaContentSignerBuilder.class, (m, ctx) -> {
                         when(m.setProvider(anyString())).thenReturn(m);
                         when(m.build(any(PrivateKey.class)))
                                 .thenThrow(new org.bouncycastle.operator.OperatorCreationException("test"));
                     })) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signData("data".getBytes()))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("criptográfica");
        }
    }

    // ── signPdf L187: keystore has no aliases ────────────────────────────────
    @Test
    void signPdf_emptyAliases_throwsDseNoAlias() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert4.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.emptyEnumeration());

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("sem alias");
        }
    }

    // ── signPdf L204-205: UnrecoverableKeyException ───────────────────────────
    @Test
    void signPdf_unrecoverableKey_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert5.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
        doThrow(new UnrecoverableKeyException("bad password"))
                .when(mockKs).getKey(eq("alias1"), any(char[].class));

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("chave privada");
        }
    }

    // ── signPdf L211-212: GeneralSecurityException from signDetached ─────────
    @Test
    void signPdf_generalSecurityException_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert6.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        PrivateKey mockPK = mock(PrivateKey.class);
        X509Certificate mockCert = mock(X509Certificate.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
        when(mockKs.getKey(eq("alias1"), any(char[].class))).thenReturn(mockPK);
        when(mockKs.getCertificateChain("alias1")).thenReturn(new Certificate[]{mockCert});
        when(mockPK.getAlgorithm()).thenReturn("RSA");

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS);
             MockedConstruction<com.itextpdf.kernel.pdf.PdfReader> mockReader = mockConstruction(
                     com.itextpdf.kernel.pdf.PdfReader.class);
             MockedConstruction<com.itextpdf.signatures.PdfSigner> mockSigner = mockConstruction(
                     com.itextpdf.signatures.PdfSigner.class, (m, ctx) -> {
                         com.itextpdf.signatures.PdfSignatureAppearance appearance =
                                 mock(com.itextpdf.signatures.PdfSignatureAppearance.class);
                         when(appearance.setReason(anyString())).thenReturn(appearance);
                         when(appearance.setLocation(anyString())).thenReturn(appearance);
                         when(m.getSignatureAppearance()).thenReturn(appearance);
                         doNothing().when(m).setFieldName(anyString());
                         doThrow(new GeneralSecurityException("crypto failure"))
                                 .when(m).signDetached(any(), any(), any(), any(), any(), any(), anyInt(), any());
                     })) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("criptográfica");
        }
    }

    // ── signData L99-100: getCertificate returns null → throw DSE(X.509) ────────
    @Test
    void signData_nullCertificate_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert7.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        PrivateKey mockPK = mock(PrivateKey.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
        when(mockKs.getKey(eq("alias1"), any(char[].class))).thenReturn(mockPK);
        when(mockKs.getCertificate("alias1")).thenReturn(null);

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signData("data".getBytes()))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("X.509");
        }
    }

    // ── signData L130: IOException with "password" in message ───────────────────
    @Test
    void signData_ioExceptionWithPassword_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert8.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        doThrow(new IOException("bad password provided"))
                .when(mockKs).load(any(InputStream.class), any(char[].class));

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signData("data".getBytes()))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("Senha");
        }
    }

    // ── signPdf L186-187: getCertificateChain returns null → throw DSE ──────────
    @Test
    void signPdf_nullChain_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert9.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        PrivateKey mockPK = mock(PrivateKey.class);
        doNothing().when(mockKs).load(any(InputStream.class), any(char[].class));
        when(mockKs.aliases()).thenReturn(Collections.enumeration(List.of("alias1")));
        when(mockKs.getKey(eq("alias1"), any(char[].class))).thenReturn(mockPK);
        when(mockKs.getCertificateChain("alias1")).thenReturn(null);

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("cadeia");
        }
    }

    // ── signPdf L207: IOException with "password" in message ────────────────────
    @Test
    void signPdf_ioExceptionWithPassword_throwsDse() throws Exception {
        Path certFile = Files.createFile(tempDir.resolve("cert10.p12"));
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", certFile.toString());
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");

        KeyStore mockKs = mock(KeyStore.class);
        doThrow(new IOException("wrong password"))
                .when(mockKs).load(any(InputStream.class), any(char[].class));

        try (MockedStatic<KeyStore> mockKsStatic = mockStatic(KeyStore.class, Answers.CALLS_REAL_METHODS)) {
            mockKsStatic.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(mockKs);
            assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("Senha");
        }
    }
}