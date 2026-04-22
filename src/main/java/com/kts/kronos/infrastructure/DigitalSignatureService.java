package com.kts.kronos.infrastructure;

import com.kts.kronos.config.CertificateCryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DigitalSignatureService {

    private final CertificateCryptoProperties certificateProperties;

    public DigitalSignatureService(CertificateCryptoProperties certificateProperties) {
        this.certificateProperties = certificateProperties;
    }

    static {
        // Registra o provider de segurança da Bouncy Castle
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * Gera uma assinatura digital PKCS#7 (CMS) padrão ICP-Brasil.
     * @param dataToSign Dados originais (ex: conteúdo do arquivo AEJ)
     * @return Bytes da assinatura (para salvar como .p7s)
     */
    public byte[] signData(byte[] dataToSign) {
        try {
            log.info(
                    "Iniciando processo de assinatura digital. payloadSize={}",
                    dataToSign != null ? dataToSign.length : 0
            );
            // 1. Carregar KeyStore (Certificado .pfx)
            var keyStore = KeyStore.getInstance("PKCS12");
            try (var is = Files.newInputStream(certificateProperties.pathAsPath())) {
                keyStore.load(is, certificateProperties.passwordAsChars());
            }

            // 2. Obter Alias (Nome interno do certificado)
            var alias = keyStore.aliases().nextElement();
            var privateKey = (PrivateKey) keyStore.getKey(alias, certificateProperties.passwordAsChars());
            var certificate = (X509Certificate) keyStore.getCertificate(alias);

            // 3. Criar Cadeia de Certificação
            List<Certificate> certList = new ArrayList<>();
            certList.add(certificate);
            var certs = new JcaCertStore(certList);

            // 4. Configurar Assinador (SHA256 com RSA)
            var sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(privateKey);

            var generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                            .build(sha256Signer, certificate));

            generator.addCertificates(certs);

            // 5. Assinar o Conteúdo
            var msg = new CMSProcessableByteArray(dataToSign);
            
            // true = Encapsulated (O arquivo .p7s contém o original + assinatura)
            // false = Detached (O arquivo .p7s contém só a assinatura, precisa do .txt junto)
            // Para AEJ, geralmente usamos Detached (false) ou conforme especificação do layout.
            // Vamos usar TRUE (Attached) para garantir que o arquivo seja autocontido se baixado.
            var signedData = generator.generate(msg, true);

            return signedData.getEncoded();

        } catch (IOException | GeneralSecurityException | CMSException | OperatorCreationException e) {
            log.error(
                    "Erro crítico na assinatura digital. payloadSize={}",
                    dataToSign != null ? dataToSign.length : 0,
                    e
            );
            throw new RuntimeException("Falha ao assinar documento digitalmente", e);
        }
    }
}
