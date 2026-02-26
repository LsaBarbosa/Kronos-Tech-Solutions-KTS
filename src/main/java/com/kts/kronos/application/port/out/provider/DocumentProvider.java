package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DocumentProvider {
    Document save(Document doc);
    Document findById(UUID documentId);
    List<Document> findByEmployeeAndType(UUID employeeId, DocumentType type, boolean isManagerView);
    List<Document> findByEmployeeAndDateAndType(UUID employeeId, LocalDate date, DocumentType type, boolean isManagerView) ;
    void delete(UUID employeeId, UUID documentId);
    void deleteByEmployeeId(UUID employeeId);
    List<Document> findByTimeRecordId(Long timeRecordId);
    List<Document> findByTimeRecordIdIn(Set<Long> timeRecordIds);
    boolean existsByEmployeeIdAndType(UUID employeeId, DocumentType type);
    List<Document> saveAll(List<Document> documents);
    Document findLatestByEmployeeIdAndType(UUID employeeId, DocumentType type);
}
