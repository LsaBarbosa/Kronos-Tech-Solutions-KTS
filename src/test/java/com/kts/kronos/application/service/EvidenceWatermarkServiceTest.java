package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.kts.kronos.application.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceWatermarkServiceTest {

    private EvidenceWatermarkService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceWatermarkService();
    }

    private byte[] createMinimalPdf() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer)) {
            pdf.addNewPage();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private EvidenceWatermarkService.EvidenceStamp makeStamp() {
        return new EvidenceWatermarkService.EvidenceStamp(
                "João da Silva",
                Instant.now(),
                "v1.0",
                "abc123def456789012345678901234567890"
        );
    }

    @Test
    void deveAplicarMarcaDaguaEmPdfValido() {
        byte[] pdf = createMinimalPdf();
        var stamp = makeStamp();

        byte[] result = service.applyEvidenceWatermark(pdf, stamp);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void deveLancarBadRequestExceptionParaPdfNulo() {
        var stamp = makeStamp();

        var ex = assertThrows(BadRequestException.class,
                () -> service.applyEvidenceWatermark(null, stamp));
        assertTrue(ex.getMessage().contains("vazio"));
    }

    @Test
    void deveLancarBadRequestExceptionParaPdfVazio() {
        var stamp = makeStamp();

        var ex = assertThrows(BadRequestException.class,
                () -> service.applyEvidenceWatermark(new byte[0], stamp));
        assertTrue(ex.getMessage().contains("vazio"));
    }

    @Test
    void deveLancarBadRequestExceptionParaBytesInvalidos() {
        byte[] garbage = "not a pdf at all".getBytes();
        var stamp = makeStamp();

        var ex = assertThrows(BadRequestException.class,
                () -> service.applyEvidenceWatermark(garbage, stamp));
        assertNotNull(ex.getMessage());
    }

    @Test
    void deveUsarHashCurtoQuandoHashEhNulo() {
        byte[] pdf = createMinimalPdf();
        var stamp = new EvidenceWatermarkService.EvidenceStamp(
                "Signatário Teste", Instant.now(), "v1.0", null
        );

        byte[] result = service.applyEvidenceWatermark(pdf, stamp);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void deveUsarHashCurtoQuandoHashTemMenosDe16Caracteres() {
        byte[] pdf = createMinimalPdf();
        var stamp = new EvidenceWatermarkService.EvidenceStamp(
                "Signatário Teste", Instant.now(), "v1.0", "short"
        );

        byte[] result = service.applyEvidenceWatermark(pdf, stamp);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void deveAplicarMarcaDaguaEmPdfComMultiplasPaginas() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer)) {
            pdf.addNewPage();
            pdf.addNewPage();
            pdf.addNewPage();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        byte[] pdfBytes = out.toByteArray();

        byte[] result = service.applyEvidenceWatermark(pdfBytes, makeStamp());

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ── BR L93 (pdf != null in finally) + LINE 94: drawWatermarkOnPage throws ──
    @Test
    void applyEvidenceWatermark_nullSignerName_triggersRuntimeExceptionWithPdfOpen() {
        // null signerFullName → NPE in drawWatermarkOnPage AFTER pdf is created
        // → RuntimeException catch fires, and in finally: pdf != null → pdf.close()
        byte[] pdf = createMinimalPdf();
        var stamp = new EvidenceWatermarkService.EvidenceStamp(
                null, // null signerFullName → NPE when added to Paragraph
                java.time.Instant.now(),
                "v1.0",
                "abc123def456789012345678901234567890"
        );

        // Expect BadRequestException wrapping the RuntimeException from drawWatermarkOnPage
        assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> service.applyEvidenceWatermark(pdf, stamp));
    }

}