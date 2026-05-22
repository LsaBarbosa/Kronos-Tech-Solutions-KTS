package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentProvider {
    void save(Document doc);
    Document findById(UUID documentId);
    Optional<Document> findByIdAndEmployeeId(UUID documentId, UUID employeeId);
    List<Document> findByEmployeeAndType(UUID employeeId, DocumentType type, boolean isManagerView);
    List<Document> findByEmployeeAndDateAndType(UUID employeeId, LocalDate date, DocumentType type, boolean isManagerView) ;
    void delete(UUID employeeId, UUID documentId);
    void deleteByEmployeeId(UUID employeeId);
    List<Document> findByTimeRecordId(Long timeRecordId);
    boolean existsByEmployeeIdAndType(UUID employeeId, DocumentType type);
    List<Document> findByTimeRecordIds(Collection<Long> timeRecordIds);
    List<Document> findAllByEmployeeId(UUID employeeId);
}
