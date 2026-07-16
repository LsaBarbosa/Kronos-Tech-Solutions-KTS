package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
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
    void deveSalvarDocumentoConvertendoParaEntidade() {
        UUID employeeId = UUID.randomUUID();
        DocumentEntity entity = documentEntity(employeeId, "save.pdf");
        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(entity);

        provider.save(entity.toDomain());

        verify(documentRepository).save(any(DocumentEntity.class));
    }

    @Test
    void deveBuscarPorIdOuFalharQuandoNaoExiste() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentEntity entity = documentEntity(employeeId, "found.pdf");
        entity.setDocumentId(documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));
        when(documentRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .thenReturn(Optional.empty());

        assertEquals(documentId, provider.findById(documentId).documentId());
        assertThrows(ResourceNotFoundException.class, () ->
                provider.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    @Test
    void deveBuscarPorIdEEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentEntity entity = documentEntity(employeeId, "owned.pdf");
        entity.setDocumentId(documentId);

        when(documentRepository.findByDocumentIdAndEmployeeId(documentId, employeeId))
                .thenReturn(Optional.of(entity));

        assertEquals(documentId, provider.findByIdAndEmployeeId(documentId, employeeId).orElseThrow().documentId());
    }

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
    void deveBuscarVisaoDeEmployee() {
        UUID employeeId = UUID.randomUUID();

        when(documentRepository.findVisibleToEmployee(employeeId, DocumentType.PAYSLIP))
                .thenReturn(List.of(documentEntity(employeeId, "employee.pdf")));

        var result = provider.findByEmployeeAndType(employeeId, DocumentType.PAYSLIP, false);

        assertEquals(1, result.size());
        assertEquals("employee.pdf", result.getFirst().fileName());
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
    void deveBuscarVisaoDeManagerComData() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 17);

        when(documentRepository.findVisibleToManagerByDate(
                eq(employeeId),
                eq(date.atStartOfDay()),
                eq(date.atTime(23, 59, 59)),
                eq(DocumentType.TIME_OFF)
        )).thenReturn(List.of(documentEntity(employeeId, "manager-date.pdf")));

        var result = provider.findByEmployeeAndDateAndType(employeeId, date, DocumentType.TIME_OFF, true);

        assertEquals(1, result.size());
        assertEquals("manager-date.pdf", result.getFirst().fileName());
    }

    @Test
    void deveDeletarDocumentoDoProprioEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentEntity entity = documentEntity(employeeId, "own.pdf");
        entity.setDocumentId(documentId);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employeeEntity(employeeId)));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));

        provider.delete(employeeId, documentId);

        verify(documentRepository).deleteById(documentId);
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
    void deveFalharAoDeletarQuandoEmployeeNaoExiste() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> provider.delete(employeeId, documentId));
    }

    @Test
    void deveDelegarDeleteByEmployeeIdExistsEBuscaPorTimeRecord() {
        UUID employeeId = UUID.randomUUID();
        when(documentRepository.findByTimeRecordId(10L)).thenReturn(List.of(documentEntity(employeeId, "tr.pdf")));
        when(documentRepository.existsByEmployeeIdAndType(employeeId, DocumentType.TIME_OFF)).thenReturn(true);

        provider.deleteByEmployeeId(employeeId);

        assertEquals(1, provider.findByTimeRecordId(10L).size());
        assertEquals(true, provider.existsByEmployeeIdAndType(employeeId, DocumentType.TIME_OFF));
        verify(documentRepository).deleteByEmployeeId(employeeId);
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

    @Test
    void deveBuscarTodosDocumentosDoEmployee() {
        UUID employeeId = UUID.randomUUID();
        var docs = List.of(
                documentEntity(employeeId, "a.pdf"),
                documentEntity(employeeId, "b.pdf")
        );
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(docs);

        var result = provider.findAllByEmployeeId(employeeId);

        assertEquals(2, result.size());
        assertEquals("a.pdf", result.get(0).fileName());
        assertEquals("b.pdf", result.get(1).fileName());
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
