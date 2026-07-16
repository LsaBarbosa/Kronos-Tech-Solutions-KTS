package com.kts.kronos.infrastructure;

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

import com.kts.kronos.application.exceptions.DigitalSignatureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DigitalSignatureServiceTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void registerBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("signData: deve gerar assinatura CMS com conteúdo encapsulado")
    void shouldSignDataWithGeneratedPkcs12Certificate() throws Exception {
        String password = "changeit";
        Path certificate = createPkcs12Certificate(password);
        DigitalSignatureService service = service(certificate, password);
        byte[] payload = "arquivo AEJ de teste".getBytes(StandardCharsets.UTF_8);

        byte[] signature = service.signData(payload);

        CMSSignedData signedData = new CMSSignedData(signature);
        assertThat((byte[]) signedData.getSignedContent().getContent()).isEqualTo(payload);
        assertThat(signedData.getSignerInfos().getSigners()).hasSize(1);
        assertThat(signedData.getCertificates().getMatches(null)).hasSize(1);
    }

    @Test
    @DisplayName("signData: deve lançar DigitalSignatureException quando certificado não existe")
    void shouldWrapCertificateLoadingFailure() {
        DigitalSignatureService service = service(tempDir.resolve("missing.p12"), "secret");

        assertThatThrownBy(() -> service.signData("conteudo".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("Certificado digital não localizado")
                // mensagem não pode vazar caminho do arquivo nem a senha
                .hasMessageNotContaining(tempDir.toString())
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("signData: payload nulo continua lançando DigitalSignatureException de configuração")
    void shouldWrapCertificateLoadingFailureWithNullPayload() {
        DigitalSignatureService service = service(tempDir.resolve("missing.p12"), "secret");

        assertThatThrownBy(() -> service.signData(null))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("Certificado digital não localizado")
                .hasMessageNotContaining(tempDir.toString())
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisplayName("signData: path em branco lança DigitalSignatureException de configuração")
    void shouldFailWhenCertificatePathIsBlank() {
        DigitalSignatureService service = new DigitalSignatureService();
        ReflectionTestUtils.setField(service, "certificatePath", "");
        ReflectionTestUtils.setField(service, "certificatePassword", "secret");

        assertThatThrownBy(() -> service.signData("data".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não configurado")
                .hasMessageNotContaining("secret");
    }

    private DigitalSignatureService service(Path certificatePath, String password) {
        DigitalSignatureService service = new DigitalSignatureService();
        ReflectionTestUtils.setField(service, "certificatePath", certificatePath.toString());
        ReflectionTestUtils.setField(service, "certificatePassword", password);
        return service;
    }

    private Path createPkcs12Certificate(String password) throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = selfSignedCertificate(keyPair);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password.toCharArray());
        keyStore.setKeyEntry(
                "kronos-test",
                keyPair.getPrivate(),
                password.toCharArray(),
                new Certificate[]{certificate}
        );

        Path file = tempDir.resolve("certificate.p12");
        try (var output = Files.newOutputStream(file)) {
            keyStore.store(output, password.toCharArray());
        }
        return file;
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    private X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        Instant now = Instant.parse("2026-04-20T00:00:00Z");
        X500Principal subject = new X500Principal("CN=Kronos Test Certificate");
        var builder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.ONE,
                Date.from(now.minusSeconds(60)),
                Date.from(now.plusSeconds(3600)),
                subject,
                keyPair.getPublic()
        );
        var signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));
    }

    @Test
    @DisplayName("signData: lanca excecao quando password e null")
    void shouldThrowWhenPasswordIsNull() {
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", "/nonexistent/cert.p12");
        ReflectionTestUtils.setField(svc, "certificatePassword", null);
        assertThatThrownBy(() -> svc.signData("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("Senha do certificado digital não configurada");
    }

    @Test
    @DisplayName("signPdf: lanca excecao quando password e null")
    void shouldThrowSignPdfWhenPasswordIsNull() {
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", "/nonexistent.p12");
        ReflectionTestUtils.setField(svc, "certificatePassword", null);
        assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("Senha do certificado digital não configurada");
    }

    @Test
    @DisplayName("signPdf: lanca excecao quando path e blank")
    void shouldThrowSignPdfWhenPathIsBlank() {
        DigitalSignatureService svc = new DigitalSignatureService();
        ReflectionTestUtils.setField(svc, "certificatePath", "");
        ReflectionTestUtils.setField(svc, "certificatePassword", "pass");
        assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não configurado");
    }

    @Test
    @DisplayName("signPdf: lanca excecao quando arquivo nao existe")
    void shouldThrowSignPdfWhenCertMissing() {
        DigitalSignatureService svc = service(tempDir.resolve("missingpdf.p12"), "pass");
        assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não localizado");
    }

    @Test
    @DisplayName("signPdf: lanca excecao quando arquivo sem permissao de leitura")
    void shouldThrowSignPdfWhenFileIsNotReadable() throws java.io.IOException {
        java.nio.file.Path certFile = tempDir.resolve("noperm2.p12");
        Files.createFile(certFile);
        certFile.toFile().setReadable(false);
        try {
            DigitalSignatureService svc = service(certFile, "pass");
            assertThatThrownBy(() -> svc.signPdf("pdf".getBytes(), "reason", "loc"))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("sem permissão");
        } finally {
            certFile.toFile().setReadable(true);
        }
    }

    @Test
    @DisplayName("signData: lanca excecao quando arquivo sem permissao de leitura")
    void shouldThrowSignDataWhenFileIsNotReadable() throws java.io.IOException {
        java.nio.file.Path certFile = tempDir.resolve("noperm1.p12");
        Files.createFile(certFile);
        certFile.toFile().setReadable(false);
        try {
            DigitalSignatureService svc = service(certFile, "pass");
            assertThatThrownBy(() -> svc.signData("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .isInstanceOf(DigitalSignatureException.class)
                    .hasMessageContaining("sem permissão");
        } finally {
            certFile.toFile().setReadable(true);
        }
    }

    @Test
    @DisplayName("signData: keystore vazio (sem aliases) lanca excecao")
    void shouldThrowWhenKeystoreHasNoAliases() throws Exception {
        String password = "empty";
        java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
        ks.load(null, password.toCharArray());
        java.nio.file.Path certFile = tempDir.resolve("empty.p12");
        try (var os = Files.newOutputStream(certFile)) {
            ks.store(os, password.toCharArray());
        }
        DigitalSignatureService svc = service(certFile, password);
        assertThatThrownBy(() -> svc.signData("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("sem alias válido");
    }

    @Test
    @DisplayName("signData: entrada sem chave privada (TrustedCertificateEntry) lanca excecao")
    void shouldThrowWhenPrivateKeyIsNull() throws Exception {
        String password = "certonly";
        KeyPair kp = generateKeyPair();
        java.security.cert.X509Certificate cert = selfSignedCertificate(kp);
        java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
        ks.load(null, password.toCharArray());
        ks.setCertificateEntry("trusted", cert);
        java.nio.file.Path certFile = tempDir.resolve("certonly.p12");
        try (var os = Files.newOutputStream(certFile)) {
            ks.store(os, password.toCharArray());
        }
        DigitalSignatureService svc = service(certFile, password);
        assertThatThrownBy(() -> svc.signData("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não contém chave privada");
    }

    @Test
    @DisplayName("signData: senha errada gera DigitalSignatureException")
    void shouldWrapWrongPasswordIoException() throws Exception {
        java.nio.file.Path cert = createPkcs12Certificate("correct");
        DigitalSignatureService svc = service(cert, "wrong-password-here");
        assertThatThrownBy(() -> svc.signData("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class);
    }

    @Test
    @DisplayName("signData: arquivo corrompido (nao PKCS12) gera DigitalSignatureException")
    void shouldWrapIoExceptionFromCorruptedFile() throws Exception {
        java.nio.file.Path corrupt = tempDir.resolve("corrupt.p12");
        Files.write(corrupt, "not-a-pkcs12-file-contents-xxxxxx".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        DigitalSignatureService svc = service(corrupt, "whatever");
        assertThatThrownBy(() -> svc.signData("data".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(DigitalSignatureException.class);
    }

    @Test
    @DisplayName("signPdf: keystore vazio (sem aliases) lanca excecao")
    void shouldThrowSignPdfWhenKeystoreHasNoAliases() throws Exception {
        String password = "empty2";
        java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
        ks.load(null, password.toCharArray());
        java.nio.file.Path certFile = tempDir.resolve("empty2.p12");
        try (var os = Files.newOutputStream(certFile)) {
            ks.store(os, password.toCharArray());
        }
        DigitalSignatureService svc = service(certFile, password);
        byte[] pdf = createMinimalPdf();
        assertThatThrownBy(() -> svc.signPdf(pdf, "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("sem alias");
    }

    @Test
    @DisplayName("signPdf: entrada sem chave privada lanca excecao")
    void shouldThrowSignPdfWhenPrivateKeyIsNull() throws Exception {
        String password = "certonly2";
        KeyPair kp = generateKeyPair();
        java.security.cert.X509Certificate cert = selfSignedCertificate(kp);
        java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
        ks.load(null, password.toCharArray());
        ks.setCertificateEntry("trusted2", cert);
        java.nio.file.Path certFile = tempDir.resolve("certonly2.p12");
        try (var os = Files.newOutputStream(certFile)) {
            ks.store(os, password.toCharArray());
        }
        DigitalSignatureService svc = service(certFile, password);
        byte[] pdf = createMinimalPdf();
        assertThatThrownBy(() -> svc.signPdf(pdf, "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class)
                .hasMessageContaining("não contém chave privada");
    }

    @Test
    @DisplayName("signPdf: senha errada gera DigitalSignatureException")
    void shouldWrapWrongPasswordInSignPdf() throws Exception {
        java.nio.file.Path cert = createPkcs12Certificate("correct2");
        DigitalSignatureService svc = service(cert, "wrongpassword");
        byte[] pdf = createMinimalPdf();
        assertThatThrownBy(() -> svc.signPdf(pdf, "reason", "loc"))
                .isInstanceOf(DigitalSignatureException.class);
    }

    @Test
    @DisplayName("signPdf: assina PDF valido com sucesso")
    void shouldSignPdfSuccessfully() throws Exception {
        String password = "signpdf";
        java.nio.file.Path cert = createPkcs12Certificate(password);
        DigitalSignatureService svc = service(cert, password);
        byte[] pdf = createMinimalPdf();

        byte[] signed = svc.signPdf(pdf, "Assinatura de teste", "Kronos HQ");

        assertThat(signed).isNotEmpty();
        assertThat(signed.length).isGreaterThan(pdf.length);
    }

    @Test
    @DisplayName("signPdf: reason e location null usam valores padrao")
    void shouldSignPdfWithNullReasonAndLocation() throws Exception {
        String password = "signpdf2";
        java.nio.file.Path cert = createPkcs12Certificate(password);
        DigitalSignatureService svc = service(cert, password);

        byte[] signed = svc.signPdf(createMinimalPdf(), null, null);
        assertThat(signed).isNotEmpty();
    }

    private byte[] createMinimalPdf() {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (com.itextpdf.kernel.pdf.PdfDocument pdf =
                     new com.itextpdf.kernel.pdf.PdfDocument(
                             new com.itextpdf.kernel.pdf.PdfWriter(baos))) {
            pdf.addNewPage();
        }
        return baos.toByteArray();
    }
}
