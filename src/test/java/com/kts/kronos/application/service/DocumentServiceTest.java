package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentProvider documentProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock BucketStorageProvider bucketStorageProvider;

    @InjectMocks DocumentService service;

    @Test
    void listDocumentsUsesManagerViewWhenRoleManager() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(UUID.randomUUID());
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.DOCUMENTS, true)).thenReturn(List.of());

        var result = service.listDocuments(DocumentType.DOCUMENTS, employeeId, null);

        assertEquals(0, result.size());
    }

    @Test
    void listDocumentsUsesDateQueryWhenDateIsProvided() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(documentProvider.findByEmployeeAndDateAndType(employeeId, date, DocumentType.DOCUMENTS, false)).thenReturn(List.of());

        assertEquals(0, service.listDocuments(DocumentType.DOCUMENTS, employeeId, date).size());
    }

    @Test
    void listDocumentsUsesManagerViewWhenRoleCto() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(UUID.randomUUID());
        when(documentProvider.findByEmployeeAndDateAndType(employeeId, date, DocumentType.DOCUMENTS, true)).thenReturn(List.of());

        assertTrue(service.listDocuments(DocumentType.DOCUMENTS, employeeId, date).isEmpty());
    }

    @Test
    void downloadDocumentReturnsDataWhenManagerView() throws IOException {
        UUID employeeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document(docId, employeeId, DocumentType.DOCUMENTS, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(UUID.randomUUID());
        when(documentProvider.findById(docId)).thenReturn(doc);
        when(bucketStorageProvider.downloadFile("path")).thenReturn(new byte[]{1, 2});

        DocumentWithData result = service.downloadDocument(employeeId, docId);

        assertEquals(docId, result.documentId());
        assertArrayEquals(new byte[]{1, 2}, result.data());
    }

    @Test
    void downloadDocumentThrowsForbiddenForNonOwnerNonManager() {
        UUID employeeId = UUID.randomUUID();
        UUID loggedEmployee = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(docId, employeeId, DocumentType.DOCUMENTS, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployee);
        when(documentProvider.findByIdAndEmployeeId(docId, employeeId)).thenReturn(doc);

        assertThrows(ForbiddenException.class, () -> service.downloadDocument(employeeId, docId));
    }

    @Test
    void downloadDocumentWrapsStorageErrorAsBadRequest() {
        UUID employeeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(docId, employeeId, DocumentType.DOCUMENTS, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(UUID.randomUUID());
        when(documentProvider.findById(docId)).thenReturn(doc);
        when(bucketStorageProvider.downloadFile("path")).thenThrow(new RuntimeException("s3 down"));

        assertThrows(BadRequestException.class, () -> service.downloadDocument(employeeId, docId));
    }

    @Test
    void uploadDocumentSavesDocumentWhenValid() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn(new byte[]{1});
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenReturn("stored-path");

        service.uploadDocument(DocumentType.DOCUMENTS, employeeId, file);

        verify(documentProvider).save(any(Document.class));
    }

    @Test
    void uploadDocumentUsesFallbackNameWhenOriginalFileNameIsNull() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn(new byte[]{1});
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenReturn("stored-path");

        service.uploadDocument(DocumentType.DOCUMENTS, employeeId, file);

        verify(documentProvider).save(argThat(doc -> "document".equals(doc.fileName())));
    }

    @Test
    void uploadDocumentThrowsBadRequestWhenMimeTypeInvalid() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn("file.exe");
        when(file.getContentType()).thenReturn("application/octet-stream");
        when(file.getBytes()).thenReturn(new byte[]{1});

        assertThrows(BadRequestException.class, () -> service.uploadDocument(DocumentType.DOCUMENTS, employeeId, file));
    }

    @Test
    void uploadDocumentThrowsBadRequestWhenMimeTypeIsNull() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getContentType()).thenReturn(null);
        when(file.getBytes()).thenReturn(new byte[]{1});

        assertThrows(BadRequestException.class, () -> service.uploadDocument(DocumentType.DOCUMENTS, employeeId, file));
    }

    @Test
    void uploadDocumentThrowsResourceNotFoundWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.uploadDocument(DocumentType.DOCUMENTS, employeeId, file));
    }

    @Test
    void uploadDocumentWrapsUploadErrorAsBadRequest() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn(new byte[]{1});
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenThrow(new RuntimeException("s3 down"));

        assertThrows(BadRequestException.class, () -> service.uploadDocument(DocumentType.DOCUMENTS, employeeId, file));
    }

    @Test
    void uploadDocumentForTimeRecordSavesDocument() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn(new byte[]{1});
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenReturn("stored-path");

        service.uploadDocumentForTimeRecord(DocumentType.TIME_OFF, employeeId, 10L, file);

        verify(documentProvider).save(argThat(doc -> doc.timeRecordId().equals(10L)));
    }

    @Test
    void deleteDocumentSoftDeleteWhenOnlyOneSideDeletedByManager() {
        UUID employeeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID loggedManager = UUID.randomUUID();

        Document doc = new Document(docId, employeeId, DocumentType.DOCUMENTS, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedManager);
        when(documentProvider.findById(docId)).thenReturn(doc);

        service.deleteDocument(employeeId, docId);

        verify(documentProvider).save(argThat(Document::deletedByManager));
        verify(documentProvider, never()).delete(any(), any());
    }

    @Test
    void deleteDocumentPerformsHardDeleteWhenBothDeletedFlagsBecomeTrue() {
        UUID employeeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(docId, employeeId, DocumentType.DOCUMENTS, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, true, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(UUID.randomUUID());
        when(documentProvider.findById(docId)).thenReturn(doc);

        service.deleteDocument(employeeId, docId);

        verify(bucketStorageProvider).deleteFile("path");
        verify(documentProvider).delete(employeeId, docId);
        verify(documentProvider, never()).save(any());
    }

    @Test
    void deleteDocumentSoftDeleteWhenEmployeeDeletesOwnDocument() {
        UUID employeeId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(docId, employeeId, DocumentType.DOCUMENTS, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(documentProvider.findByIdAndEmployeeId(docId, employeeId)).thenReturn(doc);

        service.deleteDocument(employeeId, docId);

        verify(documentProvider).save(argThat(Document::deletedByEmployee));
    }

    @Test
    void deleteDocumentThrowsForbiddenForTimeOffWhenLoggedIsNotOwner() {
        UUID employeeId = UUID.randomUUID();
        UUID loggedId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(docId, employeeId, DocumentType.TIME_OFF, "a.pdf", "application/pdf", "path",
                LocalDateTime.now(), null, false, false);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedId);
        when(documentProvider.findById(docId)).thenReturn(doc);

        assertThrows(ForbiddenException.class, () -> service.deleteDocument(employeeId, docId));
    }

    @Test
    void uploadGeneratedDocumentWrapsStorageErrorAsBadRequest() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        when(employee.employeeId()).thenReturn(employeeId);

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenThrow(new RuntimeException("s3 down"));

        assertThrows(BadRequestException.class,
                () -> service.uploadGeneratedDocument(DocumentType.TIME_OFF, employeeId, 10L, new byte[]{1}, "doc.pdf"));
    }

    @Test
    void uploadGeneratedDocumentSavesDocumentOnSuccess() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        when(employee.employeeId()).thenReturn(employeeId);

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenReturn("stored-receipt");

        service.uploadGeneratedDocument(DocumentType.TIME_OFF, employeeId, 10L, new byte[]{1}, "doc.pdf");

        verify(documentProvider).save(any(Document.class));
    }

    @Test
    void uploadDocumentInternalCanBeInvokedByReflection() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        MultipartFile file = mock(MultipartFile.class);

        when(employee.employeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(file.getOriginalFilename()).thenReturn("private.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn(new byte[]{7});
        when(bucketStorageProvider.uploadFile(anyString(), any(), eq("application/pdf"))).thenReturn("stored-private");

        Method method = DocumentService.class.getDeclaredMethod("uploadDocumentInternal", DocumentType.class, UUID.class, Long.class, MultipartFile.class);
        method.setAccessible(true);
        method.invoke(service, DocumentType.DOCUMENTS, employeeId, 9L, file);

        verify(documentProvider).save(argThat(doc -> doc.timeRecordId().equals(9L)));
    }

    @Test
    void getEmployeePrivateMethodCanBeInvokedByReflection() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        Method method = DocumentService.class.getDeclaredMethod("getEmployee", UUID.class);
        method.setAccessible(true);
        Object result = method.invoke(service, employeeId);

        assertSame(employee, result);
    }

    @Test
    void getEmployeePrivateMethodThrowsWhenEmployeeNotFound() throws Exception {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        Method method = DocumentService.class.getDeclaredMethod("getEmployee", UUID.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, employeeId));

        assertTrue(exception.getCause() instanceof ResourceNotFoundException);
    }
}
