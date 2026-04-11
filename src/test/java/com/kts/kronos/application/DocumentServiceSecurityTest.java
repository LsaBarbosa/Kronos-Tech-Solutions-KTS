package com.kts.kronos.application;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.DocumentService;
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
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.ERROR_GET_FILE;
import static com.kts.kronos.constants.Messages.INVALID_DOCUMENT_TYPE;
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
    private EmployeeProvider employeeProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private BucketStorageProvider bucketStorageProvider;

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
    }

    @Test
    @DisplayName("download: permite acesso ao próprio colaborador autorizado")
    void shouldAllowDownloadForAuthorizedEmployee() throws Exception {
        UUID documentId = UUID.randomUUID();
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        Document document = buildDocument(documentId, loggedEmployeeId, "docs/file.pdf");
        byte[] fileBytes = "payload".getBytes(StandardCharsets.UTF_8);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.isWithEmployeeId(null)).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(documentProvider.findByIdAndEmployeeId(documentId, loggedEmployeeId)).thenReturn(Optional.of(document));
        when(bucketStorageProvider.downloadFile(document.storagePath())).thenReturn(fileBytes);

        DocumentWithData response = service.downloadDocument(null, documentId);

        assertEquals(documentId, response.documentId());
        assertArrayEquals(fileBytes, response.data());
        verify(documentProvider).findByIdAndEmployeeId(documentId, loggedEmployeeId);
    }

    @Test
    @DisplayName("download: bloqueia manager em documento de outra empresa")
    void shouldBlockManagerDownloadFromOtherTenant() {
        UUID documentId = UUID.randomUUID();
        Employee managerEmployee = buildEmployee(managerEmployeeId, companyAId);
        Employee targetEmployee = buildEmployee(otherTenantEmployeeId, companyBId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(otherTenantEmployeeId)).thenReturn(otherTenantEmployeeId);
        when(employeeProvider.findById(otherTenantEmployeeId)).thenReturn(Optional.of(targetEmployee));
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));

        assertThrows(ForbiddenException.class, () -> service.downloadDocument(otherTenantEmployeeId, documentId));
        verify(documentProvider, never()).findByIdAndEmployeeId(any(), any());
    }

    @Test
    @DisplayName("delete: permite dono apagar documento conforme regra atual")
    void shouldAllowOwnerDeleteDocument() {
        UUID documentId = UUID.randomUUID();
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);
        Document document = buildDocument(documentId, loggedEmployeeId, "safe/object.pdf");

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(null)).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(documentProvider.findByIdAndEmployeeId(documentId, loggedEmployeeId)).thenReturn(Optional.of(document));

        service.deleteDocument(null, documentId);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentProvider).save(captor.capture());
        assertTrue(captor.getValue().deletedByEmployee());
        assertFalse(captor.getValue().deletedByManager());
        verify(bucketStorageProvider, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("delete: bloqueia exclusão cross-tenant")
    void shouldBlockDeleteCrossTenant() {
        UUID documentId = UUID.randomUUID();
        Employee managerEmployee = buildEmployee(managerEmployeeId, companyAId);
        Employee targetEmployee = buildEmployee(otherTenantEmployeeId, companyBId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(otherTenantEmployeeId)).thenReturn(otherTenantEmployeeId);
        when(employeeProvider.findById(otherTenantEmployeeId)).thenReturn(Optional.of(targetEmployee));
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));

        assertThrows(ForbiddenException.class, () -> service.deleteDocument(otherTenantEmployeeId, documentId));
        verify(documentProvider, never()).findByIdAndEmployeeId(any(), any());
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

        when(jwtAuthenticatedUser.isWithEmployeeId(null)).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(bucketStorageProvider.uploadFile(anyString(), any(byte[].class), anyString())).thenReturn("safe/storage/path");

        service.uploadDocument(DocumentType.PAYSLIP, null, file);

        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(bucketStorageProvider).uploadFile(objectNameCaptor.capture(), any(byte[].class), anyString());
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
        verify(bucketStorageProvider, never()).uploadFile(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("download: não vaza path interno em erro de storage")
    void shouldNotLeakInternalStoragePathOnDownloadError() {
        UUID documentId = UUID.randomUUID();
        Document document = buildDocument(documentId, loggedEmployeeId, "safe/object.pdf");
        Employee employee = buildEmployee(loggedEmployeeId, companyAId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.isWithEmployeeId(null)).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(documentProvider.findByIdAndEmployeeId(documentId, loggedEmployeeId)).thenReturn(Optional.of(document));
        when(bucketStorageProvider.downloadFile(anyString()))
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
        when(jwtAuthenticatedUser.isWithEmployeeId(null)).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(documentProvider.findByEmployeeAndType(loggedEmployeeId, DocumentType.PAYSLIP, false))
                .thenReturn(List.of());

        service.listDocuments(DocumentType.PAYSLIP, null, null);

        verify(jwtAuthenticatedUser, atLeastOnce()).getCurrentRole();
        verify(jwtAuthenticatedUser, never()).getRoleFromToken();
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
}
