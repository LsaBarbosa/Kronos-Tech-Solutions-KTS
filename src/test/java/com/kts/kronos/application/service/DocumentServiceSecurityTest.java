package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FileScanningProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.kts.kronos.constants.Messages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceSecurityTest {

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
    private EmployeeProvider employeeProvider;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditRequestContextService auditRequestContextService;

    private UUID loggedEmployeeId;
    private UUID managerEmployeeId;
    private UUID otherTenantEmployeeId;
    private UUID companyAId;
    private UUID companyBId;

    @BeforeEach
    void setUp() {
        loggedEmployeeId = UUID.randomUUID();
        managerEmployeeId = UUID.randomUUID();
        otherTenantEmployeeId = UUID.randomUUID();
        companyAId = UUID.randomUUID();
        companyBId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "maxUploadBytes", 5 * 1024 * 1024L);

        var mockContext = new AuditRequestContextService.AuditRequestContext("127.0.0.1", "Test-Agent/1.0", "LOCAL", false);
        lenient().when(auditRequestContextService.extractContext()).thenReturn(mockContext);

        // Mock genérico para authorizeEmployeeAccess (usado para obter companyId em auditoria)
        lenient().when(domainAuthorizationService.authorizeEmployeeAccess(any())).thenAnswer(invocation -> {
            UUID empId = invocation.getArgument(0);
            return buildEmployee(empId != null ? empId : loggedEmployeeId, companyAId);
        });
    }

    @Test
    @DisplayName("download: permite acesso ao próprio colaborador autorizado")
    void shouldAllowDownloadForAuthorizedEmployee() throws Exception {
        UUID documentId = UUID.randomUUID();
        Document document = buildDocument(documentId, loggedEmployeeId, "docs/file.pdf");
        byte[] fileBytes = "payload".getBytes(StandardCharsets.UTF_8);

        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(bucketStorageProvider.downloadFile(document.type(), document.storagePath())).thenReturn(fileBytes);

        DocumentWithData response = service.downloadDocument(null, documentId);

        assertEquals(documentId, response.documentId());
        assertArrayEquals(fileBytes, response.data());
        verify(domainAuthorizationService).authorizeDocumentAccess(documentId, null);
    }

    @Test
    @DisplayName("download: bloqueia manager em documento de outra empresa")
    void shouldBlockManagerDownloadFromOtherTenant() {
        UUID documentId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.downloadDocument(otherTenantEmployeeId, documentId));
        verify(bucketStorageProvider, never()).downloadFile(any(DocumentType.class), anyString());
    }

    @Test
    @DisplayName("download: traduz storage ausente para documento nao encontrado")
    void shouldTranslateMissingStorageObjectOnDownload() {
        UUID documentId = UUID.randomUUID();
        Document document = buildDocument(documentId, loggedEmployeeId, "safe/object.pdf");

        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(bucketStorageProvider.downloadFile(document.type(), document.storagePath()))
                .thenThrow(new ResourceNotFoundException("bucket object missing"));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.downloadDocument(null, documentId)
        );

        assertEquals(DOCUMENT_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("delete: permite dono apagar documento conforme regra atual")
    void shouldAllowOwnerDeleteDocument() {
        UUID documentId = UUID.randomUUID();
        Document document = buildDocument(documentId, loggedEmployeeId, "safe/object.pdf");

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);

        service.deleteDocument(null, documentId);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(captor.capture());
        assertTrue(captor.getValue().deletedByEmployee());
        assertFalse(captor.getValue().deletedByManager());
        verify(bucketStorageProvider, never()).deleteFile(any(DocumentType.class), anyString());
    }

    @Test
    @DisplayName("delete: bloqueia TIME_OFF quando solicitante nao e dono")
    void shouldBlockTimeOffDeleteByNonOwner() {
        UUID documentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Document document = new Document(
                documentId,
                ownerId,
                DocumentType.TIME_OFF,
                "atestado.pdf",
                "application/pdf",
                "safe/time-off.pdf",
                LocalDateTime.now(),
                null,
                false,
                false
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.deleteDocument(null, documentId)
        );

        assertEquals(ONLY_OWNER_DELETE_TIME_OFF_DOCS, exception.getMessage());
        verify(documentProvider, never()).save(any());
    }

    @Test
    @DisplayName("delete: manager marca documento como removido pelo gestor")
    void shouldMarkDocumentDeletedByManager() {
        UUID documentId = UUID.randomUUID();
        Document document = buildDocument(documentId, loggedEmployeeId, "safe/object.pdf");

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, loggedEmployeeId)).thenReturn(document);

        service.deleteDocument(loggedEmployeeId, documentId);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(captor.capture());
        assertTrue(captor.getValue().deletedByManager());
        assertFalse(captor.getValue().deletedByEmployee());
    }

    @Test
    @DisplayName("delete: partner nao pode apagar documento de outro colaborador")
    void shouldBlockPartnerDeletingAnotherEmployeesDocument() {
        UUID documentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Document document = buildDocument(documentId, ownerId, "safe/object.pdf");

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, ownerId)).thenReturn(document);

        assertThrows(ForbiddenException.class, () -> service.deleteDocument(ownerId, documentId));
        verify(documentProvider, never()).save(any());
    }

    @Test
    @DisplayName("delete: bloqueia exclusão cross-tenant")
    void shouldBlockDeleteCrossTenant() {
        UUID documentId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeDocumentAccess(documentId, otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.deleteDocument(otherTenantEmployeeId, documentId));
        verify(documentProvider, never()).save(any());
    }

    @Test
    @DisplayName("upload: neutraliza tentativa de path traversal no nome original")
    void shouldSanitizePathTraversalInUploadedFileName() throws Exception {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        byte[] pdfBytes = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../../etc/passwd.pdf",
                "application/pdf",
                pdfBytes
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(any(DocumentType.class), anyString(), any(byte[].class), anyString())).thenReturn("safe/storage/path");

        service.uploadDocument(DocumentType.PAYSLIP, null, file);

        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(bucketStorageProvider).uploadFile(any(DocumentType.class), objectNameCaptor.capture(), any(byte[].class), anyString());
        String objectName = objectNameCaptor.getValue();

        assertFalse(objectName.contains(".."));
        assertTrue(objectName.contains(employee.employeeId().toString() + "/"));

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(docCaptor.capture());
        assertEquals("passwd.pdf", docCaptor.getValue().fileName());
    }

    @Test
    @DisplayName("upload: rejeita MIME/extensão inválidos")
    void shouldRejectInvalidMimeAndExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.exe",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
        verify(bucketStorageProvider, never()).uploadFile(any(DocumentType.class), anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("upload: aceita assinaturas reais de tipos permitidos")
    void shouldAcceptAllowedRealMimeTypes() throws Exception {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        List<MockMultipartFile> files = List.of(
                new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}),
                new MockMultipartFile("file", "imagem.png", "image/png", new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
                }),
                new MockMultipartFile("file", "documento.pdf", "application/pdf", new byte[]{(byte) 0x25, 0x50, 0x44, 0x46})
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(any(DocumentType.class), anyString(), any(byte[].class), anyString())).thenReturn("safe/storage/path");

        for (MockMultipartFile file : files) {
            service.uploadDocument(DocumentType.PAYSLIP, null, file);
        }

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider, times(files.size())).save(captor.capture());

        List<String> contentTypes = captor.getAllValues().stream()
                .map(Document::contentType)
                .toList();
        assertEquals(List.of(
                "image/jpeg",
                "image/png",
                "application/pdf"
        ), contentTypes);
    }

    @Test
    @DisplayName("upload: rejeita arquivo nulo ou vazio")
    void shouldRejectNullFile() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, null)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita arquivo acima do limite configurado")
    void shouldRejectFileLargerThanConfiguredLimit() {
        ReflectionTestUtils.setField(service, "maxUploadBytes", 3L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "grande.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(FILE_TOO_LARGE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita bytes vazios mesmo quando multipart informa nao vazio")
    void shouldRejectEmptyBytesFromMultipart() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(0L);
        when(file.getOriginalFilename()).thenReturn("vazio.pdf");
        when(file.getBytes()).thenReturn(new byte[0]);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita assinatura real incompativel")
    void shouldRejectUnknownRealMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.pdf",
                "application/pdf",
                "not-a-pdf".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita extensao incompatível com MIME real")
    void shouldRejectExtensionThatDoesNotMatchDetectedMime() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.doc",
                "application/msword",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita docx sem estrutura esperada")
    void shouldRejectZipWithoutDocxStructure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes(false)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: traduz IOException de leitura")
    void shouldTranslateUploadReadIOException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getOriginalFilename()).thenReturn("arquivo.pdf");
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(NOT_ABLE_TO_READ_FILE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: propaga falha interna de storage sem mascarar")
    void shouldPropagateRuntimeExceptionOnRegularUploadStorageFailure() {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );
        RuntimeException storageFailure = new RuntimeException("storage down");

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(any(DocumentType.class), anyString(), any(byte[].class), eq("application/pdf")))
                .thenThrow(storageFailure);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(storageFailure, exception);
    }

    @Test
    @DisplayName("upload: invoca fileScanningProvider.scanOrThrow durante upload")
    void uploadDocument_shouldInvokeFileScanningProvider() throws Exception {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        byte[] pdfBytes = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                pdfBytes
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(bucketStorageProvider.uploadFile(any(DocumentType.class), anyString(), any(byte[].class), anyString())).thenReturn("safe/storage/path");

        service.uploadDocument(DocumentType.PAYSLIP, null, file);

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileScanningProvider).scanOrThrow(anyString(), eq("application/pdf"), bytesCaptor.capture());
        assertArrayEquals(pdfBytes, bytesCaptor.getValue());
    }

    @Test
    @DisplayName("upload: rejeita quando fileScanningProvider rejeita arquivo")
    void uploadDocument_shouldRejectWhenFileScanningProviderRejects() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicioso.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );

        doThrow(new BadRequestException(MALICIOUS_FILE_DETECTED))
                .when(fileScanningProvider).scanOrThrow(anyString(), eq("application/pdf"), any(byte[].class));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(MALICIOUS_FILE_DETECTED, exception.getMessage());
    }

    @Test
    @DisplayName("upload: bloqueia quando fileScanningProvider falha com erro interno")
    void uploadDocument_shouldRejectWhenFileScanningProviderFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );

        doThrow(new RuntimeException("scanner unavailable"))
                .when(fileScanningProvider).scanOrThrow(anyString(), eq("application/pdf"), any(byte[].class));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals("scanner unavailable", exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita nome original nulo")
    void shouldRejectNullOriginalFileName() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getOriginalFilename()).thenReturn(null);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita arquivo sem extensão")
    void shouldRejectFileNameWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita docx corrompido com assinatura zip")
    void shouldRejectCorruptedDocxZip() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{0x50, 0x4B, 0x03, 0x04}
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita docx com header zip truncado")
    void shouldRejectMalformedDocxZipThatRaisesIoException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{
                        0x50, 0x4B, 0x03, 0x04,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 10, 0, 0, 0
                }
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("upload: rejeita arquivo menor que qualquer assinatura conhecida")
    void shouldRejectTooShortFileSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "arquivo.pdf",
                "application/pdf",
                new byte[]{0x25}
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadDocument(DocumentType.PAYSLIP, null, file)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("download: não vaza path interno em erro de storage")
    void shouldNotLeakInternalStoragePathOnDownloadError() {
        UUID documentId = UUID.randomUUID();
        Document document = buildDocument(documentId, loggedEmployeeId, "safe/object.pdf");

        when(domainAuthorizationService.authorizeDocumentAccess(documentId, null)).thenReturn(document);
        when(bucketStorageProvider.downloadFile(any(DocumentType.class), anyString()))
                .thenThrow(new RuntimeException("falha em /mnt/data/documents/secret.pdf"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.downloadDocument(null, documentId)
        );

        assertEquals(ERROR_GET_FILE, exception.getMessage());
        assertFalse(exception.getMessage().contains("/mnt/data/documents"));
    }

    @Test
    @DisplayName("list: usa role atual do contexto e não claim histórica")
    void shouldUseCurrentRoleInListDocuments() {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(employee);
        when(documentProvider.findByEmployeeAndType(loggedEmployeeId, DocumentType.PAYSLIP, false))
                .thenReturn(List.of());

        service.listDocuments(DocumentType.PAYSLIP, null, null);

        verify(jwtAuthenticatedUser, atLeastOnce()).getCurrentRole();
    }

    @Test
    @DisplayName("list: usa visão de gestor quando role atual é MANAGER")
    void shouldUseManagerViewWhenListingDocumentsWithDate() {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        LocalDate date = LocalDate.of(2026, 4, 22);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(loggedEmployeeId)).thenReturn(employee);
        when(documentProvider.findByEmployeeAndDateAndType(loggedEmployeeId, date, DocumentType.PAYSLIP, true))
                .thenReturn(List.of());

        service.listDocuments(DocumentType.PAYSLIP, loggedEmployeeId, date);

        verify(documentProvider).findByEmployeeAndDateAndType(loggedEmployeeId, date, DocumentType.PAYSLIP, true);
    }

    @Test
    @DisplayName("uploadGeneratedDocument: rejeita nome invalido")
    void shouldRejectGeneratedDocumentWithInvalidFileName() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadGeneratedDocument(DocumentType.PAYSLIP, loggedEmployeeId, null, new byte[]{1}, "...")
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("uploadGeneratedDocument: rejeita nome nulo")
    void shouldRejectGeneratedDocumentWithNullFileName() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.uploadGeneratedDocument(DocumentType.PAYSLIP, loggedEmployeeId, null, new byte[]{1}, null)
        );

        assertEquals(INVALID_DOCUMENT_TYPE, exception.getMessage());
    }

    @Test
    @DisplayName("uploadGeneratedDocument: limita nome base a 100 caracteres")
    void shouldTruncateLongGeneratedDocumentFileName() {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        String longBaseName = "a".repeat(120);

        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(bucketStorageProvider.uploadFile(any(DocumentType.class), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("safe/storage/path");

        service.uploadGeneratedDocument(
                DocumentType.PAYSLIP,
                loggedEmployeeId,
                null,
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8),
                longBaseName + ".pdf"
        );

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(captor.capture());
        assertEquals("a".repeat(100) + ".pdf", captor.getValue().fileName());
    }

    @Test
    @DisplayName("uploadGeneratedDocument: preserva excecoes de regra")
    void shouldRethrowBusinessExceptionOnGeneratedDocumentUpload() {
        when(employeeProvider.findById(loggedEmployeeId))
                .thenThrow(new ForbiddenException("forbidden"));

        assertThrows(
                ForbiddenException.class,
                () -> service.uploadGeneratedDocument(DocumentType.PAYSLIP, loggedEmployeeId, null, new byte[]{1}, "ok.pdf")
        );
    }

    @Test
    @DisplayName("uploadGeneratedDocument: propaga falha interna de storage")
    void shouldPropagateRuntimeExceptionOnGeneratedDocumentStorageFailure() {
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        RuntimeException storageFailure = new RuntimeException("storage down");

        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(bucketStorageProvider.uploadFile(any(DocumentType.class), anyString(), any(byte[].class), eq("application/pdf")))
                .thenThrow(storageFailure);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.uploadGeneratedDocument(DocumentType.PAYSLIP, loggedEmployeeId, null, new byte[]{1}, "ok.pdf")
        );

        assertEquals(storageFailure, exception);
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Nome",
                "12345678901",
                "12345678901",
                "Dev",
                "dev@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }

    private Document buildDocument(UUID documentId, UUID employeeId, String storagePath) {
        return new Document(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                storagePath,
                LocalDateTime.now(),
                null,
                false,
                false
        );
    }

    private byte[] docxBytes(boolean validStructure) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            List<String> entries = new ArrayList<>();
            if (validStructure) {
                entries.add("[Content_Types].xml");
                entries.add("word/document.xml");
            } else {
                entries.add("custom/data.xml");
            }

            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write("<xml/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
