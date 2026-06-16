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
}
