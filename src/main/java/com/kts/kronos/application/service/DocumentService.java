package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import com.kts.kronos.domain.model.enuns.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kts.kronos.application.exceptions.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import java.util.HexFormat;

import static com.kts.kronos.constants.Messages.*;
import com.kts.kronos.application.port.out.provider.FileScanningProvider;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    private static final String FORBIDDEN_OTHER_EMPLOYEE_DELETE = "Você não pode apagar documentos de outro funcionário.";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final HexFormat HEX = HexFormat.of();
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_MIME = Map.of(
            "application/pdf", Set.of("pdf"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png")
    );

    private final DocumentProvider documentProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BucketStorageProvider bucketStorageProvider;
    private final DomainAuthorizationService domainAuthorizationService;
    private final FileScanningProvider fileScanningProvider;
    private final AuditService auditService;
    private final AuditRequestContextService auditRequestContextService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Value("${kronos.security.upload.max-bytes:5242880}")
    private long maxUploadBytes;
     
    @Override
    public void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException {
        uploadDocumentInternal(type, employeeId, null, file);
    }

    @Override
    public DocumentWithData downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        var doc = domainAuthorizationService.authorizeDocumentAccess(documentId, employeeId);
        var documentType = normalizeDocumentType(doc.type());

        try {
            if (doc.storagePath() == null || doc.storagePath().isBlank()) {
                metrics().documentDownloadFailure(documentType, "invalid_storage_path");
                log.warn("event=document_download result=failure document_type={} reason=invalid_storage_path document_id={}",
                        documentType, documentId);
                throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
            }

            byte[] fileData = tracing().observe("kronos.document.download", () -> bucketStorageProvider.downloadFile(
                    doc.type(),
                    doc.storagePath()
            ), "document_type", documentType, "operation", "download");
            metrics().documentDownloadSuccess(documentType);
            log.info("event=document_download result=success document_type={} document_id={} file_size_bytes={}",
                    documentType, documentId, fileData.length);

            // Obter companyId do employee para auditoria completa (SPEC-003)
            var employee = getAuthorizedEmployee(employeeId);

            // Registrar auditoria com severidade apropriada
            registerDocumentAudit(
                    AuditAction.DOCUMENT_DOWNLOADED,
                    doc,
                    employee.companyId(),
                    null
            );

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
            if (e.getMessage().contains("Arquivo não encontrado")) {
                metrics().documentDownloadFailure(documentType, "storage_object_not_found");
                log.warn("event=document_download result=failure document_type={} reason=storage_object_not_found document_id={}",
                        documentType, documentId);
            } else {
                metrics().documentDownloadFailure(documentType, "metadata_not_found");
                log.warn("event=document_download result=failure document_type={} reason=metadata_not_found document_id={}",
                        documentType, documentId);
            }
            throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
        } catch (RuntimeException e) {
            metrics().documentDownloadFailure(documentType, "storage_error");
            log.error("event=document_download result=failure document_type={} reason=storage_error exception_type={} message={}",
                    documentType,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            throw new BadRequestException(ERROR_GET_FILE);
        }
    }


    @Override
    public List<Document> listDocuments(DocumentType type, UUID employeeId, LocalDate date) {
        var currentUserRole = jwtAuthenticatedUser.getCurrentRole();
        boolean isManagerView = currentUserRole == Role.MANAGER || currentUserRole == Role.CTO;

        var targetEmployee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        var targetEmployeeId = targetEmployee.employeeId();

        if (date == null) {
            return documentProvider.findByEmployeeAndType(targetEmployeeId, type, isManagerView);
        } else {
            return documentProvider.findByEmployeeAndDateAndType(targetEmployeeId, date, type, isManagerView);
        }
    }

    @Override
    public void uploadDocumentForTimeRecord(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        uploadDocumentInternal(type, employeeId, timeRecordId, file);
    }

    @Override
    public void deleteDocument(UUID employeeId, UUID documentId) {
        var documentType = "unknown";
        try {
            var currentUserRole = jwtAuthenticatedUser.getCurrentRole();
            var currentUserId = jwtAuthenticatedUser.getEmployeeId();
            var doc = domainAuthorizationService.authorizeDocumentAccess(documentId, employeeId);
            documentType = normalizeDocumentType(doc.type());
            var employee = getAuthorizedEmployee(employeeId);

            var loggedInEmployeeId = jwtAuthenticatedUser.getEmployeeId();

            if (doc.type() == DocumentType.TIME_OFF && !doc.employeeId().equals(loggedInEmployeeId)) {
                throw new ForbiddenException(ONLY_OWNER_DELETE_TIME_OFF_DOCS);
            }

            Document updatedDoc;
            boolean isManager = currentUserRole == Role.MANAGER || currentUserRole == Role.CTO;
            AuditAction softDeleteAction;

            if (isManager) {
                updatedDoc = doc.markDeletedByManager();
                softDeleteAction = AuditAction.DOCUMENT_SOFT_DELETED_BY_MANAGER;
            } else {
                if (!doc.employeeId().equals(currentUserId)) {
                    throw new ForbiddenException(FORBIDDEN_OTHER_EMPLOYEE_DELETE);
                }
                updatedDoc = doc.markDeletedByEmployee();
                softDeleteAction = AuditAction.DOCUMENT_SOFT_DELETED_BY_EMPLOYEE;
            }

            // Registrar soft delete (SPEC-003)
            registerDocumentAudit(
                    softDeleteAction,
                    doc,
                    employee.companyId(),
                    null
            );

            if (updatedDoc.deletedByEmployee() && updatedDoc.deletedByManager()) {
                bucketStorageProvider.deleteFile(
                        doc.type(),
                        doc.storagePath()
                );
                documentProvider.delete(doc.employeeId(), doc.documentId());

                // Registrar exclusão física (SPEC-003)
                registerDocumentAudit(
                        AuditAction.DOCUMENT_PHYSICALLY_DELETED,
                        doc,
                        employee.companyId(),
                        null
                );
            } else {
                documentProvider.save(updatedDoc);
            }

            metrics().documentDeleteSuccess(documentType);
            log.info("event=document_delete result=success document_type={}", documentType);
        } catch (BadRequestException | ForbiddenException | ResourceNotFoundException e) {
            metrics().documentDeleteFailure(documentType, "validation");
            log.warn("event=document_delete result=failure document_type={} reason=validation", documentType);
            throw e;
        } catch (RuntimeException e) {
            metrics().documentDeleteFailure(documentType, "unknown");
            log.error("event=document_delete result=failure document_type={} reason=unknown exception_type={}",
                    documentType,
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    private Employee getAuthorizedEmployee(UUID employeeId) {
        return domainAuthorizationService.authorizeEmployeeAccess(employeeId);
    }

    private void uploadDocumentInternal(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        try {
            var uploadData = validateAndPrepareUpload(file);
            var employee = getAuthorizedEmployee(employeeId);
            tracing().observe("kronos.document.upload", () -> {
                var uniqueObjectName = buildStorageKey(employee, type, uploadData.fileName());
                var storagePath = bucketStorageProvider.uploadFile(
                        type,
                        uniqueObjectName,
                        uploadData.data(),
                        uploadData.contentType()
                );
                var doc = new Document(
                        employee.employeeId(),
                        type,
                        uploadData.fileName(),
                        uploadData.contentType(),
                        storagePath,
                        TIME_ZONE_BRAZIL,
                        timeRecordId,
                        false,
                        false,
                        calculateSha256(uploadData.data())
                );
                documentProvider.save(doc);

                // Registrar auditoria de upload (SPEC-003)
                var additionalDetails = timeRecordId != null ? String.format("timeRecordId=%d", timeRecordId) : null;
                registerDocumentAudit(
                        AuditAction.DOCUMENT_UPLOADED,
                        doc,
                        employee.companyId(),
                        additionalDetails
                );
            }, "document_type", normalizeDocumentType(type), "operation", "upload");
            metrics().documentUploadSuccess(normalizeDocumentType(type));
            log.info("event=document_upload result=success document_type={}", normalizeDocumentType(type));
        } catch (BadRequestException | ForbiddenException | ResourceNotFoundException e) {
            metrics().documentUploadFailure(normalizeDocumentType(type), "validation");
            log.warn("event=document_upload result=failure document_type={} reason=validation",
                    normalizeDocumentType(type));
            throw e;
        } catch (IOException e) {
            metrics().documentUploadFailure(normalizeDocumentType(type), "io");
            log.warn("event=document_upload result=failure document_type={} reason=io",
                    normalizeDocumentType(type));
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE);
        } catch (RuntimeException e) {
            metrics().documentUploadFailure(normalizeDocumentType(type), "unknown");
            log.error("event=document_upload result=failure document_type={} reason=unknown exception_type={}",
                    normalizeDocumentType(type),
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    public UUID uploadGeneratedDocument(DocumentType type, UUID employeeId, Long timeRecordId, byte[] content, String fileName) {
        try {
            // Validação interna básica (opcional, já que geramos o PDF confiável)
            var contentType = "application/pdf";
            var safeFileName = sanitizeFileName(fileName);

            var employee = getAuthorizedEmployee(employeeId);
            // Define o caminho no Bucket
            var uniqueObjectName = buildStorageKey(employee, type, safeFileName);

            // Upload Físico
            var storagePath = bucketStorageProvider.uploadFile(
                    type,
                    uniqueObjectName,
                    content,
                    contentType
            );

            // Salva Metadados no Banco
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    safeFileName,
                    contentType,
                    storagePath,
                    TIME_ZONE_BRAZIL,
                    timeRecordId,
                    false,
                    false,
                    calculateSha256(content)
            );
            documentProvider.save(doc);

            // Registrar auditoria de documento gerado (SPEC-003)
            var additionalDetails = timeRecordId != null ? String.format("timeRecordId=%d", timeRecordId) : null;
            registerDocumentAudit(
                    AuditAction.DOCUMENT_GENERATED,
                    doc,
                    employee.companyId(),
                    additionalDetails
            );

            return doc.documentId();
        } catch (BadRequestException | ForbiddenException | ResourceNotFoundException e) {
            log.warn("event=document_upload result=failure document_type={} reason=validation",
                    normalizeDocumentType(type));
            throw e;
        } catch (RuntimeException e) {
            log.error("event=document_upload result=failure document_type={} reason=unknown exception_type={}",
                    normalizeDocumentType(type),
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    private UploadData validateAndPrepareUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        if (file.getSize() > maxUploadBytes) {
            throw new BadRequestException(FILE_TOO_LARGE);
        }

        var safeFileName = sanitizeFileName(file.getOriginalFilename());
        var extension = extractExtension(safeFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var detectedContentType = detectRealMimeType(bytes);
        if (!ALLOWED_MIME_TYPES.contains(detectedContentType)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var allowedExtensionsForMime = ALLOWED_EXTENSIONS_BY_MIME.get(detectedContentType);
        if (allowedExtensionsForMime == null || !allowedExtensionsForMime.contains(extension)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        fileScanningProvider.scanOrThrow(safeFileName, detectedContentType, bytes);

        return new UploadData(bytes, safeFileName, detectedContentType);
    }




    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var normalized = Normalizer.normalize(originalFileName, Normalizer.Form.NFKC).replace('\\', '/');
        var baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
        baseName = baseName.replaceAll("[\\p{Cntrl}]", "");
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        baseName = baseName.replaceAll("^\\.+", "");

        if (baseName.isBlank()) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var extension = extractExtension(baseName);
        var fileNameWithoutExtension = baseName.substring(0, baseName.lastIndexOf('.'));

        if (fileNameWithoutExtension.isBlank()) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        if (fileNameWithoutExtension.length() > 100) {
            fileNameWithoutExtension = fileNameWithoutExtension.substring(0, 100);
        }

        return fileNameWithoutExtension + "." + extension;
    }

    private String extractExtension(String fileName) {
        var extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0 || extensionIndex == fileName.length() - 1) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }
        return fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String detectRealMimeType(byte[] bytes) {
        if (hasPrefix(bytes, 0x25, 0x50, 0x44, 0x46)) {
            return "application/pdf";
        }
        if (hasPrefix(bytes, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (hasPrefix(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (hasPrefix(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            return "application/msword";
        }
        if (isDocx(bytes)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "";
    }

    private boolean isDocx(byte[] bytes) {
        if (!hasPrefix(bytes, 0x50, 0x4B, 0x03, 0x04)) {
            return false;
        }

        try (var zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            boolean hasContentTypes = false;
            boolean hasWordFolder = false;
            int entriesRead = 0;

            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null && entriesRead < 200) {
                var entryName = entry.getName();
                if ("[Content_Types].xml".equals(entryName)) {
                    hasContentTypes = true;
                }
                if (entryName.startsWith("word/")) {
                    hasWordFolder = true;
                }
                if (hasContentTypes && hasWordFolder) {
                    return true;
                }
                entriesRead++;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasPrefix(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String buildStorageKey(Employee employee, DocumentType type, String fileName) {
        var now = LocalDate.now();
        return String.format(
                "company/%s/employee/%s/%s/%d/%02d/%s-%s",
                employee.companyId(),
                employee.employeeId(),
                type.name(),
                now.getYear(),
                now.getMonthValue(),
                UUID.randomUUID(),
                fileName
        );
    }

    private String normalizeDocumentType(DocumentType type) {
        return type == null ? "unknown" : type.name().toLowerCase(Locale.ROOT);
    }

    private String calculateSha256(byte[] payload) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ERROR_TO_GENERATE_HASH, e);
        }
    }

    /**
     * Determina a severidade de auditoria conforme tipo documental (SPEC-003).
     * Documentos sensíveis recebem severidade HIGH, demais recebem MEDIUM.
     */
    private String getAuditSeverityForDocumentType(DocumentType type) {
        if (type == null) {
            return "MEDIUM";
        }
        return switch (type) {
            case BIOMETRIC_CONSENT_TERM, TIME_OFF, EMPLOYEE_DOCUMENTS -> "HIGH";
            case POINT_RECORD_RECEIPT, SERVICE_CONTRACT_TERMS -> "MEDIUM";
            default -> "MEDIUM";
        };
    }

    /**
     * Registra auditoria de operação documental (SPEC-003).
     * Encapsula lógica de obtenção de contexto e preenchimento de campos.
     */
    private void registerDocumentAudit(
            AuditAction action,
            Document doc,
            UUID companyId,
            String additionalDetails
    ) {
        try {
            var auditContext = auditRequestContextService.extractContext();
            var severity = getAuditSeverityForDocumentType(doc.type());
            var details = String.format(
                    "documentId=%s, documentType=%s%s",
                    doc.documentId(),
                    doc.type(),
                    additionalDetails != null && !additionalDetails.isBlank()
                            ? ", " + additionalDetails
                            : ""
            );

            auditService.register(
                    action,
                    currentUserIdOrNull(),
                    doc.employeeId(),
                    companyId,
                    "DOCUMENT",
                    doc.documentId().toString(),
                    severity,
                    auditContext.ipAddress(),
                    auditContext.userAgent(),
                    details
            );
        } catch (Exception e) {
            log.warn("event=document_audit result=failure action={} document_id={} exception_type={}",
                    action.name(), doc.documentId(), e.getClass().getSimpleName(), e);
            // Não bloqueia operação se auditoria falhar
        }
    }

    private record UploadData(byte[] data, String fileName, String contentType) {}

    private UUID currentUserIdOrNull() {
        try {
            return jwtAuthenticatedUser.getuserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private KronosMetrics metrics() {
        return kronosMetrics != null ? kronosMetrics : ObservabilityDefaults.metrics();
    }

    private KronosTracing tracing() {
        return kronosTracing != null ? kronosTracing : ObservabilityDefaults.tracing();
    }

}
