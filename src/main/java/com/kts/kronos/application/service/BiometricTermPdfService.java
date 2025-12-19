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
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class BiometricTermPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Value("${kronos.security.biometric-term-salt}")
    private String secretSalt;

    public byte[] generateConsentTerm(Employee employee, Company company, String ipAddress, String userAgent) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            // Define tamanho A4
            pdf.setDefaultPageSize(PageSize.A4);

            Document document = new Document(pdf);
            document.setMargins(40, 40, 40, 40); // Margens elegantes

            // --- 1. CARREGAMENTO DE FONTES (CORREÇÃO DO ERRO) ---
            PdfFont fontBody = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont fontTech = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontTechBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // --- 2. DADOS DE AUDITORIA ---
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(DATE_FMT);
            String finalIp = (ipAddress != null && !ipAddress.isEmpty()) ? ipAddress : "IP Não Identificado";
            String finalUserAgent = (userAgent != null && !userAgent.isEmpty()) ? userAgent : "Dispositivo Desconhecido";

            // Hash Calculation
            String dataToSign = String.format("%s|%s|%s|%s|%s|%s",
                    employee.cpf(), company.cnpj(), formattedDate, finalIp, finalUserAgent, this.secretSalt);
            String validationHash = calculateSha256(dataToSign);

            // --- 3. CONSTRUÇÃO DO LAYOUT ---

            // TÍTULO
            Paragraph title = new Paragraph("TERMO DE CONSENTIMENTO PARA\nTRATAMENTO DE DADOS BIOMÉTRICOS")
                    .setFont(fontBold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // IDENTIFICAÇÃO DAS PARTES (Caixa sutil)
            Table partiesTable = new Table(UnitValue.createPercentArray(new float[]{20, 80}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            partiesTable.addCell(createLabelCell("EMPREGADOR:", fontTechBold));
            partiesTable.addCell(createValueCell(company.name() + " (CNPJ: " + company.cnpj() + ")", fontTech));

            partiesTable.addCell(createLabelCell("COLABORADOR:", fontTechBold));
            partiesTable.addCell(createValueCell(employee.fullName() + " (CPF: " + employee.cpf() + ")", fontTech));

            document.add(partiesTable);

            // TEXTO LEGAL (Justificado e Elegante)
            String legalTextContent = "O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, especificamente sua IMAGEM FACIAL (Biometria), para a finalidade exclusiva de REGISTRO E CONTROLE DE JORNADA DE TRABALHO, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e a Portaria 671/2021 do Ministério do Trabalho e Previdência.";

            document.add(new Paragraph(legalTextContent)
                    .setFont(fontBody).setFontSize(12)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setFirstLineIndent(30)
                    .setMarginBottom(10));

            // LISTA DE ITENS
            com.itextpdf.layout.element.List list = new com.itextpdf.layout.element.List()
                    .setSymbolIndent(12)
                    .setListSymbol("\u2022") // Bullet point
                    .setFont(fontBody).setFontSize(12)
                    .setMarginBottom(20)
                    .setMarginLeft(20);

            list.add(new ListItem("FINALIDADE: Autenticação segura da identidade no momento do registro de ponto eletrônico, prevenindo fraudes."));
            list.add(new ListItem("ARMAZENAMENTO: Os dados serão armazenados em ambiente seguro de computação em nuvem (SaaS) provido pela KRONOS TECH SOLUTIONS."));
            list.add(new ListItem("REVOGAÇÃO: Este consentimento poderá ser revogado a qualquer momento pelo Titular, mediante solicitação expressa ao departamento de Recursos Humanos."));

            document.add(list);

            // --- SEÇÃO DE VALIDAÇÃO TÉCNICA (Estilo "Certificado") ---
            // Criamos uma caixa com borda para dar peso jurídico

            document.add(new Paragraph("\n")); // Espaço

            Table certTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
            certTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

            // Cabeçalho da Caixa
            Cell headerCell = new Cell().add(new Paragraph("REGISTRO DE ACEITE ELETRÔNICO (Assinatura Eletrônica Avançada)")
                    .setFont(fontTechBold).setFontSize(10).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER));
            headerCell.setBackgroundColor(DeviceGray.BLACK); // Fundo preto, texto branco
            certTable.addHeaderCell(headerCell);

            // Corpo da Caixa
            Cell bodyCell = new Cell();
            bodyCell.setPadding(10);

            bodyCell.add(new Paragraph("Este documento foi assinado digitalmente através da plataforma KRONOS, garantindo autenticidade e integridade conforme MP 2.200-2/2001.")
                    .setFont(fontTech).setFontSize(9).setItalic().setMarginBottom(10));

            bodyCell.add(new Paragraph("Data/Hora do Aceite: ").setFont(fontTechBold).setFontSize(9)
                    .add(new Text(formattedDate).setFont(fontTech)));

            bodyCell.add(new Paragraph("Endereço IP de Origem: ").setFont(fontTechBold).setFontSize(9)
                    .add(new Text(finalIp).setFont(fontTech)));

            // Tratamento User Agent
            String displayUA = finalUserAgent.length() > 90 ? finalUserAgent.substring(0, 90) + "..." : finalUserAgent;
            bodyCell.add(new Paragraph("Dispositivo/Navegador: ").setFont(fontTechBold).setFontSize(9)
                    .add(new Text(displayUA).setFont(fontTech)));

            bodyCell.add(new Paragraph("ID Único do Usuário: ").setFont(fontTechBold).setFontSize(9)
                    .add(new Text(employee.employeeId().toString()).setFont(fontTech)));

            // Hash em destaque
            bodyCell.add(new Paragraph("\nCÓDIGO DE VALIDAÇÃO (HASH SHA-256):")
                    .setFont(fontTechBold).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            bodyCell.add(new Paragraph(validationHash)
                    .setFont(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD)) // Fonte monoespaçada para o hash
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER)
                    .setBackgroundColor(new DeviceGray(0.95f)) // Fundo cinza claro para o hash
                    .setPadding(4));

            certTable.addCell(bodyCell);
            document.add(certTable);

            // Rodapé simples
            document.add(new Paragraph("Kronos Tech Solutions - Tecnologia em Gestão de Ponto")
                    .setFont(fontTech).setFontSize(7).setFontColor(DeviceGray.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Termo de Consentimento", e);
            throw new RuntimeException("Falha na geração do Termo PDF: " + e.getMessage());
        }
    }

    // Métodos auxiliares para tabela limpa
    private Cell createLabelCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(null)
                .setPaddingBottom(5);
    }

    private Cell createValueCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(null)
                .setPaddingBottom(5);
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular Hash SHA-256", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}