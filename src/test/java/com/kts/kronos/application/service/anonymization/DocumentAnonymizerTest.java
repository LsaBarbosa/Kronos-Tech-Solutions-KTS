package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DocumentAnonymizerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private BucketStorageProvider bucketStorageProvider;

    @InjectMocks
    private DocumentAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.DOCUMENT, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoDocuments() {
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenReturn(new ArrayList<>());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
    }

    @Test
    void testExecuteDryRunWithDocuments() {
        var docs = Arrays.asList(createDocument(), createDocument());
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenReturn(docs);

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteApplyDeletesS3AndAnonymizes() {
        var doc1 = createDocument();
        var doc2 = createDocument();
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenReturn(Arrays.asList(doc1, doc2));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.affectedCount());

        verify(bucketStorageProvider, times(2)).deleteFile(any(), anyString());
        verify(documentRepository, times(2)).save(any());
    }

    @Test
    void testExecuteApplyHandlesS3Failure() {
        var doc1 = createDocument();
        var doc2 = createDocument();
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenReturn(Arrays.asList(doc1, doc2));
        doThrow(new RuntimeException("S3 error")).when(bucketStorageProvider).deleteFile(any(), anyString());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("PARTIAL", result.status());
        assertEquals(2, result.errorCount());
    }

    @Test
    void documentAnonymizer_shouldNotLogRawStoragePath(CapturedOutput output) {
        var doc = createDocument();
        doc.setStoragePath("company/123/employee/456/private/term.pdf");
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenReturn(Arrays.asList(doc));
        doThrow(new RuntimeException("S3 error")).when(bucketStorageProvider).deleteFile(any(), anyString());

        anonymizer.execute(createPlan(), "APPLY");

        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("storageRef=" + SensitiveDataMasker.maskStorageReference(doc.getStoragePath())));
        assertTrue(logs.contains("exception_type=RuntimeException"));
        assertFalse(logs.contains(doc.getStoragePath()));
        assertFalse(logs.contains("storagePath=" + doc.getStoragePath()));
    }

    @Test
    void testExecuteApplyAnonymizesFileName() {
        var doc = createDocument();
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenReturn(Arrays.asList(doc));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertTrue(saved.getFileName().startsWith("anon_"));
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    private AnonymizationPlan createPlan() {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test anonymization",
                false,
                false,
                false,
                true,
                false,
                false
        );
    }

    private DocumentEntity createDocument() {
        return DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .fileName("test.pdf")
                .contentType("application/pdf")
                .type(DocumentType.DOCUMENTS)
                .storagePath("path/to/file.pdf")
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
