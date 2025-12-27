package com.kts.kronos.application.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.domain.model.Company;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
public class TechnicalCertificatePdfService {

    // Injeção de dados via application.yml para evitar dados fixos no código
    @Value("${kronos.legal.dev-name:KRONOS TECH SOLUTIONS LTDA}")
    private String devName;

    @Value("${kronos.legal.dev-cnpj:00.000.000/0001-00}")
    private String devCnpj;

    @Value("${kronos.legal.software-version:1.0}")
    private String softwareVersion;

    public byte[] generateCertificate(Company clientCompany) {
        try (var baos = new ByteArrayOutputStream()) {
            var writer = new PdfWriter(baos);
            var pdf = new PdfDocument(writer);
            var document = new Document(pdf, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            // Fontes padrão (não precisa de arquivo .ttf externo)
            var fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            var fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Título
            document.add(new Paragraph("ATESTADO TÉCNICO E TERMO DE RESPONSABILIDADE\n(PORTARIA MTP 671/2021)")
                    .setFont(fontBold).setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // 1. Identificação do Software House (Fabricante)
            addSectionTitle(document, "1. IDENTIFICAÇÃO DO DESENVOLVEDOR (REP-P)", fontBold);
            var tableDev = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();

            // Usa as variáveis injetadas em vez de texto fixo
            addRow(tableDev, "Razão Social:", this.devName, fontBold, fontRegular);
            addRow(tableDev, "CNPJ:", this.devCnpj, fontBold, fontRegular);
            addRow(tableDev, "Software:", "KRONOS SYSTEM v" + this.softwareVersion, fontBold, fontRegular);

            document.add(tableDev);
            document.add(new Paragraph("\n"));

            // 2. Identificação do Cliente (A Padaria)
            addSectionTitle(document, "2. IDENTIFICAÇÃO DO EMPREGADOR USUÁRIO", fontBold);
            var tableCli = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
            addRow(tableCli, "Razão Social:", clientCompany.name(), fontBold, fontRegular);

            // Formata o CNPJ do cliente para ficar bonito (opcional, mas recomendado)
            addRow(tableCli, "CNPJ:", formatCnpj(clientCompany.cnpj()), fontBold, fontRegular);
            document.add(tableCli);
            document.add(new Paragraph("\n"));

            // 3. Texto Legal (Declaração)
            addSectionTitle(document, "3. DECLARAÇÃO DE CONFORMIDADE", fontBold);
            var declaration = "Declaramos para os devidos fins que o software mencionado atende integralmente aos requisitos do REGISTRADOR ELETRÔNICO DE PONTO VIA PROGRAMA (REP-P), conforme Art. 76 a 80 da Portaria 671/2021.\n\n" +
                    "O sistema garante:\n" +
                    "I - Registro fiel das marcações, sem restrições de horário;\n" +
                    "II - Geração do Arquivo Fonte de Dados (AFD) e Arquivo Eletrônico de Jornada (AEJ);\n" +
                    "III - Assinatura digital padrão ICP-Brasil nos documentos fiscais;\n" +
                    "IV - Sincronismo de relógio com NTP.br e auditoria de alterações.";

            document.add(new Paragraph(declaration)
                    .setFont(fontRegular).setFontSize(11).setTextAlignment(TextAlignment.JUSTIFIED));
            document.add(new Paragraph("\n"));

            // Assinatura Visual
            document.add(new Paragraph("\n\n__________________________________________________")
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(this.devName + "\nResponsável Técnico")
                    .setFont(fontBold).setTextAlignment(TextAlignment.CENTER));

            // Data com Locale BR garantido
            var localeBr = new Locale("pt", "BR");
            var date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", localeBr));
            document.add(new Paragraph("Emitido em: " + date).setFontSize(10).setTextAlignment(TextAlignment.CENTER));

            // Rodapé
            document.add(new Paragraph("Este documento digital deve ser validado via assinatura eletrônica P7S anexa.")
                    .setFontSize(8).setFontColor(DeviceGray.GRAY).setTextAlignment(TextAlignment.CENTER).setFixedPosition(30, 30, 535));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF do atestado técnico", e);
            throw new RuntimeException(ERROR_GENERATING_TECHNICAL_CERTIFICATE, e);
        }
    }

    private void addSectionTitle(Document doc, String title, PdfFont font) {
        doc.add(new Paragraph(title).setFont(font).setFontSize(12).setBackgroundColor(DeviceGray.WHITE));
    }

    private void addRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(10)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "").setFont(regular).setFontSize(10)).setBorder(null));
    }

    private String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }
}