package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void listDocumentsWithDateUsesDateQuery() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(documentProvider.findByEmployeeAndDateAndType(employeeId, date, DocumentType.DOCUMENTS, false)).thenReturn(List.of());

        assertEquals(0, service.listDocuments(DocumentType.DOCUMENTS, employeeId, date).size());
    }
}
