package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRetentionProcessorTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private BucketStorageProvider bucketStorageProvider;

    @InjectMocks
    private DocumentRetentionProcessor processor;

    @Test
    void testSupports() {
        assertEquals(RetentionResourceType.DOCUMENT, processor.supports());
    }

    @Test
    void testExecuteDryRunWithNoRemovableDocuments() {
        when(documentRepository.countRemovableByRetention(any())).thenReturn(0L);
        when(documentRepository.countPreservedByType(any())).thenReturn(0L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteDryRunWithRemovableDocuments() {
        when(documentRepository.countRemovableByRetention(any())).thenReturn(10L);
        when(documentRepository.countPreservedByType(any())).thenReturn(0L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(10, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteDryRunWithRemovableAndPreservedDocuments() {
        when(documentRepository.countRemovableByRetention(any())).thenReturn(10L);
        when(documentRepository.countPreservedByType(any())).thenReturn(5L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(15, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(5, result.skippedCount());
    }

    @Test
    void testExecuteApplyDeletesFromS3AndMarksInDB() {
        var doc1 = createDocumentEntity(DocumentType.DOCUMENTS, "path1");
        var doc2 = createDocumentEntity(DocumentType.PAYSLIP, "path2");
        var removableDocuments = Arrays.asList(doc1, doc2);

        when(documentRepository.findRemovableByRetention(any())).thenReturn(removableDocuments);
        when(documentRepository.markAsDeletedByRetention(any(), any(), anyString())).thenReturn(2);
        when(documentRepository.countPreservedByType(any())).thenReturn(3L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(5, result.scannedCount());
        assertEquals(2, result.affectedCount());
        assertEquals(3, result.skippedCount());

        verify(bucketStorageProvider, times(2)).deleteFile(any(), anyString());
        verify(documentRepository, times(1)).markAsDeletedByRetention(any(LocalDateTime.class), any(LocalDateTime.class), anyString());
    }

    @Test
    void testExecuteApplyPreservesLegalDocuments() {
        when(documentRepository.findRemovableByRetention(any())).thenReturn(Arrays.asList());
        when(documentRepository.markAsDeletedByRetention(any(), any(), anyString())).thenReturn(0);
        when(documentRepository.countPreservedByType(any())).thenReturn(5L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(5, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(5, result.skippedCount());
    }

    @Test
    void testExecuteApplyHandlesS3Failures() {
        var doc1 = createDocumentEntity(DocumentType.DOCUMENTS, "path1");
        var doc2 = createDocumentEntity(DocumentType.PAYSLIP, "path2");
        var removableDocuments = Arrays.asList(doc1, doc2);

        when(documentRepository.findRemovableByRetention(any())).thenReturn(removableDocuments);
        doThrow(new RuntimeException("S3 not available"))
                .when(bucketStorageProvider).deleteFile(DocumentType.DOCUMENTS, "path1");
        when(documentRepository.markAsDeletedByRetention(any(), any(), anyString())).thenReturn(2);
        when(documentRepository.countPreservedByType(any())).thenReturn(2L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("PARTIAL", result.status());
        assertEquals(4, result.scannedCount());
        assertEquals(1, result.affectedCount());
        assertEquals(1, result.errorCount());
        assertTrue(result.notes().contains("S3 deletion errors"));
    }

    @Test
    void testExecuteApplyWithAllS3Failures() {
        var doc = createDocumentEntity(DocumentType.DOCUMENTS, "path");
        when(documentRepository.findRemovableByRetention(any())).thenReturn(Arrays.asList(doc));
        doThrow(new RuntimeException("S3 error"))
                .when(bucketStorageProvider).deleteFile(any(), anyString());
        when(documentRepository.markAsDeletedByRetention(any(), any(), anyString())).thenReturn(1);
        when(documentRepository.countPreservedByType(any())).thenReturn(2L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("PARTIAL", result.status());
        assertEquals(3, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(1, result.errorCount());
    }

    @Test
    void testExecuteApplyHandlesDBException() {
        when(documentRepository.findRemovableByRetention(any()))
                .thenThrow(new RuntimeException("Database error"));

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertTrue(result.notes().contains("Database error"));
    }

    @Test
    void testExecuteDryRunHandlesException() {
        when(documentRepository.countRemovableByRetention(any()))
                .thenThrow(new RuntimeException("Query failed"));

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertTrue(result.notes().contains("Query failed"));
    }

    @Test
    void testExecuteApplyRecordsPolicyCode() {
        var doc = createDocumentEntity(DocumentType.DOCUMENTS, "path");
        when(documentRepository.findRemovableByRetention(any())).thenReturn(Arrays.asList(doc));
        when(documentRepository.markAsDeletedByRetention(any(), any(), anyString())).thenReturn(1);
        when(documentRepository.countPreservedByType(any())).thenReturn(0L);

        var policy = createPolicy();
        processor.execute(policy, "APPLY");

        verify(documentRepository).markAsDeletedByRetention(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyString()
        );
    }

    private DocumentEntity createDocumentEntity(DocumentType type, String path) {
        return DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .fileName("test.pdf")
                .contentType("application/pdf")
                .storagePath(path)
                .type(type)
                .uploadedAt(LocalDateTime.now().minusDays(40))
                .build();
    }

    private RetentionPolicy createPolicy() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOC_RETENTION",
                "Test document retention policy",
                "DOCUMENT",
                30,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.now(),
                null
        );
    }
}
