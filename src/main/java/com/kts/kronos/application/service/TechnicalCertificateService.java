package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.TechnicalCertificateUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Company;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import static com.kts.kronos.constants.LegalCompanyData.*;
import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalCertificateService implements TechnicalCertificateUseCase {


    private final CompanyProvider companyProvider;

    @Override
    public byte[] generateCertificate(UUID companyId) {
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        try (var baos = new ByteArrayOutputStream()) {
            var writer = new PdfWriter(baos);
            var pdf = new PdfDocument(writer);
            var document = new Document(pdf);

            // 1. TÍTULO
            addTitle(document, "ATESTADO TÉCNICO E TERMO DE RESPONSABILIDADE");
            addParagraph(document, "\n");

            // 2. IDENTIFICAÇÃO DO FABRICANTE (KRONOS)
            addSection(document, "1. IDENTIFICAÇÃO DO DESENVOLVEDOR (FABRICANTE)");
            addInfo(document, "Razão Social: ", DEV_RAZAO_SOCIAL);
            addInfo(document, "CNPJ: ", DEV_CNPJ);

            // 3. IDENTIFICAÇÃO DO SOFTWARE
            addSection(document, "2. IDENTIFICAÇÃO DO PROGRAMA (REP-P)");
            addInfo(document, "Nome do Programa: ", SOFTWARE_NAME);
            addInfo(document, "Versão: ", SOFTWARE_VERSION);
            addInfo(document, "Número de Registro no INPI: ", INPI_NUMBER);

            // 4. IDENTIFICAÇÃO DO EMPREGADOR (CLIENTE)
            addSection(document, "3. IDENTIFICAÇÃO DO EMPREGADOR (USUÁRIO)");
            addInfo(document, "Razão Social: ", company.name());
            addInfo(document, "CNPJ: ", company.cnpj());
            addInfo(document, "Endereço: ", formatAddress(company));

            // 5. DECLARAÇÃO JURÍDICA (TEXTO OBRIGATÓRIO/PADRÃO)
            addSection(document, "4. DECLARAÇÃO DE CONFORMIDADE");
            var declaration = "Declaramos, para fins de comprovação junto à Auditoria-Fiscal do Trabalho, que o programa de computador acima identificado, denominado REP-P (Registrador Eletrônico de Ponto via Programa), atende integralmente aos requisitos estabelecidos pela Portaria MTP nº 671, de 8 de novembro de 2021, especialmente quanto:\n" +
                    "a) Ao registro fiel das marcações de ponto;\n" +
                    "b) À não restrição de marcação de ponto;\n" +
                    "c) À não alteração ou eliminação dos dados registrados pelo empregado;\n" +
                    "d) À geração do Arquivo Fonte de Dados (AFD);\n" +
                    "e) À geração do Arquivo Eletrônico de Jornada (AEJ);\n" +
                    "f) À emissão do Comprovante de Registro de Ponto do Trabalhador.";
            
            document.add(new Paragraph(declaration).setTextAlignment(TextAlignment.JUSTIFIED));

            // 6. TERMO DE RESPONSABILIDADE
            addSection(document, "5. TERMO DE RESPONSABILIDADE");
            var responsibility = "A empresa desenvolvedora assume a responsabilidade técnica pelo funcionamento do programa e garante que este não possui mecanismos que permitam a adulteração dos dados de ponto ou o bloqueio à marcação, estando sujeito às sanções legais em caso de desconformidade.";
            document.add(new Paragraph(responsibility).setTextAlignment(TextAlignment.JUSTIFIED));

            // 7. DATA E ASSINATURA
            addParagraph(document, "\n\n");
            addParagraph(document, "Magé, RJ, " + LocalDateTime.now().format(DATE_FMT_BR));
            addParagraph(document, "\n\n___________________________________________________");
            addParagraph(document, "Assinado Eletronicamente por");
            addParagraph(document, DEV_RAZAO_SOCIAL);
            addParagraph(document, "Responsável Técnico / Representante Legal");

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Atestado Técnico", e);
            throw new RuntimeException(ERROR_GENERATING_TECHNICAL_CERTIFICATE, e);
        }
    }

    private void addTitle(Document doc, String text) {
        doc.add(new Paragraph(text).setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(14));
    }

    private void addSection(Document doc, String text) {
        doc.add(new Paragraph("\n" + text).setBold().setFontSize(11));
    }

    private void addInfo(Document doc, String label, String value) {
        doc.add(new Paragraph().add(new Text(label).setBold()).add(new Text(value != null ? value : "")));
    }
    
    private void addParagraph(Document doc, String text) {
        doc.add(new Paragraph(text));
    }

    private String formatAddress(Company c) {
        if (c.address() == null) return ADDRESS_NOT_REGISTERED;
        return String.format("%s, %s - %s, %s - %s",
                c.address().street(), c.address().number(),
                c.address().city(), c.address().state(), c.address().postalCode());
    }
}