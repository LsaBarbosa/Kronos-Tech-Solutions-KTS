package com.kts.kronos.application.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.domain.model.Company;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class TechnicalCertificatePdfService {

    public byte[] generateCertificate(Company clientCompany) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Título
            document.add(new Paragraph("ATESTADO TÉCNICO E TERMO DE RESPONSABILIDADE\n(PORTARIA MTP 671/2021)")
                    .setFont(fontBold).setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Identificação do Software House
            addSectionTitle(document, "1. IDENTIFICAÇÃO DO DESENVOLVEDOR (REP-P)", fontBold);
            Table tableDev = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
            addRow(tableDev, "Razão Social:", "KRONOS TECH SOLUTIONS LTDA", fontBold, fontRegular);
            addRow(tableDev, "CNPJ:", "00.000.000/0001-00", fontBold, fontRegular); // Seu CNPJ
            addRow(tableDev, "Software:", "KRONOS SYSTEM v1.0", fontBold, fontRegular);
            document.add(tableDev);
            document.add(new Paragraph("\n"));

            // Identificação do Cliente (A Padaria)
            addSectionTitle(document, "2. IDENTIFICAÇÃO DO EMPREGADOR USUÁRIO", fontBold);
            Table tableCli = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
            addRow(tableCli, "Razão Social:", clientCompany.name(), fontBold, fontRegular);
            addRow(tableCli, "CNPJ:", clientCompany.cnpj(), fontBold, fontRegular);
            document.add(tableCli);
            document.add(new Paragraph("\n"));

            // Texto Legal
            addSectionTitle(document, "3. DECLARAÇÃO DE CONFORMIDADE", fontBold);
            String declaration = "Declaramos para os devidos fins que o software mencionado atende integralmente aos requisitos do REGISTRADOR ELETRÔNICO DE PONTO VIA PROGRAMA (REP-P), conforme Art. 76 a 80 da Portaria 671/2021.\n\n" +
                    "O sistema garante:\n" +
                    "I - Registro fiel das marcações, sem restrições de horário;\n" +
                    "II - Geração do Arquivo Fonte de Dados (AFD) e Arquivo Eletrônico de Jornada (AEJ);\n" +
                    "III - Assinatura digital padrão ICP-Brasil nos documentos fiscais;\n" +
                    "IV - Sincronismo de relógio com NTP.br e auditoria de alterações.";
            
            document.add(new Paragraph(declaration)
                    .setFont(fontRegular).setFontSize(11).setTextAlignment(TextAlignment.JUSTIFIED));
            document.add(new Paragraph("\n"));

            // Assinatura
            document.add(new Paragraph("\n\n__________________________________________________")
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("KRONOS TECH SOLUTIONS\nResponsável Técnico")
                    .setFont(fontBold).setTextAlignment(TextAlignment.CENTER));
            
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
            document.add(new Paragraph("Emitido em: " + date).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            
            // Rodapé
            document.add(new Paragraph("Este documento deve ser apresentado à Auditoria-Fiscal do Trabalho quando solicitado.")
                    .setFontSize(8).setFontColor(DeviceGray.GRAY).setTextAlignment(TextAlignment.CENTER).setFixedPosition(30, 30, 535));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Atestado Técnico", e);
        }
    }

    private void addSectionTitle(Document doc, String title, PdfFont font) {
        doc.add(new Paragraph(title).setFont(font).setFontSize(12).setBackgroundColor(DeviceGray.WHITE));
    }

    private void addRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(10)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(10)).setBorder(null));
    }
}