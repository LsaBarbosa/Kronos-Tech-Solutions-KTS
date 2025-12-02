package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kts.kronos.application.exceptions.BadRequestException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    public static final String ERROR_GET_FILE = "Falha ao buscar o arquivo no storage: ";
    private final DocumentProvider documentProvider;
    private final EmployeeProvider employeeProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BucketStorageProvider bucketStorageProvider;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/msword");
    @Override
    public void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException {

        var fileMimeType = file.getContentType();
        // 3. Verifique se o tipo está na lista permitida
        if (!ALLOWED_MIME_TYPES.contains(fileMimeType)) {
            // Se não estiver, lança a exceção
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }
        try {
            var employee = getEmployee(employeeId);

            var bytes = file.getBytes();
            var uniqueObjectName = employee.employeeId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, bytes, file.getContentType());
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    storagePath, // USANDO O CAMINHO DO GCS
                    TIME_ZONE_BRAZIL,null
            );
            documentProvider.save(doc);
        } catch (Exception e) {
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE + ": " + e.getMessage());
        }
    }

    @Override
    public DocumentWithData downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        getEmployee(employeeId);

        var doc = documentProvider.findById(documentId);

        try {
            byte[] fileData = bucketStorageProvider.downloadFile(doc.storagePath());

            return new DocumentWithData(
                    doc.documentId(),
                    doc.employeeId(),
                    doc.type(),
                    doc.fileName(),
                    doc.contentType(),
                    fileData,
                    doc.uploadeAt()
            );

        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
        } catch (RuntimeException e) {
            throw new BadRequestException(ERROR_GET_FILE + e.getMessage());
        }
    }

    @Override
    public List<Document> listDocuments(DocumentType type, UUID employeeId, LocalDate date) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return date == null
                ? documentProvider.findByEmployeeAndType(employeeIdWith, type)
                : documentProvider.findByEmployeeAndDateAndType(employeeIdWith, date, type);
    }

    @Override
    public void uploadDocumentForTimeRecord(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        uploadDocumentInternal(type, employeeId, timeRecordId, file);
    }

    @Override
    public void deleteDocument(UUID employeeId, UUID documentId) {
        var doc = documentProvider.findById(documentId);
        var loggedInEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        if (doc.type() == DocumentType.TIME_OFF) {
            if (!doc.employeeId().equals(loggedInEmployeeId)) {
                throw new ForbiddenException(
                        "Apenas o proprietário pode excluir documentos de justificativa de abono (TIME_OFF)."
                );
            }
        }
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        bucketStorageProvider.deleteFile(doc.storagePath());
        documentProvider.delete(employeeIdWith, documentId);
    }

    private Employee getEmployee(UUID employeeId) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return employeeProvider.findById(employeeIdWith)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    private void uploadDocumentInternal(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        var fileMimeType = file.getContentType();
        // 3. Verifique se o tipo está na lista permitida
        if (!ALLOWED_MIME_TYPES.contains(fileMimeType)) {
            // Se não estiver, lança a exceção
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }
        try {
            var employee = getEmployee(employeeId);

            var bytes = file.getBytes();
            var uniqueObjectName = employee.employeeId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, bytes, file.getContentType());
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    storagePath, // USANDO O CAMINHO DO GCS
                    TIME_ZONE_BRAZIL,
                    timeRecordId // NOVO CAMPO: timeRecordId
            );
            documentProvider.save(doc);
        } catch (Exception e) {
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE + ": " + e.getMessage());
        }
    }

}
