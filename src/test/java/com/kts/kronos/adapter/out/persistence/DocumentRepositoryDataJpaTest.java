package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private TestEntityManager em;

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
    void deveBuscarPorEmployeeIdETipo() {
        UUID employeeId = UUID.randomUUID();
        repository.save(document(employeeId, DocumentType.PAYSLIP, nowMinusHours(2), false, false, null, "pay.pdf"));
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(1), false, false, null, "time-off.pdf"));

        var result = repository.findByEmployeeIdAndType(employeeId, DocumentType.PAYSLIP);

        assertEquals(1, result.size());
        assertEquals("pay.pdf", result.getFirst().getFileName());
    }

    @Test
    void deveBuscarPorDocumentIdEEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        DocumentEntity saved = repository.save(document(employeeId, DocumentType.PAYSLIP, nowMinusHours(2), false, false, null, "pay.pdf"));

        assertTrue(repository.findByDocumentIdAndEmployeeId(saved.getDocumentId(), employeeId).isPresent());
        assertTrue(repository.findByDocumentIdAndEmployeeId(saved.getDocumentId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void deveBuscarPorEmployeeTipoEUploadedAtEntreInstants() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 4, 20, 10, 0);
        repository.save(document(employeeId, DocumentType.PAYSLIP, uploadedAt, false, false, null, "inside.pdf"));
        repository.save(document(employeeId, DocumentType.PAYSLIP, uploadedAt.plusDays(2), false, false, null, "outside.pdf"));
        repository.flush();

        em.getEntityManager()
                .createQuery("""
                    UPDATE DocumentEntity d
                       SET d.uploadedAt = :uploadedAt
                     WHERE d.fileName = :fileName
                """)
                .setParameter("uploadedAt", uploadedAt)
                .setParameter("fileName", "inside.pdf")
                .executeUpdate();
        em.getEntityManager()
                .createQuery("""
                    UPDATE DocumentEntity d
                       SET d.uploadedAt = :uploadedAt
                     WHERE d.fileName = :fileName
                """)
                .setParameter("uploadedAt", uploadedAt.plusDays(2))
                .setParameter("fileName", "outside.pdf")
                .executeUpdate();
        em.flush();
        em.clear();

        var result = repository.findByEmployeeIdAndTypeAndUploadedAtBetween(
                employeeId,
                Instant.parse("2026-04-20T00:00:00Z"),
                Instant.parse("2026-04-20T23:59:59Z"),
                DocumentType.PAYSLIP.name()
        );

        assertEquals(1, result.size());
        assertEquals("inside.pdf", result.getFirst().getFileName());
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
    void deveFiltrarDocumentosVisiveisParaEmployeePorData() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        repository.save(document(employeeId, DocumentType.PAYSLIP, now, false, false, null, "inside.pdf"));
        repository.save(document(employeeId, DocumentType.PAYSLIP, now, true, false, null, "hidden.pdf"));
        repository.flush();

        var result = repository.findVisibleToEmployeeByDate(
                employeeId,
                now.minusMinutes(1),
                now.plusMinutes(1),
                DocumentType.PAYSLIP
        );

        assertEquals(1, result.size());
        assertEquals("inside.pdf", result.getFirst().getFileName());
    }

    @Test
    void deveBuscarPorTimeRecordIdEExistenciaPorEmployeeIdETipo() {
        UUID employeeId = UUID.randomUUID();
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(2), false, false, 100L, "time-off.pdf"));

        assertEquals(1, repository.findByTimeRecordId(100L).size());
        assertTrue(repository.existsByEmployeeIdAndType(employeeId, DocumentType.TIME_OFF));
        assertFalse(repository.existsByEmployeeIdAndType(employeeId, DocumentType.PAYSLIP));
    }

    @Test
    void deveExcluirPorEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();
        repository.save(document(employeeId, DocumentType.TIME_OFF, nowMinusHours(2), false, false, 100L, "a.pdf"));
        repository.save(document(otherEmployeeId, DocumentType.TIME_OFF, nowMinusHours(1), false, false, 200L, "b.pdf"));
        repository.flush();

        repository.deleteByEmployeeId(employeeId);
        repository.flush();

        assertFalse(repository.existsByEmployeeIdAndType(employeeId, DocumentType.TIME_OFF));
        assertTrue(repository.existsByEmployeeIdAndType(otherEmployeeId, DocumentType.TIME_OFF));
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
