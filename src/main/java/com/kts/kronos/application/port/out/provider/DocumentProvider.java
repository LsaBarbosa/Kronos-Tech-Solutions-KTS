package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentProvider {
    Document save(Document doc);
    Document findById(UUID documentId);
    List<Document> findAllByEmployee(UUID employeeId);
    List<Document> findByEmployeeAndDate(UUID employeeId, LocalDate date);
}
