package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.domain.model.Document;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProviderImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DocumentProviderImpl provider;

    @Test
    void deveBuscarVisaoDeManager() {
        UUID employeeId = UUID.randomUUID();

        when(documentRepository.findVisibleToManager(employeeId, DocumentType.TIME_OFF))
                .thenReturn(List.of(documentEntity(employeeId, "manager.pdf")));

        var result = provider.findByEmployeeAndType(employeeId, DocumentType.TIME_OFF, true);

        assertEquals(1, result.size());
        assertEquals("manager.pdf", result.getFirst().fileName());
        verify(documentRepository).findVisibleToManager(employeeId, DocumentType.TIME_OFF);
        verify(documentRepository, never()).findVisibleToEmployee(any(), any());
    }

    @Test
    void deveBuscarVisaoDeEmployeeComData() {
        UUID employeeId = UUID.randomUUID();

        when(documentRepository.findVisibleToEmployeeByDate(
                eq(employeeId),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(DocumentType.TIME_OFF)
        )).thenReturn(List.of(documentEntity(employeeId, "employee.pdf")));

        var result = provider.findByEmployeeAndDateAndType(
                employeeId,
                LocalDate.of(2026, 4, 17),
                DocumentType.TIME_OFF,
                false
        );

        assertEquals(1, result.size());
        assertEquals("employee.pdf", result.getFirst().fileName());
    }

    @Test
    void deveFalharAoDeletarDocumentoDeOutroEmployee() {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID ownerEmployeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(employeeRepository.findById(loggedEmployeeId)).thenReturn(Optional.of(employeeEntity(loggedEmployeeId)));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(documentEntity(ownerEmployeeId, "forbidden.pdf")));

        assertThrows(BadRequestException.class, () -> provider.delete(loggedEmployeeId, documentId));

        verify(documentRepository, never()).deleteById(any());
    }

    @Test
    void deveMapearBuscaPorTimeRecordIds() {
        UUID employeeId = UUID.randomUUID();

        when(documentRepository.findByTimeRecordIdIn(List.of(10L, 20L)))
                .thenReturn(List.of(
                        documentEntity(employeeId, "a.pdf"),
                        documentEntity(employeeId, "b.pdf")
                ));

        var result = provider.findByTimeRecordIds(List.of(10L, 20L));

        assertEquals(2, result.size());
    }

    private static EmployeeEntity employeeEntity(UUID employeeId) {
        return EmployeeEntity.builder()
                .employeeId(employeeId)
                .fullName("Pessoa Teste")
                .cpf("12345678901")
                .jobPosition("Analista")
                .email("teste@kts.com")
                .salary(1000.0)
                .companyId(UUID.randomUUID())
                .address(AddressEmbeddable.builder()
                        .street("Rua A")
                        .number("10")
                        .postalCode("65000000")
                        .city("São Luís")
                        .state("MA")
                        .build())
                .build();
    }

    private static DocumentEntity documentEntity(UUID employeeId, String fileName) {
        return DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId)
                .fileName(fileName)
                .contentType("application/pdf")
                .storagePath("docs/" + fileName)
                .uploadedAt(LocalDateTime.now())
                .type(DocumentType.TIME_OFF)
                .timeRecordId(10L)
                .deletedByEmployee(false)
                .deletedByManager(false)
                .build();
    }
}