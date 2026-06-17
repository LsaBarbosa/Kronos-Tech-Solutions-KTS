package com.kts.kronos.application.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
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
 * Aplica ao PDF um carimbo padronizado de evidência de assinatura eletrônica
 * avançada interna (Lei 14.063/2020). O carimbo é desenhado como OVERLAY em
 * cada página do PDF — sem fundo opaco — funcionando como marca d'água que
 * NÃO impede a leitura do conteúdo abaixo.
 *
 * <p>Usado tanto para contratos de serviço quanto para espelhos de ponto:
 * a única coisa que muda entre os fluxos é o conteúdo do {@link EvidenceStamp}.
 * O PDF retornado AINDA NÃO está assinado digitalmente — isso é
 * responsabilidade do {@link com.kts.kronos.infrastructure.DigitalSignatureService#signPdf}.</p>
 */
@Slf4j
@Service
public class EvidenceWatermarkService {

    public record EvidenceStamp(
            String signerFullName,
            Instant signedAt,
            String declarationVersion,
            String canonicalEvidenceHashSha256
    ) {}

    public byte[] applyEvidenceWatermark(byte[] originalPdf, EvidenceStamp stamp) {
        if (originalPdf == null || originalPdf.length == 0) {
            throw new BadRequestException("PDF original está vazio.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = null;
        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(originalPdf));
            PdfWriter writer = new PdfWriter(out);
            pdf = new PdfDocument(reader, writer);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss 'BRT (UTC-03:00)'",
                    new Locale("pt", "BR"));
            String when = stamp.signedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(fmt);
            String hashShort = stamp.canonicalEvidenceHashSha256() != null
                    && stamp.canonicalEvidenceHashSha256().length() >= 16
                    ? stamp.canonicalEvidenceHashSha256().substring(0, 16) + "…"
                    : "—";

            int totalPages = pdf.getNumberOfPages();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                drawWatermarkOnPage(pdf.getPage(pageNum), pdf, stamp, when, hashShort, pageNum, totalPages);
            }

            pdf.close();
            pdf = null;

            byte[] result = out.toByteArray();
            log.info("event=evidence_watermark_applied result=success pages={} bytes={}", totalPages, result.length);
            return result;
        } catch (IOException ex) {
            log.error("event=evidence_watermark_applied result=failure reason=io exception_type={}",
                    ex.getClass().getSimpleName());
            throw new BadRequestException("Falha ao processar o PDF.");
        } catch (RuntimeException ex) {
            log.error("event=evidence_watermark_applied result=failure reason=unexpected exception_type={} msg={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage() == null ? "(no message)" : ex.getMessage());
            throw new BadRequestException("Falha ao processar o PDF (" + ex.getClass().getSimpleName() + ").");
        } finally {
            if (pdf != null) {
                try { pdf.close(); } catch (RuntimeException ignored) { /* best-effort */ }
            }
        }
    }

    private void drawWatermarkOnPage(
            PdfPage page,
            PdfDocument pdf,
            EvidenceStamp stamp,
            String when,
            String hashShort,
            int pageNum,
            int totalPages
    ) {
        Rectangle pageSize = page.getPageSize();
        float blockHeight = 140f;
        float margin = 36f;
        Rectangle stampArea = new Rectangle(
                margin,
                margin,
                pageSize.getWidth() - 2 * margin,
                blockHeight
        );

        PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);

        // Alpha 0.55 — texto semi-transparente, sem pintar fundo. Conteúdo da
        // página por baixo permanece legível como marca d'água tradicional.
        PdfExtGState gs = new PdfExtGState();
        gs.setFillOpacity(0.55f);
        gs.setStrokeOpacity(0.55f);
        pdfCanvas.saveState();
        pdfCanvas.setExtGState(gs);

        try (Canvas layout = new Canvas(pdfCanvas, stampArea)) {
            DeviceRgb purple = new DeviceRgb(124, 58, 237);
            DeviceRgb dark = new DeviceRgb(31, 41, 55);

            Paragraph title = new Paragraph("ASSINATURA ELETRÔNICA REGISTRADA")
                    .setBold().setFontSize(10).setFontColor(purple)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(2);
            layout.add(title);

            Paragraph subtitle = new Paragraph(
                    new Text("Tipo: Assinatura eletrônica avançada interna — não ICP-Brasil. ").setFontSize(7)
            ).add(new Text("Base normativa: Lei nº 14.063/2020.").setFontSize(7))
             .setFontColor(dark).setMarginBottom(4);
            layout.add(subtitle);

            Paragraph block = new Paragraph()
                    .setFontSize(7).setFontColor(dark).setMarginBottom(2)
                    .add(new Text("Signatário: ").setBold()).add(stamp.signerFullName()).add("\n")
                    .add(new Text("Data/hora do aceite: ").setBold()).add(when).add("\n")
                    .add(new Text("Documento/declaração: ").setBold()).add("versão " + stamp.declarationVersion()).add("\n")
                    .add(new Text("Hash canônico dos registros de evidência (SHA-256): ").setBold()).add(hashShort);
            layout.add(block);

            Paragraph evidences = new Paragraph(
                    "Evidências registradas: usuário autenticado, identificador interno do usuário, " +
                    "data/hora do evento, versão do documento, hash do conteúdo aceito, IP, User-Agent " +
                    "e trilha de auditoria."
            ).setFontSize(6).setFontColor(dark).setItalic().setMarginBottom(2);
            layout.add(evidences);

            Paragraph integrity = new Paragraph(
                    "Integridade do PDF: este documento também possui assinatura digital da empresa em " +
                    "formato PAdES/CMS/PKCS#7. A assinatura digital pode ser validada em leitores " +
                    "compatíveis, como Adobe Acrobat Reader."
            ).setFontSize(6).setFontColor(dark).setItalic();
            layout.add(integrity);

            Paragraph footer = new Paragraph(
                    String.format(Locale.ROOT, "Página %d de %d — assinatura registrada eletronicamente",
                            pageNum, totalPages)
            ).setFontSize(5).setFontColor(dark).setTextAlignment(TextAlignment.RIGHT).setMarginTop(2);
            layout.add(footer);
        }

        pdfCanvas.restoreState();
    }
}
