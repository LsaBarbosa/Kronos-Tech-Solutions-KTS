package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.domain.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
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
    List<DocumentEntity> findByEmployeeIdAndTypeAndUploadedAtBetween(
            @Param("emp")   UUID employeeId,
            @Param("start") Instant start,
            @Param("end")   Instant end,
            @Param("type")  String documentType
    );
}
