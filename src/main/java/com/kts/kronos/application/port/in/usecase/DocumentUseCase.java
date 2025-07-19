package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentUseCase {
    void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException;
    Document downloadDocument(UUID employeeId,UUID documentId) throws IOException;
    List<Document> listDocuments(DocumentType type, UUID employeeId, LocalDate date);
    void deleteDocument(UUID employeeId, UUID documentId);
}
