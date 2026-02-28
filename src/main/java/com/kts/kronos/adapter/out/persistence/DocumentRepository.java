package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    List<DocumentEntity> findByEmployeeIdAndType(UUID employeeId, DocumentType type);

    @Query(value = """
            SELECT *
              FROM tb_document d
             WHERE d.employee_id = :emp
               AND d.document_type = :type
               AND d.uploaded_at BETWEEN :start AND :end
            """, nativeQuery = true)
    List<DocumentEntity> findByEmployeeIdAndTypeAndUploadedAtBetween(@Param("emp") UUID employeeId, @Param("start") Instant start, @Param("end") Instant end, @Param("type") String documentType);

    void deleteByEmployeeId(UUID employeeId);

    List<DocumentEntity> findByTimeRecordId(Long timeRecordId);

    List<DocumentEntity> findByTimeRecordIdIn(Set<Long> timeRecordIds);

    boolean existsByEmployeeIdAndType(UUID employeeId, DocumentType type);

    @Query("""
                SELECT d FROM DocumentEntity d
                WHERE d.employeeId = :employeeId
                  AND d.type = :type
                  AND d.uploadedAt BETWEEN :start AND :end
                  AND d.deletedByManager = false
                ORDER BY d.uploadedAt DESC
            """)
    List<DocumentEntity> findVisibleToManagerByDate(@Param("employeeId") UUID employeeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("type") DocumentType type);

    @Query("""
                SELECT d FROM DocumentEntity d
                WHERE d.employeeId = :employeeId
                  AND d.type = :type
                  AND d.uploadedAt BETWEEN :start AND :end
                  AND d.deletedByEmployee = false
                ORDER BY d.uploadedAt DESC
            """)
    List<DocumentEntity> findVisibleToEmployeeByDate(@Param("employeeId") UUID employeeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("type") DocumentType type);

    @Query("""
            SELECT d FROM DocumentEntity d
            WHERE d.employeeId = :employeeId
              AND d.type = :type
              AND d.deletedByManager = false
            ORDER BY d.uploadedAt DESC
            """)
    List<DocumentEntity> findVisibleToManager(@Param("employeeId") UUID employeeId, @Param("type") DocumentType type);

    @Query("""
            SELECT d FROM DocumentEntity d
            WHERE d.employeeId = :employeeId
              AND d.type = :type
              AND d.deletedByEmployee = false
            ORDER BY d.uploadedAt DESC
            """)
    List<DocumentEntity> findVisibleToEmployee(@Param("employeeId") UUID employeeId, @Param("type") DocumentType type);
    Optional<DocumentEntity> findTopByEmployeeIdAndTypeOrderByUploadedAtDesc(UUID employeeId, DocumentType type);
    Optional<DocumentEntity> findByIdAndEmployeeId(UUID documentId, UUID employeeId);
}