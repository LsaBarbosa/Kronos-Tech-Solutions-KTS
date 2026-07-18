package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.kts.kronos.application.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class EvidenceWatermarkServiceCoverage2Test {

    private EvidenceWatermarkService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceWatermarkService();
    }

    private EvidenceWatermarkService.EvidenceStamp stamp() {
        return new EvidenceWatermarkService.EvidenceStamp(
                "Test Signer", Instant.now(), "v1.0", "abc123def456789012345678");
    }

    // L83-86: IOException catch in applyEvidenceWatermark — PdfReader constructor throws
    @Test
    void applyEvidenceWatermark_ioException_throwsBadRequest() {
        try (MockedConstruction<PdfReader> mockReader = mockConstruction(PdfReader.class,
                (mock, ctx) -> { throw new IOException("simulated read error"); })) {

            assertThrows(BadRequestException.class,
                    () -> service.applyEvidenceWatermark(new byte[]{1, 2, 3}, stamp()));
        }
    }

    // L94: RuntimeException in finally pdf.close() when pdf != null
    @Test
    void applyEvidenceWatermark_runtimeExceptionInFinally_suppressedOnClose() {
        PdfPage mockPage = mock(PdfPage.class);
        when(mockPage.getPageSize()).thenReturn(new com.itextpdf.kernel.geom.Rectangle(595, 842));
        when(mockPage.getResources()).thenReturn(mock(com.itextpdf.kernel.pdf.PdfResources.class));

        try (MockedConstruction<PdfReader> mockReader = mockConstruction(PdfReader.class);
             MockedConstruction<PdfDocument> mockDoc = mockConstruction(PdfDocument.class,
                     (mock, ctx) -> {
                         when(mock.getNumberOfPages()).thenThrow(new RuntimeException("forced error"));
                         doThrow(new RuntimeException("close error")).when(mock).close();
                     })) {

            assertThrows(BadRequestException.class,
                    () -> service.applyEvidenceWatermark(new byte[]{1, 2, 3}, stamp()));
        }
    }
}
