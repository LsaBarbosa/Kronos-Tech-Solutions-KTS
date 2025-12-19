package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ReceiptPdfService {

    // Substitua pelo número real de registro do software no INPI quando houver
    private static final String INPI_REGISTRATION_NUMBER = "999999999";
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Gera o Comprovante de Registro de Ponto do Trabalhador (Portaria 671).
     * * @param company Dados da empresa
     * @param employee Dados do funcionário
     * @param recordDate Data/Hora da marcação
     * @param nsr Número Sequencial de Registro
     * @return byte[] contendo o arquivo PDF gerado
     */
    public byte[] generateReceipt(Company company, Employee employee, LocalDateTime recordDate, Long nsr) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 1. Cabeçalho Obrigatório (Art. 79, I)
            addTitle(document, "Comprovante de Registro de Ponto do Trabalhador");

            // 2. Identificação do Empregador (Art. 79, II)
            addSection(document, "EMPREGADOR");
            addInfo(document, "Nome: ", company.name());
            addInfo(document, "CNPJ: ", company.cnpj());

            // 3. Identificação do Trabalhador (Art. 79, III)
            addSection(document, "TRABALHADOR");
            addInfo(document, "Nome: ", employee.fullName());
            addInfo(document, "CPF: ", employee.cpf());
            // PIS é opcional no comprovante novo, mas útil se tiver
            // addInfo(document, "PIS: ", employee.pis());

            // 4. Dados da Marcação (Art. 79, IV)
            addSection(document, "MARCAÇÃO DE PONTO");
            addInfo(document, "Data e Hora: ", recordDate.format(DATETIME_FMT));
            addInfo(document, "NSR (Número Sequencial): ", String.valueOf(nsr));

            // 5. Identificação do REP-P (Art. 79, V)
            addInfo(document, "Registro no INPI: ", INPI_REGISTRATION_NUMBER);
            addInfo(document, "Tipo de Sistema: ", "REP-P (Registrador Eletrônico de Ponto via Programa)");

            // 6. Código Hash (SHA-256) (Art. 79, VI)
            // O hash garante que esses dados não foram alterados.
            String rawData = String.format("%s%s%s%s%s",
                    nsr,
                    company.cnpj(),
                    employee.cpf(),
                    recordDate.format(DATETIME_FMT),
                    INPI_REGISTRATION_NUMBER);

            String hash = calculateSha256(rawData);

            addSection(document, "SEGURANÇA");
            addInfo(document, "Código Hash (SHA-256): ", hash);

            // 7. Assinatura Eletrônica (Placeholder) (Art. 79, VII / Art. 80)
            // Aqui entraria a representação visual da assinatura digital ICP-Brasil
            addParagraph(document, "\n-------------------------------------------------------------");
            addParagraph(document, "Documento assinado eletronicamente conforme Art. 87 da Portaria 671.");
            addParagraph(document, "Assinatura Digital do Fabricante/Desenvolvedor: [ASSINATURA_DIGITAL_AQUI]");

            document.close();

            // TODO: Integrar com componente de assinatura digital (PKCS#7 / CMS) se possuir certificado A1.

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar comprovante de ponto PDF", e);
            throw new RuntimeException("Erro na geração do comprovante de ponto", e);
        }
    }

    private void addTitle(Document doc, String text) {
        Paragraph p = new Paragraph(text)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(14);
        doc.add(p);
    }

    private void addSection(Document doc, String text) {
        Paragraph p = new Paragraph("\n" + text)
                .setBold()
                .setFontSize(12)
                .setUnderline();
        doc.add(p);
    }

    private void addInfo(Document doc, String label, String value) {
        Text labelText = new Text(label).setBold();
        Text valueText = new Text(value != null ? value : "");
        Paragraph p = new Paragraph().add(labelText).add(valueText);
        doc.add(p);
    }

    private void addParagraph(Document doc, String text) {
        doc.add(new Paragraph(text).setFontSize(10));
    }

    private String calculateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular Hash SHA-256", e);
        }
    }
}