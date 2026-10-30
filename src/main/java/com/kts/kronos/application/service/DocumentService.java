package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.in.web.dto.document.PayslipImportPreviewResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.UploadScannerUnavailableException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.DocumentScanStatus;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import com.kts.kronos.domain.model.enuns.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kts.kronos.constants.Messages.*;
import com.kts.kronos.application.port.out.provider.FileScanningProvider;
import org.springframework.beans.factory.annotation.Value;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    private static final String FORBIDDEN_OTHER_EMPLOYEE_DELETE = "Você não pode apagar documentos de outro funcionário.";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final HexFormat HEX = HexFormat.of();
    private static final Pattern CPF_PATTERN = Pattern.compile("(?<!\\d)(?:\\d{3}[.\\s]?){2}\\d{3}[-\\s]?\\d{2}(?!\\d)");
    private static final int MAX_PAYSLIP_PDF_PAGES = 100;
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_MIME = Map.of(
            "application/pdf", Set.of("pdf"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png")
    );

    private final DocumentProvider documentProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BucketStorageProvider bucketStorageProvider;
    private final DomainAuthorizationService domainAuthorizationService;
    private final EmployeeProvider employeeProvider;
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
    public PayslipImportPreviewResponse previewPayslipImport(MultipartFile file) throws IOException {
        ensurePayslipManager();
        var uploadData = validatePayslipPdf(file);
        fileScanningProvider.scanOrThrow(uploadData.fileName(), uploadData.contentType(), uploadData.data());
        var identity = extractPayslipIdentity(uploadData.data());
        var companyId = domainAuthorizationService.authorizeCompanyAccess(null);
        var response = buildPayslipPreview(uploadData.fileName(), companyId, identity);
        log.info("event=payslip_batch_preview result={} reason=completed", response.status());
        return response;
    }

    @Override
    public void importPayslip(UUID employeeId, boolean manualReviewConfirmed, MultipartFile file) throws IOException {
        ensurePayslipManager();
        var companyId = domainAuthorizationService.authorizeCompanyAccess(null);
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        if (!companyId.equals(employee.companyId())) {
            throw new ResourceNotFoundException(EMPLOYEE_NOT_FOUND);
        }

        var uploadData = validatePayslipPdf(file);
        fileScanningProvider.scanOrThrow(uploadData.fileName(), uploadData.contentType(), uploadData.data());
        var identity = extractPayslipIdentity(uploadData.data());
        var employeeCpf = normalizeCpf(employee.cpf());
        var cpfMatches = identity.validCpfs().contains(employeeCpf);
        var nameMatches = containsEmployeeName(identity.normalizedText(), employee.fullName());
        if (!identity.validCpfs().isEmpty()) {
            if (!cpfMatches) {
                throw new BadRequestException("O CPF encontrado no PDF não corresponde ao colaborador selecionado.");
            }
            if ((identity.validCpfs().size() > 1 || !nameMatches) && !manualReviewConfirmed) {
                throw new BadRequestException("Confira o nome e os CPFs do PDF e confirme manualmente a associação.");
            }
        } else if (!manualReviewConfirmed) {
            throw new BadRequestException("O PDF não contém CPF válido legível. Confira o documento e confirme manualmente a associação.");
        }

        // Reutiliza a validação, a varredura e a auditoria do fluxo documental existente.
        uploadDocumentInternal(DocumentType.PAYSLIP, employeeId, null, file);
        log.info("event=payslip_batch_import result=success document_type=PAYSLIP");
    }

    private void ensurePayslipManager() {
        if (jwtAuthenticatedUser.getCurrentRole() != Role.MANAGER) {
            throw new ForbiddenException("A importação de contracheques está disponível apenas para gestores.");
        }
    }

    private UploadData validatePayslipPdf(MultipartFile file) throws IOException {
        var uploadData = validateAndPrepareUpload(file, false);
        if (!"application/pdf".equals(uploadData.contentType())) {
            throw new BadRequestException("A importação em lote aceita somente arquivos PDF.");
        }
        return uploadData;
    }

    private PayslipImportPreviewResponse buildPayslipPreview(String fileName, UUID companyId, PayslipIdentity identity) {
        if (identity.text().isBlank()) {
            return payslipPreview(fileName, PayslipImportPreviewResponse.Status.UNREADABLE_PDF, null, null, null,
                    "O PDF não contém texto legível para identificar o colaborador.");
        }

        if (identity.validCpfs().size() == 1) {
            var cpf = identity.validCpfs().iterator().next();
            var employee = employeeProvider.findByCompanyIdAndCpf(companyId, cpf).orElse(null);
            if (employee == null || employee.deletedAt() != null) {
                return payslipPreview(fileName, PayslipImportPreviewResponse.Status.EMPLOYEE_NOT_FOUND, null, null,
                        maskCpf(cpf), "O CPF não corresponde a um colaborador disponível desta empresa.");
            }
            if (containsEmployeeName(identity.normalizedText(), employee.fullName())) {
                return payslipPreview(fileName, PayslipImportPreviewResponse.Status.MATCHED, employee.employeeId(),
                        employee.fullName(), maskCpf(cpf), "CPF e nome conferem.");
            }
            return payslipPreview(fileName, PayslipImportPreviewResponse.Status.MANUAL_REVIEW, employee.employeeId(),
                    employee.fullName(), maskCpf(cpf), "O CPF confere, mas o nome não foi localizado exatamente no PDF. Confira antes de confirmar.");
        }

        if (identity.validCpfs().size() > 1) {
            var employees = identity.validCpfs().stream()
                    .map(cpf -> employeeProvider.findByCompanyIdAndCpf(companyId, cpf).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .filter(employee -> employee.deletedAt() == null)
                    .distinct()
                    .toList();
            if (employees.size() == 1) {
                var employee = employees.get(0);
                return payslipPreview(fileName, PayslipImportPreviewResponse.Status.MANUAL_REVIEW, employee.employeeId(),
                        employee.fullName(), null, "O PDF contém mais de um CPF. Confira o documento e confirme a associação.");
            }
            return payslipPreview(fileName, PayslipImportPreviewResponse.Status.AMBIGUOUS, null, null, null,
                    "O PDF contém mais de um CPF de colaboradores desta empresa; não foi possível sugerir um destinatário.");
        }

        var nameMatches = employeeProvider.findByCompanyId(companyId).stream()
                .filter(employee -> employee.deletedAt() == null)
                .filter(employee -> containsEmployeeName(identity.normalizedText(), employee.fullName()))
                .toList();
        if (nameMatches.size() == 1) {
            var employee = nameMatches.get(0);
            var status = identity.cpfCandidateFound()
                    ? PayslipImportPreviewResponse.Status.INVALID_CPF
                    : PayslipImportPreviewResponse.Status.MANUAL_REVIEW;
            return payslipPreview(fileName, status, employee.employeeId(), employee.fullName(), null,
                    "O nome sugere este colaborador, mas não foi encontrado um CPF válido. Confira o PDF antes de confirmar.");
        }
        if (nameMatches.size() > 1) {
            return payslipPreview(fileName, PayslipImportPreviewResponse.Status.AMBIGUOUS, null, null, null,
                    "O nome aparece em mais de um cadastro. Não foi possível sugerir um destinatário.");
        }
        var status = identity.cpfCandidateFound()
                ? PayslipImportPreviewResponse.Status.INVALID_CPF
                : PayslipImportPreviewResponse.Status.NO_MATCH;
        return payslipPreview(fileName, status, null, null, null,
                "Não foi possível associar o PDF a um colaborador desta empresa.");
    }

    private PayslipImportPreviewResponse payslipPreview(
            String fileName, PayslipImportPreviewResponse.Status status, UUID employeeId, String employeeName,
            String maskedCpf, String message
    ) {
        return new PayslipImportPreviewResponse(fileName, status, employeeId, employeeName, maskedCpf,
                status != PayslipImportPreviewResponse.Status.MATCHED, message);
    }

    private PayslipIdentity extractPayslipIdentity(byte[] bytes) {
        var text = new StringBuilder();
        try (var pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            if (pdf.getNumberOfPages() > MAX_PAYSLIP_PDF_PAGES) {
                throw new BadRequestException("O PDF excede o limite de páginas para identificação automática.");
            }
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page), new LocationTextExtractionStrategy())).append(' ');
                if (text.length() > 1_000_000) {
                    break;
                }
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("PDF inválido, protegido por senha ou não legível.");
        }

        var rawText = text.toString();
        var normalizedText = normalizeIdentityText(rawText);
        var validCpfs = new LinkedHashSet<String>();
        boolean cpfCandidateFound = false;
        Matcher matcher = CPF_PATTERN.matcher(rawText);
        while (matcher.find()) {
            cpfCandidateFound = true;
            var cpf = normalizeCpf(matcher.group());
            if (isValidCpf(cpf)) {
                validCpfs.add(cpf);
            }
        }
        return new PayslipIdentity(rawText, normalizedText, Set.copyOf(validCpfs), cpfCandidateFound);
    }

    private String normalizeCpf(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean isValidCpf(String cpf) {
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        int firstDigit = cpfDigit(cpf, 9, 10);
        int secondDigit = cpfDigit(cpf, 10, 11);
        return cpf.charAt(9) - '0' == firstDigit && cpf.charAt(10) - '0' == secondDigit;
    }

    private int cpfDigit(String cpf, int length, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (cpf.charAt(index) - '0') * (initialWeight - index);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private String maskCpf(String cpf) {
        return "***.***.***-" + cpf.substring(cpf.length() - 2);
    }

    private boolean containsEmployeeName(String normalizedText, String fullName) {
        var normalizedName = normalizeIdentityText(fullName);
        return !normalizedName.isBlank() && (" " + normalizedText + " ").contains(" " + normalizedName + " ");
    }

    private String normalizeIdentityText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    @Override
    public DocumentWithData downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        var doc = domainAuthorizationService.authorizeDocumentAccess(documentId, employeeId);
        if (documentProvider.getScanStatus(documentId) != DocumentScanStatus.CLEAN) {
            throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
        }
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
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("event=document_download result=failure document_type={} reason=storage_error exception_type={} message={}",
                    documentType,
                    e.getClass().getSimpleName(),
                    detail,
                    e);
            throw new BadRequestException(ERROR_GET_FILE + detail);
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
    public Document uploadDocumentForTimeRecordPendingScan(DocumentType type, UUID employeeId, Long timeRecordId,
                                                           MultipartFile file) throws IOException {
        var uploadData = validateAndPrepareUpload(file, false);
        var employee = getAuthorizedEmployee(employeeId);
        var storagePath = buildStorageKey(employee, type, uploadData.fileName());
        var document = new Document(employee.employeeId(), type, uploadData.fileName(), uploadData.contentType(),
                storagePath, TIME_ZONE_BRAZIL, timeRecordId, false, false, calculateSha256(uploadData.data()));
        documentProvider.savePendingScan(document, uploadData.data());
        registerDocumentAudit(AuditAction.DOCUMENT_UPLOADED, document, employee.companyId(),
                String.format("timeRecordId=%d scanStatus=PENDING", timeRecordId));
        log.info("event=document_upload_scan result=pending document_type={} time_record_id={}",
                normalizeDocumentType(type), timeRecordId);
        return document;
    }

    @Scheduled(fixedDelayString = "${kronos.security.upload.scan.poll-ms:10000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processPendingDocumentScans() {
        var processedObjects = new java.util.HashSet<String>();
        for (var document : documentProvider.findPendingScans(20)) {
            var objectIdentity = document.type() + ":" + document.storagePath();
            if (!processedObjects.add(objectIdentity)) {
                continue;
            }
            try {
                var content = documentProvider.getPendingScanPayload(document.documentId());
                fileScanningProvider.scanOrThrow(document.fileName(), document.contentType(), content);
                bucketStorageProvider.uploadFile(document.type(), document.storagePath(), content, document.contentType());
                documentProvider.updateScanStatusByStoragePath(document.storagePath(), document.type(), DocumentScanStatus.CLEAN);
                log.info("event=document_upload_scan result=clean document_type={} document_id={}",
                        normalizeDocumentType(document.type()), document.documentId());
            } catch (BadRequestException malwareDetected) {
                documentProvider.updateScanStatusByStoragePath(document.storagePath(), document.type(), DocumentScanStatus.REJECTED);
                log.warn("event=document_upload_scan result=rejected document_type={} document_id={}",
                        normalizeDocumentType(document.type()), document.documentId());
            } catch (UploadScannerUnavailableException unavailable) {
                log.warn("event=document_upload_scan result=pending reason=scanner_unavailable document_id={}",
                        document.documentId());
            } catch (RuntimeException failure) {
                log.error("event=document_upload_scan result=pending reason=processing_error exception_type={} document_id={}",
                        failure.getClass().getSimpleName(), document.documentId());
            }
        }
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
        } catch (UploadScannerUnavailableException e) {
            metrics().documentUploadFailure(normalizeDocumentType(type), "scanner_unavailable");
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

            // Busca direta sem autorização: chamada interna (ex: terminal checkin sem JWT)
            var employee = employeeProvider.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
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
        return validateAndPrepareUpload(file, true);
    }

    private UploadData validateAndPrepareUpload(MultipartFile file, boolean scanImmediately) throws IOException {
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

        if (scanImmediately) {
            fileScanningProvider.scanOrThrow(safeFileName, detectedContentType, bytes);
        }

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

    private record PayslipIdentity(String text, String normalizedText, Set<String> validCpfs, boolean cpfCandidateFound) {}

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
