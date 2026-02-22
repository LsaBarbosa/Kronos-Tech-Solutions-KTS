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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kts.kronos.application.exceptions.BadRequestException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Messages.DOCUMENT_ACCESS_DENIED;
import static com.kts.kronos.constants.Messages.ERROR_FETCHING_STORAGE;
import static com.kts.kronos.constants.Messages.FAILURE_TO_SAVE_AUTO_GENERATED_DOC;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    private final DocumentProvider documentProvider;
    private final EmployeeProvider employeeProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BucketStorageProvider bucketStorageProvider;

     
    @Override
    public void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException {

        log.info(LOG_INIT_UPLOAD, type, employeeId);
        validateMimeType(file.getContentType());

        try {
            var employee = getValidatedEmployee(employeeId);
            var uniqueName = String.format("%s/%s-%s", employee.employeeId(), UUID.randomUUID(), file.getOriginalFilename());
            var storagePath = bucketStorageProvider.uploadFile(uniqueName, file.getBytes(), file.getContentType());

            var doc = new Document(
                    employee.employeeId(), type, file.getOriginalFilename(),
                    file.getContentType(), storagePath, TIME_ZONE_BRAZIL, null, false, false
            );
            documentProvider.save(doc);
            log.info(LOG_UPLOAD_SUCCESS, storagePath);
        } catch (Exception e) {
            log.error(LOG_UPLOAD_ERROR, e.getMessage());
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentWithData downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        log.debug(LOG_DOWNLOAD_REQUEST, documentId, employeeId);
        getValidatedEmployee(employeeId);

        Document doc = documentProvider.findById(documentId);
        validateDocumentOwnership(doc);

        try {
            byte[] fileData = bucketStorageProvider.downloadFile(doc.storagePath());
            return new DocumentWithData(
                    doc.documentId(), doc.employeeId(), doc.type(),
                    doc.fileName(), doc.contentType(), fileData, doc.uploadedAt()
            );
        } catch (Exception e) {
            log.error(LOG_DOWNLOAD_ERROR, e.getMessage());
            throw new BadRequestException(ERROR_FETCHING_STORAGE + e.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<Document> listDocuments(DocumentType type, UUID employeeId, LocalDate date) {
        var role = jwtAuthenticatedUser.getRoleFromToken();
        var isManagerView = "MANAGER".equals(role) || "CTO".equals(role);
        var targetId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        log.debug(LOG_LIST_DOCS, type, targetId, isManagerView);

        return (date == null)
                ? documentProvider.findByEmployeeAndType(targetId, type, isManagerView)
                : documentProvider.findByEmployeeAndDateAndType(targetId, date, type, isManagerView);
    }

    @Override
    public void uploadDocumentForTimeRecord(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        uploadDocumentInternal(type, employeeId, timeRecordId, file);
    }

    @Override
    public void deleteDocument(UUID employeeId, UUID documentId) {
        log.info(LOG_DELETE_REQUEST, documentId);
        var doc = documentProvider.findById(documentId);
        validateDocumentDeletionRights(doc);

        var role = jwtAuthenticatedUser.getRoleFromToken();
        var isManager = "MANAGER".equals(role);
        var updatedDoc = isManager ? doc.markDeletedByManager() : doc.markDeletedByEmployee();

        if (updatedDoc.deletedByEmployee() && updatedDoc.deletedByManager()) {
            log.info(LOG_DELETE_PHYSICAL, documentId);
            bucketStorageProvider.deleteFile(doc.storagePath());
            documentProvider.delete(doc.employeeId(), doc.documentId());
        } else {
            log.info(LOG_DELETE_SOFT, documentId);
            documentProvider.save(updatedDoc);
        }
    }

    @Override
    public void uploadGeneratedDocument(DocumentType type, UUID employeeId, Long timeRecordId, byte[] content, String fileName) {
        try {
            var employee = getValidatedEmployee(employeeId);
            var path = String.format("%s/receipts/%s-%s", employee.employeeId(), UUID.randomUUID(), fileName);
            var storagePath = bucketStorageProvider.uploadFile(path, content, "application/pdf");

            var doc = new Document(employee.employeeId(), type, fileName, "application/pdf",
                    storagePath, TIME_ZONE_BRAZIL, timeRecordId, false, false);
            documentProvider.save(doc);
            log.info(LOG_GEN_DOC_SUCCESS, fileName);
        } catch (Exception e) {
            log.error(LOG_GEN_DOC_ERROR, e.getMessage());
            throw new BadRequestException(FAILURE_TO_SAVE_AUTO_GENERATED_DOC + e.getMessage());
        }
    }

    private void validateMimeType(String contentType) {
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            log.warn(LOG_INVALID_MIME, contentType);
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }
    }

    private Employee getValidatedEmployee(UUID employeeId) {
        var targetId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return employeeProvider.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    private void validateDocumentOwnership(Document doc) {
        var loggedEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var role = jwtAuthenticatedUser.getRoleFromToken();

        if (!"MANAGER".equals(role) && !"CTO".equals(role) && !doc.employeeId().equals(loggedEmployeeId)) {
            log.error(LOG_ACCESS_DENIED, loggedEmployeeId, doc.employeeId());
            throw new ForbiddenException(DOCUMENT_ACCESS_DENIED);
        }
    }

    private void validateDocumentDeletionRights(Document doc) {
        var loggedId = jwtAuthenticatedUser.getEmployeeId();
        if (doc.type() == DocumentType.TIME_OFF && !doc.employeeId().equals(loggedId)) {
            throw new ForbiddenException(ONLY_OWNER);
        }
    }

    private void uploadDocumentInternal(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        validateMimeType(file.getContentType());
        var employee = getValidatedEmployee(employeeId);
        var uniqueName = String.format("%s/%s-%s", employee.employeeId(), UUID.randomUUID(), file.getOriginalFilename());
        var storagePath = bucketStorageProvider.uploadFile(uniqueName, file.getBytes(), file.getContentType());

        var doc = new Document(employee.employeeId(), type, file.getOriginalFilename(), file.getContentType(),
                storagePath, TIME_ZONE_BRAZIL, timeRecordId, false, false);
        documentProvider.save(doc);
    }

    private Employee getEmployee(UUID employeeId) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return employeeProvider.findById(employeeIdWith)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }
}
