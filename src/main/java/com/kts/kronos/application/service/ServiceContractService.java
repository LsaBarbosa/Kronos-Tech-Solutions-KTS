package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.servicecontract.CreateServiceContractResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.PendingServiceContractListResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.PendingServiceContractResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminItemResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractSignatureAdminItemResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractSignatureAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractRequest;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.in.usecase.ServiceContractUseCase;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.ServiceContractAssignmentProvider;
import com.kts.kronos.application.port.out.provider.ServiceContractProvider;
import com.kts.kronos.application.port.out.provider.ServiceContractSignatureProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.ServiceContract;
import com.kts.kronos.domain.model.ServiceContractAssignment;
import com.kts.kronos.domain.model.ServiceContractSignature;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ContractSignatureMethod;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ContractSignatureType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceContractService implements ServiceContractUseCase {

    public static final ZoneId CONTRACT_ZONE = ZoneId.of("America/Sao_Paulo");
    public static final String DECLARATION_VERSION_V1 = "1.0";
    public static final String DECLARATION_TEMPLATE_V1 =
            "Declaro que li, conferi e concordo com o conteúdo integral do contrato \"%s\" " +
            "que me foi apresentado. Esta assinatura eletrônica registra minha ciência e " +
            "aceite das condições do referido contrato e estou ciente de que posso solicitar " +
            "esclarecimentos pelos canais internos aplicáveis antes da assinatura.";

    private final ServiceContractProvider contractProvider;
    private final ServiceContractAssignmentProvider assignmentProvider;
    private final ServiceContractSignatureProvider signatureProvider;
    private final EmployeeProvider employeeProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BiometricProtectionService biometricProtectionService;
    private final AuditService auditService;
    private final DocumentUseCase documentUseCase;
    private final DocumentProvider documentProvider;
    private final BucketStorageProvider bucketStorageProvider;
    private final DigitalSignatureService digitalSignatureService;
    private final EvidenceWatermarkService evidenceWatermarkService;

    // ==================== CREATE ====================

    @Override
    @Transactional
    public CreateServiceContractResponse create(
            String title,
            String description,
            List<UUID> employeeIds,
            MultipartFile pdfFile,
            String ipAddress,
            String userAgent
    ) {
        Role role = jwtAuthenticatedUser.getCurrentRole();
        if (role != Role.MANAGER && role != Role.CTO) {
            throw new ForbiddenException("Apenas gestores podem criar contratos.");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Título do contrato é obrigatório.");
        }
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new BadRequestException("É necessário atribuir o contrato a pelo menos um colaborador.");
        }
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new BadRequestException("Arquivo do contrato é obrigatório.");
        }
        if (!"application/pdf".equalsIgnoreCase(pdfFile.getContentType())) {
            throw new BadRequestException("Apenas arquivos PDF são aceitos.");
        }
        String originalFileName = sanitizeFileName(pdfFile.getOriginalFilename(), "contrato.pdf");
        if (!originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BadRequestException("Apenas arquivos PDF são aceitos.");
        }

        Employee manager = getAuthenticatedEmployee();
        UUID currentUserId = jwtAuthenticatedUser.getuserId();

        // Validar tenant: todos os employeeIds devem pertencer à mesma empresa do manager.
        Map<UUID, Employee> tenantEmployees = employeeProvider.findByCompanyId(manager.companyId())
                .stream().collect(Collectors.toMap(Employee::employeeId, e -> e));
        for (UUID assignedId : employeeIds) {
            if (!tenantEmployees.containsKey(assignedId)) {
                throw new ForbiddenException("Tentativa de atribuir contrato a colaborador de outro tenant.");
            }
        }

        byte[] pdfBytes;
        try {
            pdfBytes = pdfFile.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Não foi possível ler o arquivo PDF.");
        }
        String documentHash = sha256Hex(pdfBytes);

        // Persiste o PDF original no bucket como SERVICE_CONTRACT_TERMS, owner = manager.
        String storageName = String.format(Locale.ROOT, "contrato_original_%d_%s",
                Instant.now().toEpochMilli(), originalFileName);
        UUID sourceDocumentId = documentUseCase.uploadGeneratedDocument(
                DocumentType.SERVICE_CONTRACT_TERMS,
                manager.employeeId(),
                null,
                pdfBytes,
                storageName
        );

        Instant now = Instant.now();
        ServiceContract contract = new ServiceContract(
                UUID.randomUUID(),
                manager.companyId(),
                sourceDocumentId,
                manager.employeeId(),
                currentUserId,
                manager.employeeId(),
                title.trim(),
                description == null || description.isBlank() ? null : description.trim(),
                originalFileName,
                documentHash,
                ServiceContractStatus.ACTIVE,
                now, null, null, null, null
        );
        ServiceContract saved = contractProvider.save(contract);

        List<UUID> assignmentIds = new ArrayList<>();
        for (UUID employeeId : employeeIds) {
            ServiceContractAssignment assignment = new ServiceContractAssignment(
                    UUID.randomUUID(),
                    saved.contractId(),
                    manager.companyId(),
                    employeeId,
                    currentUserId,
                    ServiceContractAssignmentStatus.PENDING,
                    now,
                    null,
                    null
            );
            ServiceContractAssignment persisted = assignmentProvider.save(assignment);
            assignmentIds.add(persisted.assignmentId());
        }

        auditService.registerSecurity(
                AuditAction.SERVICE_CONTRACT_CREATED,
                currentUserId,
                null,
                "MEDIUM",
                "SERVICE_CONTRACT",
                saved.contractId().toString(),
                String.format("title=%s,assignments=%d", saved.title(), assignmentIds.size()),
                ipAddress,
                userAgent
        );
        log.info("event=service_contract_created result=success contract_id={} assignments={}",
                saved.contractId(), assignmentIds.size());

        return new CreateServiceContractResponse(
                saved.contractId(),
                saved.title(),
                saved.originalFileName(),
                saved.documentHashSha256(),
                saved.createdAt(),
                assignmentIds
        );
    }

    // ==================== ADMIN LIST ====================

    @Override
    @Transactional(readOnly = true)
    public ServiceContractAdminPageResponse findAdmin(ServiceContractStatus status, int page, int size) {
        Employee manager = getAuthenticatedEmployee();
        Role role = jwtAuthenticatedUser.getCurrentRole();
        if (role != Role.MANAGER && role != Role.CTO) {
            throw new ForbiddenException("Acesso negado.");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ServiceContract> contracts = contractProvider.findByCompanyFiltered(pageable, manager.companyId(), status);
        List<ServiceContractAdminItemResponse> items = contracts.getContent().stream()
                .map(this::toAdminItem)
                .toList();
        return new ServiceContractAdminPageResponse(
                items,
                contracts.getNumber(),
                contracts.getSize(),
                contracts.getTotalElements(),
                contracts.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceContractAdminItemResponse findAdminDetail(UUID contractId) {
        Employee manager = getAuthenticatedEmployee();
        Role role = jwtAuthenticatedUser.getCurrentRole();
        if (role != Role.MANAGER && role != Role.CTO) {
            throw new ForbiddenException("Acesso negado.");
        }
        ServiceContract contract = contractProvider.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado."));
        if (!contract.companyId().equals(manager.companyId())) {
            throw new ForbiddenException("Acesso negado.");
        }
        return toAdminItem(contract);
    }

    private ServiceContractAdminItemResponse toAdminItem(ServiceContract contract) {
        List<ServiceContractAssignment> all = assignmentProvider.findByContractId(contract.contractId());
        int total = all.size();
        int signed = (int) all.stream().filter(a -> a.status() == ServiceContractAssignmentStatus.SIGNED).count();
        int pending = (int) all.stream().filter(a -> a.status() == ServiceContractAssignmentStatus.PENDING).count();
        int cancelled = (int) all.stream().filter(a -> a.status() == ServiceContractAssignmentStatus.CANCELLED).count();
        return new ServiceContractAdminItemResponse(
                contract.contractId(),
                contract.title(),
                contract.originalFileName(),
                contract.status().name(),
                contract.createdAt(),
                total, signed, pending, cancelled
        );
    }

    // ==================== PENDENCIES ====================

    @Override
    @Transactional(readOnly = true)
    public PendingServiceContractListResponse findPendingForCurrentEmployee() {
        Employee employee = getAuthenticatedEmployee();
        List<ServiceContractAssignment> pendings = assignmentProvider.findByEmployeeAndStatus(
                employee.employeeId(),
                ServiceContractAssignmentStatus.PENDING
        );
        List<PendingServiceContractResponse> items = new ArrayList<>();
        for (ServiceContractAssignment a : pendings) {
            Optional<ServiceContract> contractOpt = contractProvider.findById(a.contractId());
            if (contractOpt.isEmpty() || contractOpt.get().status() != ServiceContractStatus.ACTIVE) {
                continue;
            }
            if (signatureProvider.findActiveByAssignment(a.assignmentId()).isPresent()) {
                continue;
            }
            ServiceContract contract = contractOpt.get();
            String declarationText = buildDeclarationText(contract.title());
            String declarationHash = sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
            items.add(new PendingServiceContractResponse(
                    contract.contractId(),
                    a.assignmentId(),
                    contract.title(),
                    contract.description(),
                    contract.originalFileName(),
                    contract.documentHashSha256(),
                    a.assignedAt(),
                    DECLARATION_VERSION_V1,
                    declarationText,
                    declarationHash
            ));
        }
        return new PendingServiceContractListResponse(items);
    }

    // ==================== PREVIEW ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] preview(UUID contractId) {
        Employee employee = getAuthenticatedEmployee();
        UUID currentUserId = jwtAuthenticatedUser.getuserId();
        ServiceContract contract = contractProvider.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado."));
        if (contract.status() != ServiceContractStatus.ACTIVE) {
            throw new ConflictException("Contrato não está ativo.");
        }

        Role role = jwtAuthenticatedUser.getCurrentRole();
        boolean isAdmin = (role == Role.MANAGER || role == Role.CTO)
                && contract.companyId().equals(employee.companyId());
        boolean isAssignedEmployee = assignmentProvider
                .findByContractAndEmployee(contractId, employee.employeeId())
                .isPresent();

        if (!isAdmin && !isAssignedEmployee) {
            throw new ForbiddenException("Acesso negado ao contrato.");
        }

        byte[] bytes = fetchOriginalPdfBytes(contract);

        auditService.registerSecurity(
                AuditAction.SERVICE_CONTRACT_VIEWED,
                currentUserId,
                employee.employeeId(),
                "LOW",
                "SERVICE_CONTRACT",
                contractId.toString(),
                "source=preview",
                (String) null,
                null
        );
        return bytes;
    }

    private byte[] fetchOriginalPdfBytes(ServiceContract contract) {
        Document doc = documentProvider.findById(contract.sourceDocumentId());
        if (doc == null || doc.storagePath() == null || doc.storagePath().isBlank()) {
            throw new ResourceNotFoundException("Documento original do contrato não encontrado.");
        }
        return bucketStorageProvider.downloadFile(doc.type(), doc.storagePath());
    }

    // ==================== SIGN ====================

    @Override
    @Transactional
    public SignServiceContractResponse sign(
            UUID contractId,
            SignServiceContractRequest request,
            String ipAddress,
            String userAgent
    ) {
        if (!request.confirmed()) {
            throw new BadRequestException("Confirmação explícita da declaração é obrigatória.");
        }
        Employee employee = getAuthenticatedEmployee();
        UUID currentUserId = jwtAuthenticatedUser.getuserId();

        ServiceContract contract = contractProvider.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado."));
        if (contract.status() != ServiceContractStatus.ACTIVE) {
            throw new ConflictException("Contrato não está ativo.");
        }
        if (!contract.companyId().equals(employee.companyId())) {
            throw new ForbiddenException("Contrato pertence a outro tenant.");
        }

        ServiceContractAssignment assignment = assignmentProvider
                .findByContractAndEmployee(contractId, employee.employeeId())
                .orElseThrow(() -> new ForbiddenException("Você não está atribuído a este contrato."));
        if (assignment.status() == ServiceContractAssignmentStatus.CANCELLED) {
            throw new ConflictException("Atribuição cancelada.");
        }
        if (assignment.status() == ServiceContractAssignmentStatus.SIGNED
                || signatureProvider.findActiveByAssignment(assignment.assignmentId()).isPresent()) {
            log.warn("event=service_contract_sign_block reason=already_signed contract_id={} employee_ref={}",
                    contractId, employee.employeeId());
            throw new ConflictException("Você já assinou este contrato.");
        }

        if (!contract.documentHashSha256().equalsIgnoreCase(request.contractDocumentHashSha256())) {
            throw new ConflictException("O contrato exibido mudou após a visualização. Recarregue e tente novamente.");
        }

        String declarationText = buildDeclarationText(contract.title());
        String declarationHash = sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        if (!DECLARATION_VERSION_V1.equals(request.declarationVersion())
                || !declarationHash.equalsIgnoreCase(request.declarationHashSha256())) {
            throw new BadRequestException("A declaração informada está desatualizada. Recarregue a tela.");
        }

        biometricProtectionService.protectContractSigning(employee.employeeId(), request.faceImageBase64());

        UUID recognizedEmployeeId;
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.faceImageBase64());
            recognizedEmployeeId = faceRecognitionProvider.searchFaceByImage(new ByteArrayInputStream(imageBytes));
        } catch (IllegalArgumentException ex) {
            auditService.registerSecurity(
                    AuditAction.SERVICE_CONTRACT_FACIAL_AUTH_FAILED,
                    currentUserId,
                    employee.employeeId(),
                    "HIGH",
                    "SERVICE_CONTRACT",
                    contractId.toString(),
                    "reason=invalid_base64",
                    ipAddress,
                    userAgent
            );
            throw new ForbiddenException("Imagem biométrica inválida.");
        }

        if (!employee.employeeId().equals(recognizedEmployeeId)) {
            auditService.registerSecurity(
                    AuditAction.SERVICE_CONTRACT_FACIAL_AUTH_FAILED,
                    currentUserId,
                    employee.employeeId(),
                    "HIGH",
                    "SERVICE_CONTRACT",
                    contractId.toString(),
                    null,
                    ipAddress,
                    userAgent
            );
            throw new ForbiddenException("Reconhecimento facial não confirmado.");
        }

        Instant now = Instant.now();
        UUID signatureId = UUID.randomUUID();

        // 1) Hash canônico de evidências — JSON ordenado alfabeticamente. Calculado
        //    ANTES de gerar/assinar o PDF para que possa ser exibido no carimbo.
        String canonicalEvidenceJson = buildCanonicalEvidenceJson(
                signatureId, contract, assignment, employee, currentUserId,
                now, ipAddress, userAgent, declarationHash
        );
        String canonicalEvidenceHash = sha256Hex(canonicalEvidenceJson.getBytes(StandardCharsets.UTF_8));

        // 2) Registra a evidência de auditoria PRIMEIRO para obter o auditLogId
        //    e amarrá-lo à assinatura (rastreabilidade bidirecional).
        UUID auditLogId = auditService.registerSecurityReturningId(
                AuditAction.SERVICE_CONTRACT_SIGNED,
                currentUserId,
                employee.employeeId(),
                "HIGH",
                "SERVICE_CONTRACT",
                signatureId.toString(),
                String.format("contract_id=%s,assignment_id=%s,canonical_evidence_hash=%s",
                        contract.contractId(), assignment.assignmentId(), canonicalEvidenceHash),
                ipAddress,
                userAgent
        );

        // 3) Baixa o PDF original
        byte[] originalPdf = fetchOriginalPdfBytes(contract);

        // 4) Aplica marca d'água de evidência (overlay transparente em cada página).
        byte[] stampedPdf = evidenceWatermarkService.applyEvidenceWatermark(
                originalPdf,
                new EvidenceWatermarkService.EvidenceStamp(
                        employee.fullName(),
                        now,
                        DECLARATION_VERSION_V1,
                        canonicalEvidenceHash
                )
        );

        // 5) Assina com o certificado da empresa (PAdES).
        String padesStatus;
        byte[] companySignedPdf;
        try {
            companySignedPdf = digitalSignatureService.signPdf(
                    stampedPdf,
                    "Ciência de contrato — " + employee.fullName() + " — " + contract.title(),
                    "Kronos — assinatura eletrônica interna"
            );
            padesStatus = "SUCCESS";
        } catch (RuntimeException ex) {
            log.warn("event=service_contract_pades_failed contract_id={} exception_type={}",
                    contract.contractId(), ex.getClass().getSimpleName());
            throw ex;
        }

        // 6) Persiste como SERVICE_CONTRACT_TERMS, owner = colaborador signatário
        String fileName = String.format(Locale.ROOT,
                "contrato_assinado_%s_%s.pdf",
                contract.contractId(),
                employee.employeeId()
        );
        UUID signedDocumentId = documentUseCase.uploadGeneratedDocument(
                DocumentType.SERVICE_CONTRACT_TERMS,
                employee.employeeId(),
                null,
                companySignedPdf,
                fileName
        );

        String signedPdfHash = sha256Hex(companySignedPdf);

        ServiceContractSignature signature = new ServiceContractSignature(
                signatureId,
                assignment.assignmentId(),
                contract.contractId(),
                employee.employeeId(),
                employee.companyId(),
                currentUserId,
                now,
                CONTRACT_ZONE.getId(),
                ContractSignatureType.INTERNAL_ADVANCED,
                ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                signedDocumentId,
                contract.documentHashSha256(),
                signedPdfHash,
                DECLARATION_VERSION_V1,
                declarationHash,
                declarationText,
                ipAddress,
                userAgent,
                canonicalEvidenceJson,
                now,
                null, null, null, null,
                "SERVICE_CONTRACT",
                DECLARATION_VERSION_V1,
                canonicalEvidenceHash,
                auditLogId,
                padesStatus
        );
        ServiceContractSignature savedSignature = signatureProvider.save(signature);

        ServiceContractAssignment updated = assignment
                .withStatus(ServiceContractAssignmentStatus.SIGNED)
                .withSignedAt(now);
        assignmentProvider.save(updated);
        log.info("event=service_contract_signed result=success contract_id={} signature_id={}",
                contract.contractId(), savedSignature.signatureId());

        return new SignServiceContractResponse(
                savedSignature.signatureId(),
                savedSignature.assignmentId(),
                savedSignature.contractId(),
                savedSignature.signedAt(),
                savedSignature.signatureType().name(),
                savedSignature.signatureMethod().name(),
                savedSignature.contractDocumentHashSha256(),
                savedSignature.signedPdfHashSha256(),
                savedSignature.signedDocumentId(),
                savedSignature.declarationVersion()
        );
    }

    // ==================== DOWNLOAD SIGNED ====================

    @Override
    @Transactional(readOnly = true)
    public SignedDocumentDownload downloadSignatureDocument(UUID signatureId) {
        Employee authenticated = getAuthenticatedEmployee();
        UUID currentUserId = jwtAuthenticatedUser.getuserId();
        ServiceContractSignature signature = signatureProvider.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada."));

        Role role = jwtAuthenticatedUser.getCurrentRole();
        boolean isSelf = signature.employeeId().equals(authenticated.employeeId());
        boolean isAdmin = (role == Role.MANAGER || role == Role.CTO)
                && signature.companyId().equals(authenticated.companyId());
        if (!isSelf && !isAdmin) {
            throw new ForbiddenException("Acesso negado ao documento de assinatura.");
        }

        if (signature.signedDocumentId() == null) {
            throw new ResourceNotFoundException("Documento assinado indisponível.");
        }
        Document doc = documentProvider.findById(signature.signedDocumentId());
        if (doc == null || doc.storagePath() == null || doc.storagePath().isBlank()) {
            throw new ResourceNotFoundException("Documento assinado não encontrado no storage.");
        }
        byte[] bytes = bucketStorageProvider.downloadFile(doc.type(), doc.storagePath());

        auditService.registerSecurity(
                AuditAction.SERVICE_CONTRACT_SIGNATURE_VIEWED,
                currentUserId,
                signature.employeeId(),
                "LOW",
                "SERVICE_CONTRACT_SIGNATURE",
                signature.signatureId().toString(),
                String.format("contract_id=%s", signature.contractId()),
                (String) null,
                null
        );

        String fileName = String.format(Locale.ROOT,
                "contrato_assinado_%s.pdf",
                signature.contractId()
        );
        return new SignedDocumentDownload(bytes, fileName, "application/pdf");
    }

    // ==================== ADMIN SIGNATURES ====================

    @Override
    @Transactional(readOnly = true)
    public ServiceContractSignatureAdminPageResponse findAdminSignatures(
            UUID contractId,
            ContractSignatureStatus status,
            int page,
            int size
    ) {
        Employee manager = getAuthenticatedEmployee();
        Role role = jwtAuthenticatedUser.getCurrentRole();
        if (role != Role.MANAGER && role != Role.CTO) {
            throw new ForbiddenException("Acesso negado.");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "signedAt")
        );

        Page<ServiceContractSignature> signatures = signatureProvider.findAdminFiltered(
                pageable, manager.companyId(), contractId, status
        );

        Map<UUID, String> employeeNames = employeeProvider
                .findByCompanyId(manager.companyId()).stream()
                .collect(Collectors.toMap(Employee::employeeId, e -> Optional.ofNullable(e.fullName()).orElse("")));

        List<ServiceContractSignatureAdminItemResponse> items = signatures.getContent().stream()
                .map(s -> new ServiceContractSignatureAdminItemResponse(
                        s.signatureId(),
                        s.assignmentId(),
                        s.contractId(),
                        s.employeeId(),
                        employeeNames.getOrDefault(s.employeeId(), ""),
                        s.signedAt(),
                        s.status().name(),
                        s.signatureType().name(),
                        s.signatureMethod().name(),
                        s.signedPdfHashSha256()
                ))
                .toList();

        return new ServiceContractSignatureAdminPageResponse(
                items,
                signatures.getNumber(),
                signatures.getSize(),
                signatures.getTotalElements(),
                signatures.getTotalPages()
        );
    }

    // ==================== helpers ====================

    private Employee getAuthenticatedEmployee() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        return employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador autenticado não encontrado."));
    }

    private String buildDeclarationText(String contractTitle) {
        return String.format(DECLARATION_TEMPLATE_V1, contractTitle);
    }

    /**
     * Serializa as evidências de assinatura em um JSON CANÔNICO (chaves ordenadas
     * alfabeticamente, sem espaços) cujo SHA-256 vira o
     * {@code canonical_evidence_hash_sha256}. Esse hash permite verificar a
     * integridade da própria evidência (independente do PDF e do audit log).
     */
    private String buildCanonicalEvidenceJson(
            UUID signatureId,
            ServiceContract contract,
            ServiceContractAssignment assignment,
            Employee employee,
            UUID currentUserId,
            Instant signedAt,
            String ipAddress,
            String userAgent,
            String declarationHash
    ) {
        // Construção determinística — chaves em ordem alfabética; valores escaped via
        // jsonEscape; null vira `null`. Não usar ObjectMapper para evitar variações
        // de plugin/configuração que afetem o hash entre versões.
        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        appendJson(sb, "assignmentId", assignment.assignmentId().toString()); sb.append(',');
        appendJson(sb, "authMethod", "FACIAL_RECOGNITION"); sb.append(',');
        appendJson(sb, "companyId", employee.companyId().toString()); sb.append(',');
        appendJson(sb, "contractId", contract.contractId().toString()); sb.append(',');
        appendJson(sb, "declarationHashSha256", declarationHash); sb.append(',');
        appendJson(sb, "declarationVersion", DECLARATION_VERSION_V1); sb.append(',');
        appendJson(sb, "documentHashSha256", contract.documentHashSha256()); sb.append(',');
        appendJson(sb, "documentType", "SERVICE_CONTRACT"); sb.append(',');
        appendJson(sb, "documentVersion", DECLARATION_VERSION_V1); sb.append(',');
        appendJson(sb, "employeeId", employee.employeeId().toString()); sb.append(',');
        appendJson(sb, "faceMatchThreshold", "90.0"); sb.append(',');
        appendJson(sb, "facialVerificationStatus", "APPROVED"); sb.append(',');
        appendJson(sb, "hashAlgorithm", "SHA-256"); sb.append(',');
        appendJson(sb, "ipAddress", ipAddress); sb.append(',');
        appendJson(sb, "livenessEnabled", "false"); sb.append(',');
        appendJson(sb, "livenessStatus", "DISABLED"); sb.append(',');
        appendJson(sb, "signatureId", signatureId.toString()); sb.append(',');
        appendJson(sb, "signatureType", "INTERNAL_ADVANCED"); sb.append(',');
        appendJson(sb, "signedAt", signedAt.toString()); sb.append(',');
        appendJson(sb, "timezone", CONTRACT_ZONE.getId()); sb.append(',');
        appendJson(sb, "userAgent", userAgent); sb.append(',');
        appendJson(sb, "userId", currentUserId.toString());
        sb.append('}');
        return sb.toString();
    }

    private static void appendJson(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(jsonEscape(value)).append('"');
        }
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String sanitizeFileName(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String clean = raw.replaceAll("[^a-zA-Z0-9._-]", "_");
        return clean.isBlank() ? fallback : clean;
    }

    static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }
}
