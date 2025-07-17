package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentUseCase {
    Document uploadDocument(UUID employeeId, MultipartFile file) throws IOException;
    Document downloadDocument(UUID employeeId,UUID documentId) throws IOException;
    List<Document> listDocuments(UUID employeeId, LocalDate date);

}
