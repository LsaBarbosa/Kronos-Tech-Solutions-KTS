package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_document")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class DocumentEntity {
    @Id
    @Column(name = "document_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID documentId;

    @Column(name = "employee_id",  nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "time_record_id")
    private Long timeRecordId;

    @Column(name = "storage_path", length = 512, nullable = false)
    private String storagePath;

    @CreationTimestamp // Esta anotação fará com que o Hibernate defina a data automaticamente
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType type;

    @Builder.Default
    @Column(name = "deleted_by_employee", nullable = false)
    private boolean deletedByEmployee = false;

    @Builder.Default
    @Column(name = "deleted_by_manager", nullable = false)
    private boolean deletedByManager = false;

    public Document toDomain() {
        return new Document(
                documentId,
                employeeId,
                type,
                fileName,
                contentType,
                storagePath,
                uploadedAt,
                timeRecordId,deletedByEmployee, deletedByManager
        );
    }

    public static DocumentEntity fromDomain(Document document) {
        return DocumentEntity.builder()
                .documentId(document.documentId())
                .employeeId(document.employeeId())
                .fileName(document.fileName())
                .contentType(document.contentType())
                .storagePath(document.storagePath())
                .uploadedAt(document.uploadeAt())
                .type(document.type())
                .timeRecordId(document.timeRecordId())
                .deletedByEmployee(document.deletedByEmployee())
                .deletedByManager(document.deletedByManager())
                .build();
    }
}