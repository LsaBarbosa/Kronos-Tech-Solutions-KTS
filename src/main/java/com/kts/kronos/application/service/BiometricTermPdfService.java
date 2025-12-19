package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
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

    // CORREÇÃO: Injetando valor do YAML
    @Value("${kronos.security.biometric-term-salt}")
    private String secretSalt;

    public byte[] generateConsentTerm(Employee employee, Company company, String ipAddress) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Dados do Momento da Assinatura
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(DATE_FMT);
            String finalIp = (ipAddress != null ? ipAddress : "IP Não Identificado");

            // --- GERAÇÃO DO HASH REAL (SHA-256) ---
            // Usamos o 'this.secretSalt' injetado pelo Spring
            String dataToSign = String.format("%s|%s|%s|%s|%s",
                    employee.cpf(),
                    company.cnpj(),
                    formattedDate,
                    finalIp,
                    this.secretSalt      // <--- Uso da chave do YAML
            );

            String validationHash = calculateSha256(dataToSign);
            // ----------------------------------------

            // Título
            document.add(new Paragraph("TERMO DE CONSENTIMENTO PARA TRATAMENTO DE DADOS BIOMÉTRICOS")
                    .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Identificação
            document.add(new Paragraph("Pelo presente instrumento, de um lado:"));
            document.add(new Paragraph(String.format("EMPREGADOR: %s, CNPJ: %s", company.name(), company.cnpj())).setBold());
            document.add(new Paragraph("E de outro lado:"));
            document.add(new Paragraph(String.format("COLABORADOR: %s, CPF: %s", employee.fullName(), employee.cpf())).setBold());
            document.add(new Paragraph("\n"));

            // Texto Legal
            String text = "O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, especificamente sua IMAGEM FACIAL (Biometria), para a finalidade exclusiva de REGISTRO E CONTROLE DE JORNADA DE TRABALHO, em conformidade com a LGPD e Portaria 671/2021.\n\n" +
                    "1. FINALIDADE: Autenticação da identidade no registro de ponto.\n" +
                    "2. ARMAZENAMENTO: Ambiente seguro (SaaS) da KRONOS TECH SOLUTIONS.\n" +
                    "3. REVOGAÇÃO: O consentimento pode ser revogado a qualquer momento.";
            document.add(new Paragraph(text).setTextAlignment(TextAlignment.JUSTIFIED));

            // --- RODAPÉ DE VALIDAÇÃO ---
            document.add(new Paragraph("\n\n-------------------------------------------------------------"));
            document.add(new Paragraph("REGISTRO DE ACEITE ELETRÔNICO").setBold());
            document.add(new Paragraph("Documento assinado digitalmente na plataforma KRONOS."));
            document.add(new Paragraph("Data/Hora: " + formattedDate));
            document.add(new Paragraph("IP de Origem: " + finalIp));
            document.add(new Paragraph("ID do Usuário: " + employee.employeeId()));

            // Exibe o Hash Verdadeiro
            document.add(new Paragraph("Código de Validação (Hash SHA-256):").setBold().setFontSize(8));
            document.add(new Paragraph(validationHash).setFontSize(8));

            document.add(new Paragraph("\nA integridade deste documento pode ser verificada tecnicamente recriando o hash com os dados acima.").setFontSize(7).setItalic());

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Termo de Consentimento", e);
            throw new RuntimeException("Falha na geração do Termo PDF");
        }
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