package com.kts.kronos.infrastructure;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DigitalSignatureServiceTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @TempDir
    Path tempDir;

    @Test
    void signDataReturnsSignedContentWhenCertificateIsValid() throws Exception {
        String password = "changeit";
        Path certificateFile = createPkcs12File(password, true);
        DigitalSignatureService service = buildService(certificateFile, password);

        byte[] signed = service.signData("conteudo-a-assinar".getBytes());

        assertNotNull(signed);
        assertTrue(signed.length > 0);
    }

    @Test
    void signDataThrowsRuntimeExceptionWhenCertificatePathIsInvalid() {
        DigitalSignatureService service = buildService(tempDir.resolve("missing.p12"), "changeit");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.signData("abc".getBytes()));

        assertTrue(exception.getMessage().startsWith("Falha ao assinar documento digitalmente:"));
    }

    @Test
    void signDataThrowsRuntimeExceptionWhenCertificatePasswordIsInvalid() throws Exception {
        Path certificateFile = createPkcs12File("correct-password", true);
        DigitalSignatureService service = buildService(certificateFile, "wrong-password");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.signData("abc".getBytes()));

        assertTrue(exception.getMessage().startsWith("Falha ao assinar documento digitalmente:"));
    }

    @Test
    void signDataThrowsRuntimeExceptionWhenNoAliasExistsInKeyStore() throws Exception {
        String password = "changeit";
        Path emptyCertificateFile = createPkcs12File(password, false);
        DigitalSignatureService service = buildService(emptyCertificateFile, password);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.signData("abc".getBytes()));

        assertTrue(exception.getMessage().startsWith("Falha ao assinar documento digitalmente:"));
    }

    @Test
    void signDataThrowsRuntimeExceptionWhenCertificatePasswordIsNull() throws Exception {
        Path certificateFile = createPkcs12File("changeit", true);
        DigitalSignatureService service = new DigitalSignatureService();
        ReflectionTestUtils.setField(service, "certificatePath", certificateFile.toString());
        ReflectionTestUtils.setField(service, "certificatePassword", null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.signData("abc".getBytes()));

        assertTrue(exception.getMessage().startsWith("Falha ao assinar documento digitalmente:"));
    }

    private DigitalSignatureService buildService(Path certificatePath, String password) {
        DigitalSignatureService service = new DigitalSignatureService();
        ReflectionTestUtils.setField(service, "certificatePath", certificatePath.toString());
        ReflectionTestUtils.setField(service, "certificatePassword", password);
        return service;
    }

    private Path createPkcs12File(String password, boolean withEntry) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        if (withEntry) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X509Certificate certificate = createSelfSignedCertificate(keyPair);
            PrivateKey privateKey = keyPair.getPrivate();
            keyStore.setKeyEntry("alias-cert", privateKey, password.toCharArray(), new java.security.cert.Certificate[]{certificate});
        }

        Path file = Files.createTempFile(tempDir, "certificate", ".p12");
        try (var outputStream = Files.newOutputStream(file)) {
            keyStore.store(outputStream, password.toCharArray());
        }
        return file;
    }

    private X509Certificate createSelfSignedCertificate(KeyPair keyPair) throws Exception {
        Instant now = Instant.now();
        Date notBefore = Date.from(now.minus(1, ChronoUnit.DAYS));
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

        X500Name issuer = new X500Name("CN=Kronos Test");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                issuer,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }
}
