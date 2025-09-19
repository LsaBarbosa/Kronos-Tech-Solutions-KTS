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

    @Column(name = "employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Lob
    @Column(name = "data",  columnDefinition = "BYTEA", nullable = false)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] data;

    @CreationTimestamp // Esta anotação fará com que o Hibernate defina a data automaticamente
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name="document_type", nullable=false)
    private DocumentType type;

    public Document toDomain(){
        return new Document(
                documentId,
                employeeId,
                type,
                fileName,
                contentType,
                data,
                uploadedAt
        );
    }
    public static DocumentEntity fromDomain(Document document){
        return DocumentEntity.builder()
                .documentId(document.documentId())
                .employeeId(document.employeeId())
                .fileName(document.fileName())
                .contentType(document.contentType())
                .data(document.data())
                .uploadedAt(document.uploadeAt())
                .type(document.type())
                .build();
    }
}
