package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.FileScanningProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.application.service.AuditRequestContextService.AuditRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceCoreTest {

    private static final HexFormat HEX = HexFormat.of();

    @InjectMocks
    private DocumentService service;

    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private BucketStorageProvider bucketStorageProvider;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;
    @Mock
    private FileScanningProvider fileScanningProvider;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditRequestContextService auditRequestContextService;

    @BeforeEach
     void configureUploadLimit() {
        ReflectionTestUtils.setField(service, "maxUploadBytes", 5 * 1024 * 1024L);
    }

    private AuditRequestContext createAuditContext() {
        return new AuditRequestContext("192.168.1.1", "Mozilla/5.0", "DIRECT", true);
    }

    @Test
    @DisplayName("listDocuments: manager usa visão gerencial sem data")
    void shouldListDocumentsForManagerViewWithoutDate() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId);
        List<Document> expected = List.of(buildDocument(employeeId, false, false));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.PAYSLIP, true)).thenReturn(expected);

        List<Document> result = service.listDocuments(DocumentType.PAYSLIP, employeeId, null);

        assertEquals(expected, result);
        verify(documentProvider).findByEmployeeAndType(employeeId, DocumentType.PAYSLIP, true);
    }

    @Test
    @DisplayName("listDocuments: partner usa visão do próprio colaborador com data")
    void shouldListDocumentsForPartnerViewWithDate() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId);
        LocalDate date = LocalDate.of(2026, 4, 17);
        List<Document> expected = List.of(buildDocument(employeeId, false, false));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(documentProvider.findByEmployeeAndDateAndType(employeeId, date, DocumentType.PAYSLIP, false))
                .thenReturn(expected);

        List<Document> result = service.listDocuments(DocumentType.PAYSLIP, null, date);

        assertEquals(expected, result);
        verify(documentProvider).findByEmployeeAndDateAndType(employeeId, date, DocumentType.PAYSLIP, false);
    }

    @Test
    @DisplayName("uploadGeneratedDocument: salva PDF gerado com nome sanitizado")
    void shouldUploadGeneratedDocumentWithSanitizedFileName() {
        UUID employeeId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7 test".getBytes();
        Employee employee = buildEmployee(employeeId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(eq(DocumentType.PAYSLIP), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("bucket/path/file.pdf");

        service.uploadGeneratedDocument(
                DocumentType.PAYSLIP,
                employeeId,
                77L,
                pdf,
                "../Comprovante Final.PDF"
        );

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(captor.capture());

        Document saved = captor.getValue();
        assertEquals("Comprovante_Final.pdf", saved.fileName());
        assertEquals(77L, saved.timeRecordId());
        assertEquals("application/pdf", saved.contentType());
        assertEquals(sha256(pdf), saved.checksumSha256());
    }

    @Test
    @DisplayName("uploadDocumentForTimeRecord: persiste documento vinculado ao timeRecord")
    void shouldUploadDocumentForTimeRecord() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "recibo.pdf",
                "application/pdf",
                "%PDF-1.7 valid".getBytes()
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(eq(DocumentType.TIME_OFF), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("bucket/path/recibo.pdf");

        service.uploadDocumentForTimeRecord(DocumentType.TIME_OFF, null, 55L, file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(captor.capture());

        Document saved = captor.getValue();
        assertEquals(employeeId, saved.employeeId());
        assertEquals(55L, saved.timeRecordId());
        assertEquals(DocumentType.TIME_OFF, saved.type());
        assertEquals(sha256("%PDF-1.7 valid".getBytes()), saved.checksumSha256());
    }

    @Test
    @DisplayName("deleteDocument: remove fisicamente quando ambos flags ficam true")
    void shouldDeleteDocumentPhysicallyWhenBothFlagsBecomeTrue() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployeeWithCompany(employeeId, companyId);

        Document document = new Document(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "bucket/path/holerite.pdf",
                LocalDateTime.now(),
                null,
                false,
                true
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(auditRequestContextService.extractContext()).thenReturn(createAuditContext());

        service.deleteDocument(null, documentId);

        verify(bucketStorageProvider).deleteFile(DocumentType.PAYSLIP, "bucket/path/holerite.pdf");
        verify(documentProvider).delete(employeeId, documentId);
        verify(documentProvider, never()).save(any());
    }

    @Test
    @DisplayName("uploadDocument: registra auditoria DOCUMENT_UPLOADED com severity apropriada (SPEC-003)")
    void shouldRegisterUploadAuditWithAppropriateAction() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployeeWithCompany(employeeId, companyId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "%PDF-1.7 valid".getBytes()
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(eq(DocumentType.PAYSLIP), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("bucket/path/documento.pdf");
        when(auditRequestContextService.extractContext()).thenReturn(createAuditContext());

        service.uploadDocument(DocumentType.PAYSLIP, null, file);

        verify(auditService).register(
                eq(AuditAction.DOCUMENT_UPLOADED),
                eq(employeeId),
                eq(companyId),
                eq("DOCUMENT"),
                anyString(),
                eq("MEDIUM"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("downloadDocument: registra auditoria DOCUMENT_DOWNLOADED com companyId e severity HIGH para BIOMETRIC_CONSENT_TERM (SPEC-003)")
    void shouldRegisterDownloadAuditWithCompanyIdAndAppropriateSeverity() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployeeWithCompany(employeeId, companyId);

        byte[] fileData = "%PDF-1.7 test".getBytes();
        Document document = new Document(
                documentId,
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "termo.pdf",
                "application/pdf",
                "bucket/path/termo.pdf",
                LocalDateTime.now(),
                null,
                false,
                false
        );

        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.downloadFile(DocumentType.BIOMETRIC_CONSENT_TERM, "bucket/path/termo.pdf"))
                .thenReturn(fileData);
        when(auditRequestContextService.extractContext()).thenReturn(createAuditContext());

        service.downloadDocument(null, documentId);

        verify(auditService).register(
                eq(AuditAction.DOCUMENT_DOWNLOADED),
                eq(employeeId),
                eq(companyId),
                eq("DOCUMENT"),
                eq(documentId.toString()),
                eq("HIGH"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("deleteDocument: registra DOCUMENT_SOFT_DELETED_BY_EMPLOYEE quando colaborador deleta (SPEC-003)")
    void shouldRegisterSoftDeleteByEmployeeAudit() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployeeWithCompany(employeeId, companyId);

        Document document = new Document(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "bucket/path/holerite.pdf",
                LocalDateTime.now(),
                null,
                false,
                false
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(auditRequestContextService.extractContext()).thenReturn(createAuditContext());

        service.deleteDocument(null, documentId);

        verify(auditService).register(
                eq(AuditAction.DOCUMENT_SOFT_DELETED_BY_EMPLOYEE),
                eq(employeeId),
                eq(companyId),
                eq("DOCUMENT"),
                eq(documentId.toString()),
                eq("MEDIUM"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("deleteDocument: registra DOCUMENT_SOFT_DELETED_BY_MANAGER quando gestor deleta (SPEC-003)")
    void shouldRegisterSoftDeleteByManagerAudit() {
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployeeWithCompany(employeeId, companyId);

        Document document = new Document(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "bucket/path/holerite.pdf",
                LocalDateTime.now(),
                null,
                false,
                false
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(auditRequestContextService.extractContext()).thenReturn(createAuditContext());

        service.deleteDocument(null, documentId);

        verify(auditService).register(
                eq(AuditAction.DOCUMENT_SOFT_DELETED_BY_MANAGER),
                eq(employeeId),
                eq(companyId),
                eq("DOCUMENT"),
                eq(documentId.toString()),
                eq("MEDIUM"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("uploadGeneratedDocument: registra auditoria DOCUMENT_GENERATED (SPEC-003)")
    void shouldRegisterGeneratedDocumentAudit() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7 test".getBytes();
        Employee employee = buildEmployeeWithCompany(employeeId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(eq(DocumentType.PAYSLIP), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("bucket/path/file.pdf");
        when(auditRequestContextService.extractContext()).thenReturn(createAuditContext());

        service.uploadGeneratedDocument(
                DocumentType.PAYSLIP,
                employeeId,
                77L,
                pdf,
                "Comprovante.pdf"
        );

        verify(auditService).register(
                eq(AuditAction.DOCUMENT_GENERATED),
                eq(employeeId),
                eq(companyId),
                eq("DOCUMENT"),
                anyString(),
                eq("MEDIUM"),
                anyString(),
                anyString(),
                anyString()
        );
    }

    private Employee buildEmployee(UUID employeeId) {
        return buildEmployeeWithCompany(employeeId, UUID.randomUUID());
    }

    private Employee buildEmployeeWithCompany(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Employee",
                "12345678901",
                "12345678901",
                "Developer",
                "employee@kts.com",
                3000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                companyId,
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                DayOfWeek.MONDAY,
                null,
                null
        );
    }

    private Document buildDocument(UUID employeeId, boolean deletedByEmployee, boolean deletedByManager) {
        return new Document(
                UUID.randomUUID(),
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "bucket/path/holerite.pdf",
                LocalDateTime.now(),
                null,
                deletedByEmployee,
                deletedByManager
        );
    }

    private String sha256(byte[] payload) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
