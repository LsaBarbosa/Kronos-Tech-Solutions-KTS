package com.kts.kronos.application.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricTermPdfService {
    private final PrivacyLogReferenceService privacyLogReferenceService;

     @Value("${kronos.security.biometric-term-salt}")
    private String secretSalt;

    public byte[] generateConsentTerm(
            Employee employee,
            Company company,
            String ipAddress,
            String userAgent,
            LegalText legalText
    ) {

        try (var baos = new ByteArrayOutputStream()) {
            var writer = new PdfWriter(baos);
            var pdf = new PdfDocument(writer);
            // Define tamanho A4
            pdf.setDefaultPageSize(PageSize.A4);

            var document = new Document(pdf);
            document.setMargins(40, 40, 40, 40); // Margens elegantes

            // --- 1. CARREGAMENTO DE FONTES (CORREÇÃO DO ERRO) ---
            var fontBody = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            var fontBold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            var fontTech = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            var fontTechBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // --- 2. DADOS DE AUDITORIA ---
            var now = LocalDateTime.now();
            var formattedDate = now.format(DATE_TIME);
            var finalIp = (ipAddress != null && !ipAddress.isEmpty()) ? ipAddress : "IP Não Identificado";
            var finalUserAgent = (userAgent != null && !userAgent.isEmpty()) ? userAgent : "Dispositivo Desconhecido";

            // Hash Calculation
            var dataToSign = String.format("%s|%s|%s|%s|%s|%s", employee.cpf(), company.cnpj(), formattedDate, finalIp, finalUserAgent, this.secretSalt);
            var validationHash = calculateSha256(dataToSign);

            // --- 3. CONSTRUÇÃO DO LAYOUT ---

            // TÍTULO
            var title = new Paragraph(legalText.title()).setFont(fontBold).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8);
            document.add(title);
            document.add(new Paragraph("Versão do termo: " + legalText.version())
                    .setFont(fontTech)
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // IDENTIFICAÇÃO DAS PARTES (Caixa sutil)
            var partiesTable = new Table(UnitValue.createPercentArray(new float[]{20, 80})).useAllAvailableWidth().setMarginBottom(20);

            partiesTable.addCell(createLabelCell("EMPREGADOR:", fontTechBold));
            partiesTable.addCell(createValueCell(company.name() + " (CNPJ: " + company.cnpj() + ")", fontTech));

            partiesTable.addCell(createLabelCell("COLABORADOR:", fontTechBold));
            partiesTable.addCell(createValueCell(employee.fullName() + " (CPF: " + employee.cpf() + ")", fontTech));

            document.add(partiesTable);

            renderLegalTextContent(document, fontBody, legalText.content());

            // --- SEÇÃO DE VALIDAÇÃO TÉCNICA (Estilo "Certificado") ---
            // Criamos uma caixa com borda para dar peso jurídico

            document.add(new Paragraph("\n")); // Espaço

            var certTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
            certTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

            // Cabeçalho da Caixa
            var headerCell = new Cell().add(new Paragraph("REGISTRO DE ACEITE ELETRÔNICO (Assinatura Eletrônica Avançada)").setFont(fontTechBold).setFontSize(10).setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER));
            headerCell.setBackgroundColor(DeviceGray.BLACK); // Fundo preto, texto branco
            certTable.addHeaderCell(headerCell);

            // Corpo da Caixa
            var bodyCell = new Cell();
            bodyCell.setPadding(10);

            bodyCell.add(new Paragraph("Este documento foi assinado digitalmente através da plataforma KRONOS, garantindo autenticidade e integridade conforme MP 2.200-2/2001.").setFont(fontTech).setFontSize(9).setItalic().setMarginBottom(10));

            bodyCell.add(new Paragraph("Data/Hora do Aceite: ").setFont(fontTechBold).setFontSize(9).add(new Text(formattedDate).setFont(fontTech)));

            bodyCell.add(new Paragraph("Endereço IP de Origem: ").setFont(fontTechBold).setFontSize(9).add(new Text(finalIp).setFont(fontTech)));

            // Tratamento User Agent
            var displayUA = finalUserAgent.length() > 90 ? finalUserAgent.substring(0, 90) + "..." : finalUserAgent;
            bodyCell.add(new Paragraph("Dispositivo/Navegador: ").setFont(fontTechBold).setFontSize(9).add(new Text(displayUA).setFont(fontTech)));

            bodyCell.add(new Paragraph("ID Único do Usuário: ").setFont(fontTechBold).setFontSize(9).add(new Text(employee.employeeId().toString()).setFont(fontTech)));

            // Hash em destaque
            bodyCell.add(new Paragraph("\nCÓDIGO DE VALIDAÇÃO (HASH SHA-256):").setFont(fontTechBold).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            bodyCell.add(new Paragraph(validationHash).setFont(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD)) // Fonte monoespaçada para o hash
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(new DeviceGray(0.95f)) // Fundo cinza claro para o hash
                    .setPadding(4));

            certTable.addCell(bodyCell);
            document.add(certTable);

            // Rodapé simples
            document.add(new Paragraph("Kronos Tech Solutions - Tecnologia em Gestão de Ponto").setFont(fontTech).setFontSize(7).setFontColor(DeviceGray.GRAY).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

            document.close();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("event=biometric_consent_term_pdf_io_error employeeRef={} companyRef={}",
                    privacyLogReferenceService.employeeRef(employee.employeeId()),
                    privacyLogReferenceService.companyRef(company.companyId()),
                    e);
            throw new RuntimeException(ERROR_TO_GENERATE_PDF, e);
        } catch (RuntimeException e) {
            log.error("Erro ao gerar Termo de Consentimento", e);
            throw new RuntimeException(ERROR_TO_GENERATE_PDF, e);
        }
    }

    // Métodos auxiliares para tabela limpa
    private Cell createLabelCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10)).setBorder(null).setPaddingBottom(5);
    }

    private Cell createValueCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10)).setBorder(null).setPaddingBottom(5);
    }

    private void renderLegalTextContent(Document document, PdfFont fontBody, String content) {
        List<String> listItems = new ArrayList<>();
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                flushList(document, fontBody, listItems);
                continue;
            }

            if (line.startsWith("- ")) {
                listItems.add(line.substring(2).trim());
                continue;
            }

            flushList(document, fontBody, listItems);
            document.add(new Paragraph(line)
                    .setFont(fontBody)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setFirstLineIndent(30)
                    .setMarginBottom(10));
        }
        flushList(document, fontBody, listItems);
    }

    private void flushList(Document document, PdfFont fontBody, List<String> listItems) {
        if (listItems.isEmpty()) {
            return;
        }

        var list = new com.itextpdf.layout.element.List()
                .setSymbolIndent(12)
                .setListSymbol("\u2022")
                .setFont(fontBody)
                .setFontSize(12)
                .setMarginBottom(20)
                .setMarginLeft(20);

        for (String item : listItems) {
            list.add(new ListItem(item));
        }

        document.add(list);
        listItems.clear();
    }

    private String calculateSha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ERROR_TO_GENERATE_HASH, e);
        }
    }

    private String bytesToHex(byte[] hash) {
        var hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            var hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
