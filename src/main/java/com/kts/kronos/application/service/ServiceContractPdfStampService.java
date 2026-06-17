package com.kts.kronos.application.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.application.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Anexa ao PDF original do contrato uma página final de evidência de assinatura
 * eletrônica avançada interna (Lei 14.063/2020). O PDF retornado AINDA NÃO está
 * assinado digitalmente — isso é responsabilidade do {@code DigitalSignatureService.signPdf}.
 */
@Slf4j
@Service
public class ServiceContractPdfStampService {

    public record EvidenceStamp(
            String signerFullName,
            Instant signedAt,
            String declarationVersion,
            String contractDocumentHashSha256,
            String contractTitle
    ) {}

    public byte[] appendEvidencePage(byte[] originalPdf, EvidenceStamp stamp) {
        if (originalPdf == null || originalPdf.length == 0) {
            throw new BadRequestException("PDF original do contrato está vazio.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = null;
        try {
            // NÃO usar try-with-resources em PdfDocument E Document — em iText 7,
            // fechar o Document já fecha o PdfDocument; double-close pode produzir
            // warnings e em alguns casos invalidar a saída.
            PdfReader reader = new PdfReader(new ByteArrayInputStream(originalPdf));
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(reader, writer);
            doc = new Document(pdf);

            // Apenas NEXT_PAGE: força tudo a começar em uma página nova ao final do doc.
            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            doc.add(new Paragraph("ASSINATURA ELETRÔNICA REGISTRADA")
                    .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(8));

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z", new Locale("pt", "BR"));
            String when = stamp.signedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(fmt);
            String hashShort = stamp.contractDocumentHashSha256() != null && stamp.contractDocumentHashSha256().length() >= 16
                    ? stamp.contractDocumentHashSha256().substring(0, 16) + "…"
                    : "—";

            Table evidence = new Table(UnitValue.createPercentArray(new float[]{1}));
            evidence.setWidth(UnitValue.createPercentValue(100));

            Paragraph content = new Paragraph()
                    .add(new Text("Contrato: ").setBold()).add(stamp.contractTitle()).add("\n")
                    .add(new Text("Signatário (ciência): ").setBold()).add(stamp.signerFullName()).add("\n")
                    .add(new Text("Data/Hora da ciência: ").setBold()).add(when).add("\n")
                    .add(new Text("Declaração: ").setBold()).add("versão " + stamp.declarationVersion()).add("\n")
                    .add(new Text("Resumo SHA-256 do PDF original (prefixo): ").setBold()).add(hashShort).add("\n")
                    .add(new Text("Tipo de assinatura: ").setBold()).add("Eletrônica avançada interna (INTERNAL_ADVANCED)").add("\n")
                    .add(new Text("Método: ").setBold()).add("Reautenticação por senha (PASSWORD_REAUTH)").add("\n")
                    .add(new Text("Base legal: ").setBold()).add("Lei 14.063/2020 art. 4º, II — não-ICP-Brasil.").add("\n\n")
                    .add(new Text("Este PDF também é assinado digitalmente pelo certificado da empresa " +
                            "(PAdES/PKCS#7). A integridade pode ser verificada em qualquer leitor compatível " +
                            "(Adobe Reader, ITI gov.br).").setItalic().setFontSize(9));

            Cell cell = new Cell().add(content)
                    .setBackgroundColor(new DeviceRgb(245, 240, 255))
                    .setBorder(new SolidBorder(new DeviceRgb(124, 58, 237), 1.0f))
                    .setPadding(10);
            evidence.addCell(cell);
            doc.add(evidence);

            // Fecha o Document AQUI (antes do return), garantindo que writer faz flush
            // e os bytes ficam consistentes em `out` antes de toByteArray().
            doc.close();
            doc = null;

            byte[] result = out.toByteArray();
            log.info("event=contract_evidence_page_appended result=success bytes={}", result.length);
            return result;
        } catch (IOException ex) {
            log.error("event=contract_evidence_page_appended result=failure reason=io exception_type={}",
                    ex.getClass().getSimpleName());
            throw new BadRequestException("Falha ao processar o PDF do contrato.");
        } catch (RuntimeException ex) {
            // iText pode lançar PdfException, IllegalArgumentException, etc. Surface o tipo
            // exato para diagnóstico via 400, sem vazar mensagem interna do PDF.
            log.error("event=contract_evidence_page_appended result=failure reason=unexpected exception_type={} msg={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage() == null ? "(no message)" : ex.getMessage());
            throw new BadRequestException("Falha ao processar o PDF do contrato (" + ex.getClass().getSimpleName() + ").");
        } finally {
            if (doc != null) {
                try {
                    doc.close();
                } catch (RuntimeException ignored) {
                    // best-effort close em caminho de erro
                }
            }
        }
    }
}
