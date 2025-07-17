package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Document;
import jakarta.persistence.*;
import lombok.*;
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
    @Id    @GeneratedValue(generator = "UUID")

    @Column(name = "document_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID documentId;

    @Column(name = "employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID employeeId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Lob
    @Column(name = "data", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] data;

    @Column(
            name = "uploaded_at",
            nullable = false,
            insertable = false,   // <— Hibernate não inclui no INSERT
            updatable = false     // <— Hibernate não inclui no UPDATE
    )
    private LocalDateTime uploadedAt;

    public Document toDomain(){
        return new Document(
                documentId,fileName, contentType,data,uploadedAt,employeeId
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
                .build();
    }
}
