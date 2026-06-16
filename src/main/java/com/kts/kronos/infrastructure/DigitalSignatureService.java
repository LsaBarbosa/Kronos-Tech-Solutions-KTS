package com.kts.kronos.infrastructure;

import com.kts.kronos.application.exceptions.DigitalSignatureException;
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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
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
     * @throws DigitalSignatureException quando o certificado/keystore está
     *         indisponível ou a operação criptográfica falha. Mensagens não
     *         expõem path nem senha; o motivo técnico fica apenas no log.
     */
    public byte[] signData(byte[] dataToSign) {
        validateCertificateConfig();
        try {
            byte[] signedBytes = kronosTracing.observe("kronos.legal.digital_signature", () -> {
                try {
                    var keyStore = KeyStore.getInstance("PKCS12");
                    try (InputStream is = new FileInputStream(certificatePath)) {
                        keyStore.load(is, certificatePassword.toCharArray());
                    }

                    if (!keyStore.aliases().hasMoreElements()) {
                        throw new DigitalSignatureException("Certificado digital sem alias válido.");
                    }

                    var alias = keyStore.aliases().nextElement();
                    var privateKey = (PrivateKey) keyStore.getKey(alias, certificatePassword.toCharArray());
                    if (privateKey == null) {
                        throw new DigitalSignatureException("Certificado digital não contém chave privada utilizável.");
                    }
                    var certificate = (X509Certificate) keyStore.getCertificate(alias);
                    if (certificate == null) {
                        throw new DigitalSignatureException("Certificado digital não contém certificado X.509 utilizável.");
                    }

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
                } catch (FileNotFoundException ex) {
                    throw new DigitalSignatureException(
                            "Certificado digital não localizado no caminho configurado.",
                            ex);
                } catch (UnrecoverableKeyException ex) {
                    throw new DigitalSignatureException(
                            "Não foi possível recuperar a chave privada do certificado digital.",
                            ex);
                } catch (IOException ex) {
                    String reason = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("password")
                            ? "Senha do certificado digital inválida."
                            : "Falha ao ler o certificado digital.";
                    throw new DigitalSignatureException(reason, ex);
                } catch (GeneralSecurityException | CMSException | OperatorCreationException ex) {
                    throw new DigitalSignatureException(
                            "Falha criptográfica na assinatura do documento.",
                            ex);
                }
            });

            log.info("event=legal_digital_signature result=success");
            return signedBytes;
        } catch (DigitalSignatureException ex) {
            log.error("event=legal_digital_signature result=failure reason=digital_signature exception_type={} cause={}",
                    ex.getClass().getSimpleName(),
                    ex.getCause() == null ? "none" : ex.getCause().getClass().getSimpleName());
            throw ex;
        } catch (RuntimeException ex) {
            log.error("event=legal_digital_signature result=failure reason=unexpected exception_type={}",
                    ex.getClass().getSimpleName());
            throw new DigitalSignatureException("Falha ao assinar documento digitalmente.", ex);
        }
    }

    private void validateCertificateConfig() {
        if (certificatePath == null || certificatePath.isBlank()) {
            throw new DigitalSignatureException("Certificado digital não configurado neste ambiente.");
        }
        if (certificatePassword == null) {
            throw new DigitalSignatureException("Senha do certificado digital não configurada neste ambiente.");
        }
        if (!Files.exists(Path.of(certificatePath))) {
            throw new DigitalSignatureException("Certificado digital não localizado no caminho configurado.");
        }
        if (!Files.isReadable(Path.of(certificatePath))) {
            throw new DigitalSignatureException("Certificado digital sem permissão de leitura para o processo do servidor.");
        }
    }
}
