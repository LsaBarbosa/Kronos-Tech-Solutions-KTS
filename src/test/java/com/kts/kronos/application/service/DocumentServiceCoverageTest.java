package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FileScanningProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.AuditRequestContextService.AuditRequestContext;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceCoverageTest {

    @InjectMocks
    private DocumentService service;

    @Mock private DocumentProvider documentProvider;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private BucketStorageProvider bucketStorageProvider;
    @Mock private DomainAuthorizationService domainAuthorizationService;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private FileScanningProvider fileScanningProvider;
    @Mock private AuditService auditService;
    @Mock private AuditRequestContextService auditRequestContextService;
    @Mock private KronosMetrics kronosMetrics;     // non-null → covers metrics() TRUE branch
    @Mock private KronosTracing kronosTracing;     // non-null → covers tracing() TRUE branch

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxUploadBytes", 5 * 1024 * 1024L);

        // Make tracing mock actually execute Runnable actions
        lenient().doAnswer(inv -> { ((Runnable) inv.getArgument(1)).run(); return null; })
                 .when(kronosTracing).observe(anyString(), any(Runnable.class),
                         anyString(), anyString(), anyString(), anyString());

        // Make tracing mock execute Supplier<T> actions and return result
        lenient().when(kronosTracing.observe(anyString(), any(Supplier.class),
                         anyString(), anyString(), anyString(), anyString()))
                 .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        when(auditRequestContextService.extractContext()).thenReturn(AuditRequestContext.unknown());
    }

    // ── metrics() TRUE branch (L582) + tracing() TRUE branch (L586) ──────────

    @Test
    @DisplayName("downloadDocument: kronosMetrics e kronosTracing não-nulos → cobre branches TRUE de metrics() e tracing()")
    @SuppressWarnings("unchecked")
    void downloadDocument_nonNullMetricsAndTracing_coversTrueBranches() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        Document doc = buildDocument(docId, empId, "bucket/path/file.pdf");
        byte[] fileData = "PDF content".getBytes();

        when(domainAuthorizationService.authorizeDocumentAccess(docId, empId)).thenReturn(doc);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(bucketStorageProvider.downloadFile(any(), anyString())).thenReturn(fileData);

        var result = service.downloadDocument(empId, docId);

        assertNotNull(result);
        verify(kronosMetrics).documentDownloadSuccess(anyString());
        verify(kronosTracing).observe(anyString(), any(Supplier.class),
                anyString(), anyString(), anyString(), anyString());
    }

    // ── sanitizeFileName — originalFileName null → L392 null branch ───────────

    @Test
    @DisplayName("uploadDocument: MockMultipartFile com originalFilename nulo → BadRequestException (null branch L392)")
    void uploadDocument_nullOriginalFilename_throwsBadRequest() {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        MockMultipartFile file = new MockMultipartFile("file", null, "application/pdf",
                "%PDF-1.4 test".getBytes());

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, file));
    }

    // ── validateAndPrepareUpload — file == null → L354 null branch ────────────

    @Test
    @DisplayName("uploadDocument: file==null → BadRequestException (null branch L354)")
    void uploadDocument_nullFile_throwsBadRequest() {
        UUID empId = UUID.randomUUID();

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, null));
    }

    // ── extractExtension — filename ends with dot → L422 branch ──────────────

    @Test
    @DisplayName("uploadGeneratedDocument: fileName terminando com ponto → BadRequestException (L422 branch)")
    void uploadGeneratedDocument_fileEndsWithDot_throwsBadRequest() {
        UUID empId = UUID.randomUUID();
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId)));

        assertThrows(BadRequestException.class,
                () -> service.uploadGeneratedDocument(DocumentType.PAYSLIP, empId, null,
                        "%PDF-1.4 test".getBytes(), "report."));
    }

    // ── isDocx — valid docx (both markers found) → return true (L466-467) ────

    @Test
    @DisplayName("isDocx: zip com marcadores DOCX → isDocx retorna true (L466-467)")
    void uploadDocument_validDocx_isDocxReturnsTrue_throwsMimeMismatch() throws Exception {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        byte[] docxBytes = buildDocxBytes();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", docxBytes);

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, file));
    }

    // ── isDocx — valid zip but no DOCX markers → return false (L471) ─────────

    @Test
    @DisplayName("isDocx: zip sem marcadores DOCX → return false em L471")
    void uploadDocument_zipWithoutDocxMarkers_isDocxReturnsFalse() throws Exception {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        byte[] plainZip = buildPlainZipBytes();
        MockMultipartFile file = new MockMultipartFile("file", "file.pdf", "application/pdf", plainZip);

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, file));
    }

    // ── isDocx — PK magic + corrupt data → IOException catch (L472-473) ──────

    @Test
    @DisplayName("isDocx: bytes PK+lixo → EOFException capturada em L472-473 → return false")
    void uploadDocument_corruptZipAfterPkHeader_ioExceptionCaught() {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        // PK magic header followed by too few bytes — causes EOFException in ZipInputStream
        byte[] corrupt = new byte[]{0x50, 0x4B, 0x03, 0x04};
        MockMultipartFile file = new MockMultipartFile("file", "file.pdf", "application/pdf", corrupt);

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, file));
    }

    // ── listDocuments — Role.CTO → isManagerView TRUE (L146 CTO branch) ──────

    @Test
    @DisplayName("listDocuments: papel CTO → isManagerView=true via CTO (L146 CTO branch)")
    void listDocuments_ctoRole_isManagerView() {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        List<Document> expected = List.of(buildDocument(UUID.randomUUID(), empId, "path"));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(documentProvider.findByEmployeeAndType(empId, DocumentType.PAYSLIP, true))
                .thenReturn(expected);

        var result = service.listDocuments(DocumentType.PAYSLIP, empId, null);

        assertEquals(expected, result);
        verify(documentProvider).findByEmployeeAndType(empId, DocumentType.PAYSLIP, true);
    }

    // ── deleteDocument — Role.CTO → isManager TRUE (L180 CTO branch) ─────────

    @Test
    @DisplayName("deleteDocument: papel CTO → isManager=true via CTO (L180 CTO branch)")
    void deleteDocument_ctoRole_isManagerSoftDelete() {
        UUID empId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        Document doc = new Document(docId, empId, DocumentType.PAYSLIP,
                "file.pdf", "application/pdf", "bucket/path.pdf",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(domainAuthorizationService.authorizeDocumentAccess(docId, empId)).thenReturn(doc);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        assertDoesNotThrow(() -> service.deleteDocument(empId, docId));

        // manager soft-deletes (only manager flag set) → documentProvider.save, not delete
        verify(documentProvider).save(argThat(d -> d.deletedByManager() && !d.deletedByEmployee()));
    }

    // ── registerDocumentAudit — additionalDetails não-branco (L547) ──────────

    @Test
    @DisplayName("uploadDocumentForTimeRecord: timeRecordId não-nulo → additionalDetails não-branco (L547)")
    void uploadDocumentForTimeRecord_nonNullTimeRecordId_coversL547() throws Exception {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        byte[] pdfBytes = "%PDF-1.4 test".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "recibo.pdf",
                "application/pdf", pdfBytes);

        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(any(), anyString(), any(), anyString()))
                .thenReturn("bucket/path/recibo.pdf");
        doNothing().when(fileScanningProvider).scanOrThrow(anyString(), anyString(), any());

        assertDoesNotThrow(() -> service.uploadDocumentForTimeRecord(
                DocumentType.TIME_OFF, empId, 42L, file));

        // additionalDetails = "timeRecordId=42" (non-blank) → L547 TRUE branch taken
        verify(auditRequestContextService, atLeastOnce()).extractContext();
        verify(documentProvider).save(any());
    }

    // ── validateAndPrepareUpload — file.isEmpty() TRUE → BadRequestException ───

    @Test
    @DisplayName("uploadDocument: file não-nulo mas vazio (0 bytes) → file.isEmpty()=TRUE → BadRequestException (L354 OR-right branch)")
    void uploadDocument_emptyFile_throwsBadRequest() {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        // Non-null but 0-byte content → isEmpty() returns true → L354 OR-right TRUE branch
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf",
                new byte[0]);

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, emptyFile));
    }

    // ── validateAndPrepareUpload — allowedExtensionsForMime.contains FALSE → throw ─

    @Test
    @DisplayName("uploadDocument: conteúdo PDF com extensão .jpg → MIME mismatch (L379 !contains TRUE)")
    void uploadDocument_pdfContentWithJpgExtension_throwsMimeMismatch() {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        // PDF magic bytes with .jpg filename → MIME="application/pdf", extension="jpg"
        // allowedExtensionsForMime = {"pdf"}, contains("jpg") = FALSE → throw
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", pdfBytes);

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, file));
    }

    // ── uploadGeneratedDocument — employee not found → lambda ResourceNotFound ─

    @Test
    @DisplayName("uploadGeneratedDocument: employeeProvider.findById retorna empty → lambda L304 → ResourceNotFoundException")
    void uploadGeneratedDocument_employeeNotFound_lambdaThrowsResourceNotFound() {
        UUID empId = UUID.randomUUID();
        when(employeeProvider.findById(empId)).thenReturn(java.util.Optional.empty());

        assertThrows(com.kts.kronos.application.exceptions.ResourceNotFoundException.class,
                () -> service.uploadGeneratedDocument(DocumentType.PAYSLIP, empId, null,
                        "%PDF-1.4 test".getBytes(), "report.pdf"));
    }

    // ── getAuditSeverityForDocumentType — type == null → return "MEDIUM" (L523 TRUE) ─

    @Test
    @DisplayName("downloadDocument: doc.type()=null → getAuditSeverityForDocumentType(null) → L523 TRUE branch → 'MEDIUM'")
    @SuppressWarnings("unchecked")
    void downloadDocument_nullDocumentType_coversGetAuditSeverityNullBranch() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        // Document with null type → getAuditSeverityForDocumentType(null) → L523 if(type==null) TRUE
        Document docNullType = new Document(docId, empId, null, "file.pdf",
                "application/pdf", "bucket/path.pdf", LocalDateTime.now(), null, false, false);
        byte[] fileData = "PDF content".getBytes();

        when(domainAuthorizationService.authorizeDocumentAccess(docId, empId)).thenReturn(docNullType);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(bucketStorageProvider.downloadFile(any(), anyString())).thenReturn(fileData);

        var result = service.downloadDocument(empId, docId);

        assertNotNull(result);
    }


    // ── getAuditSeverityForDocumentType — POINT_RECORD_RECEIPT → L525 switch case ─

    @Test
    @DisplayName("downloadDocument: doc.type()=POINT_RECORD_RECEIPT → switch case L525 covered")
    @SuppressWarnings("unchecked")
    void downloadDocument_pointRecordReceiptType_coversL525SwitchBranch() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        Document doc = new Document(docId, empId, DocumentType.POINT_RECORD_RECEIPT, "receipt.pdf",
                "application/pdf", "bucket/receipt.pdf", LocalDateTime.now(), null, false, false);
        byte[] fileData = "PDF content".getBytes();

        when(domainAuthorizationService.authorizeDocumentAccess(docId, empId)).thenReturn(doc);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(bucketStorageProvider.downloadFile(any(), anyString())).thenReturn(fileData);

        var result = service.downloadDocument(empId, docId);

        assertNotNull(result);
        assertEquals(DocumentType.POINT_RECORD_RECEIPT, result.type());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Employee buildEmployee(UUID empId) {
        return new Employee(empId, "Test", "12345678901", "12345678901", "Dev",
                "emp@kts.com", 3000.0, null, true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                UUID.randomUUID(), null, false, null,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, DayOfWeek.MONDAY, null, null);
    }

    private static Document buildDocument(UUID docId, UUID empId, String storagePath) {
        return new Document(docId, empId, DocumentType.PAYSLIP, "file.pdf",
                "application/pdf", storagePath, LocalDateTime.now(), null, false, false);
    }

    private static byte[] buildDocxBytes() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zos.write("<Types/>".getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            zos.write("<w:document/>".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    @DisplayName("isDocx: compressão inválida no ZIP → IOException capturada (L472-473)")
    void uploadDocument_invalidZipCompression_isDocxIOExceptionCaught() {
        UUID empId = UUID.randomUUID();
        Employee employee = buildEmployee(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);

        // ZIP LOC header with compression method = 99 (invalid) → ZipException (IOException)
        // This triggers the catch (IOException e) { return false; } inside isDocx()
        byte[] invalidZip = new byte[] {
                0x50, 0x4B, 0x03, 0x04,  // PK signature
                0x14, 0x00,               // version needed
                0x00, 0x00,               // general purpose bit flag
                0x63, 0x00,               // compression method = 99 (INVALID)
                0x00, 0x00,               // last mod time
                0x00, 0x00,               // last mod date
                0x00, 0x00, 0x00, 0x00,   // CRC-32
                0x00, 0x00, 0x00, 0x00,   // compressed size
                0x00, 0x00, 0x00, 0x00,   // uncompressed size
                0x00, 0x00,               // file name length = 0
                0x00, 0x00                // extra field length = 0
        };
        MockMultipartFile file = new MockMultipartFile("file", "file.pdf",
                "application/pdf", invalidZip);

        assertThrows(BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, empId, file));
    }

    private static byte[] buildPlainZipBytes() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("README.txt"));
            zos.write("not a docx".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
