package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class DocumentRepositoryTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private DocumentRepository repository;

    @Test
    void deveFiltrarDocumentosVisiveisParaManager() {
        UUID employeeId = UUID.randomUUID();

        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(2), false, false, 10L, "ok-manager.pdf"));
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(1), false, true, 11L, "hidden-manager.pdf"));

        var result = repository.findVisibleToManager(employeeId, DocumentType.TIME_OFF);

        assertEquals(1, result.size());
        assertEquals("ok-manager.pdf", result.getFirst().getFileName());
    }

    @Test
    void deveFiltrarDocumentosVisiveisParaEmployee() {
        UUID employeeId = UUID.randomUUID();

        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(2), false, false, 10L, "ok-employee.pdf"));
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(1), true, false, 11L, "hidden-employee.pdf"));

        var result = repository.findVisibleToEmployee(employeeId, DocumentType.TIME_OFF);

        assertEquals(1, result.size());
        assertEquals("ok-employee.pdf", result.getFirst().getFileName());
    }

    @Test
    void deveFiltrarDocumentosVisiveisParaManagerPorData() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        repository.save(document(employeeId, DocumentType.PAYSLIP, now, false, false, null, "inside.pdf"));
        repository.save(document(employeeId, DocumentType.PAYSLIP, now, false, true, null, "hidden.pdf"));
        repository.flush();

        var result = repository.findVisibleToManagerByDate(
                employeeId,
                now.minusMinutes(1),
                now.plusMinutes(1),
                DocumentType.PAYSLIP
        );

        assertEquals(1, result.size());
        assertEquals("inside.pdf", result.getFirst().getFileName());
    }
    @Test
    void deveBuscarPorListaDeTimeRecordIds() {
        UUID employeeId = UUID.randomUUID();

        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(2), false, false, 100L, "a.pdf"));
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(1), false, false, 200L, "b.pdf"));
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(1), false, false, 300L, "c.pdf"));

        List<DocumentEntity> result = repository.findByTimeRecordIdIn(List.of(100L, 300L));

        assertEquals(2, result.size());
    }

    private static LocalDateTime nowMinusHours(int hours) {
        return LocalDateTime.now().minusHours(hours);
    }

    private static DocumentEntity document(
            UUID employeeId,
            DocumentType type,
            LocalDateTime uploadedAt,
            boolean deletedByEmployee,
            boolean deletedByManager,
            Long timeRecordId,
            String fileName
    ) {
        return DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId)
                .fileName(fileName)
                .contentType("application/pdf")
                .storagePath("docs/" + fileName)
                .uploadedAt(uploadedAt)
                .type(type)
                .timeRecordId(timeRecordId)
                .deletedByEmployee(deletedByEmployee)
                .deletedByManager(deletedByManager)
                .build();
    }
}