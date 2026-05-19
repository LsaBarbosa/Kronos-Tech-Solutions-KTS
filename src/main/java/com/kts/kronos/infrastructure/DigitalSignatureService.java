package com.kts.kronos.infrastructure;

import com.kts.kronos.observability.application.KronosTracing;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.GeneralSecurityException;
@Slf4j
@Service
public class DigitalSignatureService {

    private final KronosTracing kronosTracing;

    @Value("${kronos.security.certificate.path}")
    private String certificatePath;

    @Value("${kronos.security.certificate.password}")
    private String certificatePassword;

    static {
        // Registra o provider de segurança da Bouncy Castle
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public DigitalSignatureService(KronosTracing kronosTracing) {
        this.kronosTracing = kronosTracing;
    }

    public DigitalSignatureService() {
        this(new KronosTracing());
    }

    /**
     * Gera uma assinatura digital PKCS#7 (CMS) padrão ICP-Brasil.
     * @param dataToSign Dados originais (ex: conteúdo do arquivo AEJ)
     * @return Bytes da assinatura (para salvar como .p7s)
     */
    public byte[] signData(byte[] dataToSign) {
        try {
            byte[] signedBytes = kronosTracing.observe("kronos.legal.digital_signature", () -> {
                try {
                    var keyStore = KeyStore.getInstance("PKCS12");
                    try (InputStream is = new FileInputStream(certificatePath)) {
                        keyStore.load(is, certificatePassword.toCharArray());
                    }

                    var alias = keyStore.aliases().nextElement();
                    var privateKey = (PrivateKey) keyStore.getKey(alias, certificatePassword.toCharArray());
                    var certificate = (X509Certificate) keyStore.getCertificate(alias);

                    List<Certificate> certList = new ArrayList<>();
                    certList.add(certificate);
                    Store<?> certs = new JcaCertStore(certList);

                    ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                            .setProvider("BC")
                            .build(privateKey);

                    var generator = new CMSSignedDataGenerator();
                    generator.addSignerInfoGenerator(
                            new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                                    .build(sha256Signer, certificate));

                    generator.addCertificates(certs);
                    var msg = new CMSProcessableByteArray(dataToSign);
                    var signedData = generator.generate(msg, true);
                    return signedData.getEncoded();
                } catch (IOException | GeneralSecurityException | CMSException | OperatorCreationException ex) {
                    throw new RuntimeException(ex);
                }
            });

            log.info("event=legal_digital_signature result=success");
            return signedBytes;
        } catch (RuntimeException e) {
            log.error("event=legal_digital_signature result=failure reason=digital_signature exception_type={}",
                    e.getClass().getSimpleName());
            throw new RuntimeException("Falha ao assinar documento digitalmente", e);
        }
    }
}
