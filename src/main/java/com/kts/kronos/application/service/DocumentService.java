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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kts.kronos.application.exceptions.BadRequestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    private static final String FORBIDDEN_OTHER_EMPLOYEE_DELETE = "Você não pode apagar documentos de outro funcionário.";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_MIME = Map.of(
            "application/pdf", Set.of("pdf"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "application/msword", Set.of("doc"),
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")
    );

    private final DocumentProvider documentProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BucketStorageProvider bucketStorageProvider;
    private final DomainAuthorizationService domainAuthorizationService;

     
    @Override
    public void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException {
        uploadDocumentInternal(type, employeeId, null, file);
    }

    @Override
    public DocumentWithData downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        var doc = domainAuthorizationService.authorizeDocumentAccess(documentId, employeeId);

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
            log.warn("Documento não encontrado durante download. documentId={}, requestedEmployeeId={}",
                    documentId, employeeId);
            throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
        } catch (RuntimeException e) {
            log.error("Falha interna no download do documento. documentId={}, requestedEmployeeId={}, storagePath={}",
                    documentId, employeeId, doc.storagePath(), e);
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
        var currentUserRole = jwtAuthenticatedUser.getCurrentRole();
        var currentUserId = jwtAuthenticatedUser.getEmployeeId();
        var doc = domainAuthorizationService.authorizeDocumentAccess(documentId, employeeId);

        var loggedInEmployeeId = jwtAuthenticatedUser.getEmployeeId();

        if (doc.type() == DocumentType.TIME_OFF) {
            if (!doc.employeeId().equals(loggedInEmployeeId)) {
                throw new ForbiddenException(ONLY_OWNER_DELETE_TIME_OFF_DOCS);
            }
        }

        Document updatedDoc;
        boolean isManager = currentUserRole == Role.MANAGER || currentUserRole == Role.CTO;

        if (isManager) {
            updatedDoc = doc.markDeletedByManager();
        } else {
            if (!doc.employeeId().equals(currentUserId)) {
                throw new ForbiddenException(FORBIDDEN_OTHER_EMPLOYEE_DELETE);
            }
            updatedDoc = doc.markDeletedByEmployee();
        }

        if (updatedDoc.deletedByEmployee() && updatedDoc.deletedByManager()) {
            bucketStorageProvider.deleteFile(doc.storagePath());
            documentProvider.delete(doc.employeeId(), doc.documentId());
        } else {
            documentProvider.save(updatedDoc);
        }
    }

    private Employee getAuthorizedEmployee(UUID employeeId) {
        return domainAuthorizationService.authorizeEmployeeAccess(employeeId);
    }

    private void uploadDocumentInternal(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        try {
            var uploadData = validateAndPrepareUpload(file);
            var employee = getAuthorizedEmployee(employeeId);
            var uniqueObjectName = employee.employeeId() + "/" + UUID.randomUUID() + "-" + uploadData.fileName();
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, uploadData.data(), uploadData.contentType());
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    uploadData.fileName(),
                    uploadData.contentType(),
                    storagePath, // USANDO O CAMINHO DO GCS
                    TIME_ZONE_BRAZIL,
                    timeRecordId,
                    false,false
            );
            documentProvider.save(doc);
        } catch (BadRequestException | ForbiddenException | ResourceNotFoundException e) {
            log.warn("Upload de documento rejeitado. type={}, employeeId={}, timeRecordId={}, originalFilename={}, exceptionType={}, message={}",
                    type,
                    employeeId,
                    timeRecordId,
                    file != null ? file.getOriginalFilename() : null,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        } catch (IOException e) {
            log.warn("Falha ao ler arquivo para upload. type={}, employeeId={}, timeRecordId={}, originalFilename={}",
                    type,
                    employeeId,
                    timeRecordId,
                    file != null ? file.getOriginalFilename() : null,
                    e);
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE);
        } catch (RuntimeException e) {
            log.error("Falha interna no upload do documento. type={}, employeeId={}, timeRecordId={}, originalFilename={}",
                    type,
                    employeeId,
                    timeRecordId,
                    file != null ? file.getOriginalFilename() : null,
                    e);
            throw e;
        }
    }

    @Override
    public void uploadGeneratedDocument(DocumentType type, UUID employeeId, Long timeRecordId, byte[] content, String fileName) {
        try {
            // Validação interna básica (opcional, já que geramos o PDF confiável)
            var contentType = "application/pdf";
            var safeFileName = sanitizeFileName(fileName);

            var employee = getAuthorizedEmployee(employeeId);
            // Define o caminho no Bucket
            var uniqueObjectName = employee.employeeId() + "/receipts/" + UUID.randomUUID() + "-" + safeFileName;

            // Upload Físico
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, content,contentType);

            // Salva Metadados no Banco
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    safeFileName,
                    contentType,
                    storagePath,
                    TIME_ZONE_BRAZIL,
                    timeRecordId,false,false
            );
            documentProvider.save(doc);

        } catch (BadRequestException | ForbiddenException | ResourceNotFoundException e) {
            log.warn("Persistência de documento gerado rejeitada. type={}, employeeId={}, timeRecordId={}, fileName={}, exceptionType={}, message={}",
                    type,
                    employeeId,
                    timeRecordId,
                    fileName,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha interna ao persistir documento gerado. type={}, employeeId={}, timeRecordId={}, fileName={}",
                    type,
                    employeeId,
                    timeRecordId,
                    fileName,
                    e);
            throw e;
        }
    }

    private UploadData validateAndPrepareUpload(MultipartFile file) throws IOException {
        var contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var safeFileName = sanitizeFileName(file.getOriginalFilename());
        var extension = extractExtension(safeFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var allowedExtensionsForMime = ALLOWED_EXTENSIONS_BY_MIME.get(contentType);
        if (allowedExtensionsForMime == null || !allowedExtensionsForMime.contains(extension)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        var bytes = file.getBytes();
        if (bytes.length == 0 || !matchesSignature(extension, bytes)) {
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }

        return new UploadData(bytes, safeFileName, contentType);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
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

    private boolean matchesSignature(String extension, byte[] bytes) {
        return switch (extension) {
            case "pdf" -> hasPrefix(bytes, 0x25, 0x50, 0x44, 0x46);
            case "jpg", "jpeg" -> hasPrefix(bytes, 0xFF, 0xD8, 0xFF);
            case "png" -> hasPrefix(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "doc" -> hasPrefix(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "docx" -> isDocx(bytes);
            default -> false;
        };
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

    private record UploadData(byte[] data, String fileName, String contentType) {}

}
