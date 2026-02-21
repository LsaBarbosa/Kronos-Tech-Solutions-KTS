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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
public class BiometricTermPdfService {

     @Value("${kronos.security.biometric-term-salt}")
    private String secretSalt;

    public byte[] generateConsentTerm(Employee employee, Company company, String ipAddress, String userAgent) {

        log.info(LOG_INIT_GENERATION, employee.employeeId());

        try (var baos = new ByteArrayOutputStream()) {
            var writer = new PdfWriter(baos);
            var pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);

            var document = new Document(pdf);
            document.setMargins(40, 40, 40, 40);

            // --- 1. CARREGAMENTO DE FONTES ---
            var fontBody = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            var fontBold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            var fontTech = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            var fontTechBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // --- 2. DADOS DE AUDITORIA E VALIDAÇÃO ---
            var formattedDate = LocalDateTime.now().format(DATE_TIME);

            // Otimização: Usa isBlank() em vez de isEmpty() para evitar Strings preenchidas só com espaços
            var finalIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : UNKNOWN_IP;
            var finalUserAgent = (userAgent != null && !userAgent.isBlank()) ? userAgent : UNKNOWN_DEVICE;

            var dataToSign = String.format("%s|%s|%s|%s|%s|%s", employee.cpf(), company.cnpj(), formattedDate, finalIp, finalUserAgent, this.secretSalt);
            var validationHash = calculateSha256(dataToSign);

            // --- 3. CONSTRUÇÃO DO LAYOUT ---

            // TÍTULO
            document.add(new Paragraph(TITLE_TEXT)
                    .setFont(fontBold)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // IDENTIFICAÇÃO DAS PARTES (Caixa sutil)
            var partiesTable = new Table(UnitValue.createPercentArray(new float[]{20, 80}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            partiesTable.addCell(createLabelCell(LABEL_EMPLOYER, fontTechBold));
            partiesTable.addCell(createValueCell(String.format("%s (CNPJ: %s)", company.name(), company.cnpj()), fontTech));

            partiesTable.addCell(createLabelCell(LABEL_EMPLOYEE, fontTechBold));
            partiesTable.addCell(createValueCell(String.format("%s (CPF: %s)", employee.fullName(), employee.cpf()), fontTech));

            document.add(partiesTable);

            document.add(new Paragraph(LEGAL_PARAGRAPH)
                    .setFont(fontBody)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setFirstLineIndent(30)
                    .setMarginBottom(10));
            var list = new com.itextpdf.layout.element.List()
                    .setSymbolIndent(12)
                    .setListSymbol(BULLET_SYMBOL)
                    .setFont(fontBody)
                    .setFontSize(12)
                    .setMarginBottom(20)
                    .setMarginLeft(20);

            list.add(new ListItem(BULLET_PURPOSE));
            list.add(new ListItem(BULLET_STORAGE));
            list.add(new ListItem(BULLET_REVOCATION));
            document.add(list);

            // SEÇÃO DE VALIDAÇÃO TÉCNICA
            document.add(new Paragraph("\n"));

            var certTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
            certTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

            var headerCell = new Cell().add(new Paragraph(BOX_HEADER_TEXT)
                    .setFont(fontTechBold)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER));
            headerCell.setBackgroundColor(DeviceGray.BLACK);
            certTable.addHeaderCell(headerCell);

            var bodyCell = new Cell().setPadding(10);
            bodyCell.add(new Paragraph(BOX_BODY_DISCLAIMER)
                    .setFont(fontTech)
                    .setFontSize(9)
                    .setItalic()
                    .setMarginBottom(10));

            bodyCell.add(new Paragraph(LABEL_DATE).setFont(fontTechBold).setFontSize(9).add(new Text(formattedDate).setFont(fontTech)));
            bodyCell.add(new Paragraph(LABEL_IP).setFont(fontTechBold).setFontSize(9).add(new Text(finalIp).setFont(fontTech)));

            var displayUA = finalUserAgent.length() > 90 ? finalUserAgent.substring(0, 90) + "..." : finalUserAgent;
            bodyCell.add(new Paragraph(LABEL_DEVICE).setFont(fontTechBold).setFontSize(9).add(new Text(displayUA).setFont(fontTech)));
            bodyCell.add(new Paragraph(LABEL_USER_ID).setFont(fontTechBold).setFontSize(9).add(new Text(employee.employeeId().toString()).setFont(fontTech)));

            bodyCell.add(new Paragraph(LABEL_HASH).setFont(fontTechBold).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            bodyCell.add(new Paragraph(validationHash)
                    .setFont(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBackgroundColor(new DeviceGray(0.95f))
                    .setPadding(4));

            certTable.addCell(bodyCell);
            document.add(certTable);

            // RODAPÉ
            document.add(new Paragraph(FOOTER_TEXT)
                    .setFont(fontTech)
                    .setFontSize(7)
                    .setFontColor(DeviceGray.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));

            document.close();

            byte[] finalPdfBytes = baos.toByteArray();
            log.info(LOG_SUCCESS_GENERATION, finalPdfBytes.length);

            return finalPdfBytes;

        } catch (Exception e) {
            log.error(LOG_ERROR_GENERATION, employee.employeeId(), e);
            throw new IllegalStateException(ERROR_TO_GENERATE_PDF + e.getMessage(), e);
        }
    }

    private Cell createLabelCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10)).setBorder(null).setPaddingBottom(5);
    }

    private Cell createValueCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10)).setBorder(null).setPaddingBottom(5);
    }

    private String calculateSha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            log.debug(LOG_HASH_GENERATED);
            return bytesToHex(encodedHash);
        } catch (Exception e) {
            log.error(LOG_ERROR_HASH, e);
            throw new IllegalStateException(ERROR_TO_GENERATE_HASH, e);
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