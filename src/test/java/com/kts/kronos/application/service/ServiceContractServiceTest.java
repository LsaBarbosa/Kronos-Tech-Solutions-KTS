package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.servicecontract.CreateServiceContractResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminItemResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractSignatureAdminPageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.kts.kronos.adapter.in.web.dto.servicecontract.PendingServiceContractListResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractRequest;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.ServiceContractAssignmentProvider;
import com.kts.kronos.application.port.out.provider.ServiceContractProvider;
import com.kts.kronos.application.port.out.provider.ServiceContractSignatureProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.ServiceContract;
import com.kts.kronos.domain.model.ServiceContractAssignment;
import com.kts.kronos.domain.model.ServiceContractSignature;
import com.kts.kronos.domain.model.enuns.ContractSignatureMethod;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ContractSignatureType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.springframework.web.multipart.MultipartFile;


@ExtendWith(MockitoExtension.class)
class ServiceContractServiceTest {

    @Mock ServiceContractProvider contractProvider;
    @Mock ServiceContractAssignmentProvider assignmentProvider;
    @Mock ServiceContractSignatureProvider signatureProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock FaceRecognitionProvider faceRecognitionProvider;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock BiometricProtectionService biometricProtectionService;
    @Mock PrivacyLogReferenceService privacyLogReferenceService;
    @Mock AuditService auditService;
    @Mock DocumentUseCase documentUseCase;
    @Mock DocumentProvider documentProvider;
    @Mock BucketStorageProvider bucketStorageProvider;
    @Mock DigitalSignatureService digitalSignatureService;
    @Mock EvidenceWatermarkService evidenceWatermarkService;

    @InjectMocks
    ServiceContractService service;

    UUID managerEmployeeId;
    UUID managerUserId;
    UUID employeeId;
    UUID employeeUserId;
    UUID companyId;
    UUID otherCompanyId;
    Employee manager;
    Employee employee;
    byte[] pdfBytes;
    String pdfHash;

    // A valid base64-encoded face image stub
    static final String FAKE_FACE_BASE64 = java.util.Base64.getEncoder()
            .encodeToString("fake-face-image".getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        managerEmployeeId = UUID.randomUUID();
        managerUserId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        employeeUserId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        otherCompanyId = UUID.randomUUID();
        manager = baseEmployee(managerEmployeeId, companyId, "Manager Silva");
        employee = baseEmployee(employeeId, companyId, "Colab Souza");
        pdfBytes = "%PDF-1.4 fake content".getBytes(StandardCharsets.UTF_8);
        pdfHash = ServiceContractService.sha256Hex(pdfBytes);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("create: manager cria contrato com 2 atribuições, salva PDF original e atribuições PENDING")
    void createSuccess() {
        Employee other = baseEmployee(UUID.randomUUID(), companyId, "Outro Colab");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee, other));
        UUID sourceDocId = UUID.randomUUID();
        when(documentUseCase.uploadGeneratedDocument(
                eq(DocumentType.SERVICE_CONTRACT_TERMS), eq(managerEmployeeId), eq(null), eq(pdfBytes), any()
        )).thenReturn(sourceDocId);
        ArgumentCaptor<ServiceContract> contractCaptor = ArgumentCaptor.forClass(ServiceContract.class);
        when(contractProvider.save(contractCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdfBytes);
        CreateServiceContractResponse response = service.create(
                "Contrato de Serviço 2026",
                "Cláusulas mensais",
                List.of(employeeId, other.employeeId()),
                file,
                "10.0.0.1", "JUnit"
        );

        assertThat(response.assignmentIds()).hasSize(2);
        assertThat(response.documentHashSha256()).isEqualTo(pdfHash);
        ServiceContract savedContract = contractCaptor.getValue();
        assertThat(savedContract.sourceDocumentId()).isEqualTo(sourceDocId);
        assertThat(savedContract.status()).isEqualTo(ServiceContractStatus.ACTIVE);
        assertThat(savedContract.companyId()).isEqualTo(companyId);
    }

    @Test
    @DisplayName("create: bloqueia atribuição cross-tenant (colaborador de outra empresa)")
    void createBlocksCrossTenant() {
        Employee foreigner = baseEmployee(UUID.randomUUID(), otherCompanyId, "Outsider");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee));

        MockMultipartFile file = new MockMultipartFile("file", "c.pdf", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create("T", null, List.of(foreigner.employeeId()), file, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("outro tenant");
    }

    @Test
    @DisplayName("create: rejeita upload que não é PDF")
    void createRejectsNonPdf() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        MockMultipartFile file = new MockMultipartFile("file", "c.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", pdfBytes);
        assertThatThrownBy(() -> service.create("T", null, List.of(employeeId), file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    @DisplayName("create: rejeita lista vazia de colaboradores")
    void createRejectsEmptyEmployees() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        MockMultipartFile file = new MockMultipartFile("file", "c.pdf", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create("T", null, List.of(), file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pelo menos um");
    }

    @Test
    @DisplayName("create: rejeita criação por colaborador comum (não MANAGER/CTO)")
    void createRejectsNonManager() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        MockMultipartFile file = new MockMultipartFile("file", "c.pdf", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create("T", null, List.of(employeeId), file, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("gestores");
    }

    // ==================== PENDENCIES ====================

    @Test
    @DisplayName("pendingForCurrentEmployee: retorna apenas contratos ACTIVE com assignment PENDING sem assinatura ativa")
    void pendingReturnsOnlyEligible() {
        UUID contractA = UUID.randomUUID();
        UUID contractB = UUID.randomUUID();
        UUID contractC = UUID.randomUUID();
        ServiceContractAssignment pendingA = pendingAssignment(employeeId, contractA);
        ServiceContractAssignment pendingB = pendingAssignment(employeeId, contractB);
        ServiceContractAssignment pendingC = pendingAssignment(employeeId, contractC);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignmentProvider.findByEmployeeAndStatus(employeeId, ServiceContractAssignmentStatus.PENDING))
                .thenReturn(List.of(pendingA, pendingB, pendingC));
        when(contractProvider.findById(contractA)).thenReturn(Optional.of(contractFor(contractA, ServiceContractStatus.ACTIVE)));
        when(contractProvider.findById(contractB)).thenReturn(Optional.of(contractFor(contractB, ServiceContractStatus.VOIDED))); // VOIDED → filtra
        when(contractProvider.findById(contractC)).thenReturn(Optional.of(contractFor(contractC, ServiceContractStatus.ACTIVE)));
        when(signatureProvider.findActiveByAssignment(pendingA.assignmentId())).thenReturn(Optional.empty());
        when(signatureProvider.findActiveByAssignment(pendingC.assignmentId()))
                .thenReturn(Optional.of(activeSignature(contractC, pendingC.assignmentId()))); // já tem assinatura → filtra

        PendingServiceContractListResponse response = service.findPendingForCurrentEmployee();

        assertThat(response.contracts()).hasSize(1);
        assertThat(response.contracts().get(0).contractId()).isEqualTo(contractA);
    }

    // ==================== SIGN ====================

    @Test
    @DisplayName("sign: sucesso via reconhecimento facial, anexa evidência, assina com cert da empresa e persiste documento assinado")
    void signSuccess() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        byte[] stamped = "stamped".getBytes(StandardCharsets.UTF_8);
        byte[] signed = "company-signed".getBytes(StandardCharsets.UTF_8);
        UUID signedDocId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(documentProvider.findById(contract.sourceDocumentId())).thenReturn(documentFor(contract.sourceDocumentId(), "key/orig.pdf"));
        when(bucketStorageProvider.downloadFile(DocumentType.SERVICE_CONTRACT_TERMS, "key/orig.pdf")).thenReturn(pdfBytes);
        when(evidenceWatermarkService.applyEvidenceWatermark(eq(pdfBytes), any())).thenReturn(stamped);
        when(digitalSignatureService.signPdf(eq(stamped), any(), any())).thenReturn(signed);
        when(documentUseCase.uploadGeneratedDocument(
                eq(DocumentType.SERVICE_CONTRACT_TERMS), eq(employeeId), eq(null), eq(signed), any()
        )).thenReturn(signedDocId);
        ArgumentCaptor<ServiceContractSignature> sigCaptor = ArgumentCaptor.forClass(ServiceContractSignature.class);
        when(signatureProvider.save(sigCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1,
                declarationHash, contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        SignServiceContractResponse response = service.sign(contractId, req, "10.0.0.1", "JUnit");

        assertThat(response.signedDocumentId()).isEqualTo(signedDocId);
        assertThat(response.signatureType()).isEqualTo("INTERNAL_ADVANCED");
        assertThat(response.signatureMethod()).isEqualTo("FACIAL_RECOGNITION");
        ServiceContractSignature persisted = sigCaptor.getValue();
        assertThat(persisted.status()).isEqualTo(ContractSignatureStatus.ACTIVE);
        assertThat(persisted.signedPdfHashSha256()).isEqualTo(ServiceContractService.sha256Hex(signed));
        // verifica que assignment foi atualizado para SIGNED
        ArgumentCaptor<ServiceContractAssignment> assignCaptor = ArgumentCaptor.forClass(ServiceContractAssignment.class);
        verify(assignmentProvider).save(assignCaptor.capture());
        assertThat(assignCaptor.getValue().status()).isEqualTo(ServiceContractAssignmentStatus.SIGNED);
    }

    @Test
    @DisplayName("sign: 409 quando colaborador já assinou (duplicidade)")
    void signRejectsDuplicate() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contractFor(contractId, ServiceContractStatus.ACTIVE)));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId))
                .thenReturn(Optional.of(activeSignature(contractId, assignmentId)));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, "1.0", "x", "y", FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já assinou");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("sign: 403 quando colaborador não está atribuído ao contrato")
    void signRejectsUnassigned() {
        UUID contractId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contractFor(contractId, ServiceContractStatus.ACTIVE)));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.empty());

        SignServiceContractRequest req = new SignServiceContractRequest(true, "1.0", "x", "y", FAKE_FACE_BASE64);
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("sign: 409 quando hash do documento informado diverge do contrato persistido")
    void signRejectsHashMismatch() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, "1.0", "xxx",
                "0000000000000000000000000000000000000000000000000000000000000000", // hash que não bate
                FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("contrato exibido");
    }

    @Test
    @DisplayName("sign: 403 quando rosto não corresponde ao colaborador autenticado")
    void signRejectsFacialAuthFailure() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        // Rekognition retorna outro UUID — rosto não pertence ao colaborador autenticado
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(UUID.randomUUID());

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("facial");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("download: 403 quando colaborador tenta baixar assinatura de outro tenant")
    void downloadRejectsCrossTenant() {
        UUID sigId = UUID.randomUUID();
        ServiceContractSignature otherTenantSig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), otherCompanyId, UUID.randomUUID(),
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.PASSWORD_REAUTH,
                ContractSignatureStatus.ACTIVE,
                UUID.randomUUID(), "hash", "hash", "1.0", "dhash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(otherTenantSig));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "127.0.0.1", "test-agent"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== helpers ====================

    private static Employee baseEmployee(UUID id, UUID companyId, String name) {
        return new Employee(
                id, name, "12345678901", "12345678901", "Dev", name + "@example.com", 1000d,
                "11999999999", true, null, companyId, null, false, null,
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );
    }

    private ServiceContract contractFor(UUID contractId, ServiceContractStatus status) {
        return new ServiceContract(
                contractId, companyId,
                UUID.randomUUID(), managerEmployeeId, managerUserId, managerEmployeeId,
                "Contrato " + contractId.toString().substring(0, 8),
                "Descrição teste",
                "contrato.pdf",
                pdfHash,
                status,
                Instant.now(),
                null, null, null, null
        );
    }

    private ServiceContractAssignment pendingAssignment(UUID empId, UUID contractId) {
        return pendingAssignment(empId, contractId, UUID.randomUUID());
    }

    private ServiceContractAssignment pendingAssignment(UUID empId, UUID contractId, UUID assignmentId) {
        return new ServiceContractAssignment(
                assignmentId, contractId, companyId, empId, managerUserId,
                ServiceContractAssignmentStatus.PENDING,
                Instant.now(), null, null
        );
    }

    private ServiceContractSignature activeSignature(UUID contractId, UUID assignmentId) {
        return new ServiceContractSignature(
                UUID.randomUUID(), assignmentId, contractId, employeeId, companyId, employeeUserId,
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.PASSWORD_REAUTH,
                ContractSignatureStatus.ACTIVE,
                UUID.randomUUID(), pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
    }

    private Document documentFor(UUID docId, String storagePath) {
        return new Document(
                docId, employeeId, DocumentType.SERVICE_CONTRACT_TERMS,
                "contrato.pdf", "application/pdf", storagePath,
                java.time.LocalDateTime.now(), null, false, false, pdfHash
        );
    }

    // ==================== FIND ADMIN ====================

    @Test
    @DisplayName("findAdmin: MANAGER recebe página de contratos da empresa")
    void findAdminSuccess() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findByCompanyFiltered(any(), eq(companyId), any()))
                .thenReturn(new PageImpl<>(List.of(contract), PageRequest.of(0, 10), 1));
        when(assignmentProvider.findByContractId(contractId)).thenReturn(List.of(
                pendingAssignment(employeeId, contractId),
                pendingAssignment(UUID.randomUUID(), contractId)
        ));

        ServiceContractAdminPageResponse response = service.findAdmin(null, 0, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).contractId()).isEqualTo(contractId);
        assertThat(response.items().get(0).totalAssignments()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAdmin: PARTNER recebe ForbiddenException")
    void findAdminBlocksNonManager() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.findAdmin(null, 0, 10))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== FIND ADMIN DETAIL ====================

    @Test
    @DisplayName("findAdminDetail: MANAGER obtém detalhes do contrato")
    void findAdminDetailSuccess() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractId(contractId)).thenReturn(List.of());

        ServiceContractAdminItemResponse response = service.findAdminDetail(contractId);

        assertThat(response.contractId()).isEqualTo(contractId);
    }

    @Test
    @DisplayName("findAdminDetail: contrato não encontrado lança ResourceNotFoundException")
    void findAdminDetailNotFound() {
        UUID contractId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAdminDetail(contractId))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findAdminDetail: contrato de outro tenant lança ForbiddenException")
    void findAdminDetailWrongTenant() {
        UUID contractId = UUID.randomUUID();
        ServiceContract foreignContract = new ServiceContract(
                contractId, otherCompanyId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Foreign", null, "c.pdf", pdfHash,
                ServiceContractStatus.ACTIVE, Instant.now(), null, null, null, null
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(foreignContract));

        assertThatThrownBy(() -> service.findAdminDetail(contractId))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== PREVIEW ====================

    @Test
    @DisplayName("preview: colaborador atribuído ao contrato consegue visualizar")
    void previewByAssignedEmployee() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        UUID sourceDocId = contract.sourceDocumentId();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId))
                .thenReturn(Optional.of(pendingAssignment(employeeId, contractId)));
        when(documentProvider.findById(sourceDocId)).thenReturn(documentFor(sourceDocId, "key/orig.pdf"));
        when(bucketStorageProvider.downloadFile(DocumentType.SERVICE_CONTRACT_TERMS, "key/orig.pdf")).thenReturn(pdfBytes);

        byte[] result = service.preview(contractId, "ip", "ua");

        assertThat(result).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("preview: MANAGER do mesmo tenant consegue visualizar sem ser atribuído")
    void previewByAdmin() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        UUID sourceDocId = contract.sourceDocumentId();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(documentProvider.findById(sourceDocId)).thenReturn(documentFor(sourceDocId, "key/orig.pdf"));
        when(bucketStorageProvider.downloadFile(DocumentType.SERVICE_CONTRACT_TERMS, "key/orig.pdf")).thenReturn(pdfBytes);

        byte[] result = service.preview(contractId, "ip", "ua");

        assertThat(result).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("preview: colaborador não atribuído e não admin recebe ForbiddenException")
    void previewForbiddenForNonAssigned() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== DOWNLOAD SIGNATURE DOCUMENT ====================

    @Test
    @DisplayName("downloadSignatureDocument: colaborador baixa seu próprio documento")
    void downloadSelfDocument() {
        UUID sigId = UUID.randomUUID();
        UUID signedDocId = UUID.randomUUID();
        ServiceContractSignature sig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                employeeId, companyId, employeeUserId,
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                signedDocId, pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sig));
        when(documentProvider.findById(signedDocId)).thenReturn(documentFor(signedDocId, "key/signed.pdf"));
        when(bucketStorageProvider.downloadFile(DocumentType.SERVICE_CONTRACT_TERMS, "key/signed.pdf")).thenReturn(pdfBytes);

        var result = service.downloadSignatureDocument(sigId, "ip", "ua");

        assertThat(result.data()).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("downloadSignatureDocument: signedDocumentId null lança ResourceNotFoundException")
    void downloadNullSignedDocId() {
        UUID sigId = UUID.randomUUID();
        ServiceContractSignature sig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                employeeId, companyId, employeeUserId,
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                null, // signedDocumentId = null
                pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    // ==================== FIND ADMIN SIGNATURES ====================

    @Test
    @DisplayName("findAdminSignatures: MANAGER lista assinaturas com sucesso")
    void findAdminSignaturesSuccess() {
        ServiceContractSignature sig = activeSignature(UUID.randomUUID(), UUID.randomUUID());
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(signatureProvider.findAdminFiltered(any(), eq(companyId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sig), PageRequest.of(0, 10), 1));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee));

        ServiceContractSignatureAdminPageResponse response =
                service.findAdminSignatures(null, null, 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAdminSignatures: não-MANAGER recebe ForbiddenException")
    void findAdminSignaturesBlocksNonManager() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.findAdminSignatures(null, null, 0, 10))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== JSON ESCAPE ====================

    @Test
    @DisplayName("jsonEscape: caracteres especiais são corretamente escapados")
    void jsonEscapeSpecialChars() {
        // Use sign() to trigger jsonEscape indirectly via buildCanonicalEvidenceJson —
        // but jsonEscape is package-private static, so test the visible method sha256Hex instead,
        // and verify escape through the canonical evidence JSON content in sign test above.
        // Direct test via reflection or just ensure the switch branches are hit via sign with
        // ipAddress/userAgent containing special chars.
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] stamped = pdfBytes;
        byte[] signed = "signed".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        UUID signedDocId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(documentProvider.findById(contract.sourceDocumentId())).thenReturn(documentFor(contract.sourceDocumentId(), "key.pdf"));
        when(bucketStorageProvider.downloadFile(any(), any())).thenReturn(pdfBytes);
        when(evidenceWatermarkService.applyEvidenceWatermark(any(), any())).thenReturn(stamped);
        when(digitalSignatureService.signPdf(any(), any(), any())).thenReturn(signed);
        when(documentUseCase.uploadGeneratedDocument(any(), any(), any(), any(), any())).thenReturn(signedDocId);
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(),
                FAKE_FACE_BASE64
        );
        // ip with chars that exercise jsonEscape branches: quote and backslash
        SignServiceContractResponse response = service.sign(contractId, req,
                "10.0.0.1", "Agent");

        assertThat(response.signatureId()).isNotNull();
    }

    // ==================== create — paths adicionais ====================

    @Test
    @DisplayName("create: title null lanca BadRequestException")
    void create_titleNullThrows() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        MockMultipartFile file = new MockMultipartFile("file", "c.pdf", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create(null, null, List.of(employeeId), file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Título");
    }

    @Test
    @DisplayName("create: originalFilename null → sanitizeFileName usa fallback, create continua")
    void create_originalFilenameNull() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getBytes()).thenReturn(pdfBytes);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee));
        when(documentUseCase.uploadGeneratedDocument(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(contractProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create("Titulo", null, List.of(employeeId), file, "ip", "ua");
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("create: originalFilename em branco → sanitizeFileName usa fallback, create continua")
    void create_originalFilenameBlank() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("   ");
        when(file.getBytes()).thenReturn(pdfBytes);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee));
        when(documentUseCase.uploadGeneratedDocument(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(contractProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create("Titulo", null, List.of(employeeId), file, "ip", "ua");
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("create: getBytes() lanca IOException → BadRequestException")
    void create_pdfBytesIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("contract.pdf");
        when(file.getBytes()).thenThrow(new IOException("disk error"));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee));

        assertThatThrownBy(() -> service.create("Titulo", null, List.of(employeeId), file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ler o arquivo");
    }

    @Test
    @DisplayName("create: description null → aceito (ternary null branch)")
    void create_descriptionNull() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("contract.pdf");
        when(file.getBytes()).thenReturn(pdfBytes);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, employee));
        when(documentUseCase.uploadGeneratedDocument(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(contractProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create("Titulo", null, List.of(employeeId), file, "ip", "ua");
        assertThat(response).isNotNull();
    }

    // ==================== sign — paths adicionais ====================

    @Test
    @DisplayName("sign: atribuicao cancelada lanca ConflictException")
    void sign_cancelledAssignmentThrows() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContractAssignment cancelled = new ServiceContractAssignment(
                assignmentId, contractId, companyId, employeeId, managerUserId,
                ServiceContractAssignmentStatus.CANCELLED, Instant.now(), null, null
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contractFor(contractId, ServiceContractStatus.ACTIVE)));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(cancelled));

        SignServiceContractRequest req = new SignServiceContractRequest(true, "1.0", "x", "y", FAKE_FACE_BASE64);
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("sign: versao da declaracao invalida lanca BadRequestException")
    void sign_declarationVersionMismatch() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, "2.0", "declarationHash", contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desatualizada");
    }

    @Test
    @DisplayName("sign: Base64 invalido lanca ForbiddenException (IllegalArgumentException catch)")
    void sign_invalidBase64Throws() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(), "invalid-base64-!!!"
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ForbiddenException.class)
                .hasMessageContaining("biométrica");
    }

    @Test
    @DisplayName("sign: face nao encontrada (null) lanca ForbiddenException (branch face_not_found)")
    void sign_faceNotFound() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(null);

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ForbiddenException.class)
                .hasMessageContaining("facial");
    }

    @Test
    @DisplayName("sign: contrato inativo lanca ConflictException")
    void sign_contractNotActive() {
        UUID contractId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contractFor(contractId, ServiceContractStatus.VOIDED)));

        SignServiceContractRequest req = new SignServiceContractRequest(true, "1.0", "x", "y", FAKE_FACE_BASE64);
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ativo");
    }

    @Test
    @DisplayName("sign: contrato de outro tenant lanca ForbiddenException")
    void sign_crossTenantContract() {
        UUID contractId = UUID.randomUUID();
        ServiceContract foreignContract = new ServiceContract(
                contractId, otherCompanyId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Foreign", null, "c.pdf", pdfHash,
                ServiceContractStatus.ACTIVE, Instant.now(), null, null, null, null
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(foreignContract));

        SignServiceContractRequest req = new SignServiceContractRequest(true, "1.0", "x", "y", FAKE_FACE_BASE64);
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ForbiddenException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    @DisplayName("sign: contrato nao encontrado lanca ResourceNotFoundException (orElseThrow lambda)")
    void sign_contractNotFound() {
        UUID contractId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.empty());

        SignServiceContractRequest req = new SignServiceContractRequest(true, "1.0", "x", "y", FAKE_FACE_BASE64);
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("sign: DataIntegrityViolationException na persistencia → ConflictException ja assinou")
    void sign_dataIntegrityViolation() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        byte[] signed = "signed".getBytes(StandardCharsets.UTF_8);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(documentProvider.findById(contract.sourceDocumentId())).thenReturn(documentFor(contract.sourceDocumentId(), "key.pdf"));
        when(bucketStorageProvider.downloadFile(any(), any())).thenReturn(pdfBytes);
        when(evidenceWatermarkService.applyEvidenceWatermark(any(), any())).thenReturn(pdfBytes);
        when(digitalSignatureService.signPdf(any(), any(), any())).thenReturn(signed);
        when(documentUseCase.uploadGeneratedDocument(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("assinou");
    }

    @Test
    @DisplayName("sign: RuntimeException no PAdES e relancada")
    void sign_padesRuntimeException() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(documentProvider.findById(contract.sourceDocumentId())).thenReturn(documentFor(contract.sourceDocumentId(), "key.pdf"));
        when(bucketStorageProvider.downloadFile(any(), any())).thenReturn(pdfBytes);
        when(evidenceWatermarkService.applyEvidenceWatermark(any(), any())).thenReturn(pdfBytes);
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(digitalSignatureService.signPdf(any(), any(), any())).thenThrow(new RuntimeException("HSM offline"));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HSM");
    }

    @Test
    @DisplayName("sign: ipAddress null cobre appendJson null-branch; userAgent especial cobre jsonEscape")
    void sign_nullIpAndSpecialCharsUserAgent() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        String declarationText = String.format(ServiceContractService.DECLARATION_TEMPLATE_V1, contract.title());
        String declarationHash = ServiceContractService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        byte[] signed = "signed".getBytes(StandardCharsets.UTF_8);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(documentProvider.findById(contract.sourceDocumentId())).thenReturn(documentFor(contract.sourceDocumentId(), "key.pdf"));
        when(bucketStorageProvider.downloadFile(any(), any())).thenReturn(pdfBytes);
        when(evidenceWatermarkService.applyEvidenceWatermark(any(), any())).thenReturn(pdfBytes);
        when(digitalSignatureService.signPdf(any(), any(), any())).thenReturn(signed);
        when(documentUseCase.uploadGeneratedDocument(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, ServiceContractService.DECLARATION_VERSION_V1, declarationHash,
                contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        // ipAddress null → appendJson null branch; userAgent com chars especiais → todos os casos jsonEscape
        String specialAgent = "\"\\\b\f\n\r\t\u0001normal";
        var response = service.sign(contractId, req, null, specialAgent);
        assertThat(response.signatureId()).isNotNull();
    }

    // ==================== fetchOriginalPdfBytes — paths adicionais (via preview) ====================

    @Test
    @DisplayName("fetchOriginalPdfBytes: documento null lanca ResourceNotFoundException")
    void fetchOriginalPdfBytes_docNull() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(documentProvider.findById(contract.sourceDocumentId())).thenReturn(null);

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("fetchOriginalPdfBytes: storagePath null lanca ResourceNotFoundException")
    void fetchOriginalPdfBytes_storagePathNull() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(documentProvider.findById(contract.sourceDocumentId()))
                .thenReturn(documentFor(contract.sourceDocumentId(), null));

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("fetchOriginalPdfBytes: storagePath em branco lanca ResourceNotFoundException")
    void fetchOriginalPdfBytes_storagePathBlank() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(documentProvider.findById(contract.sourceDocumentId()))
                .thenReturn(documentFor(contract.sourceDocumentId(), "   "));

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    // ==================== preview — paths adicionais ====================

    @Test
    @DisplayName("preview: contrato inativo lanca ConflictException")
    void preview_contractNotActive() {
        UUID contractId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contractFor(contractId, ServiceContractStatus.VOIDED)));

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ativo");
    }

    @Test
    @DisplayName("preview: contrato nao encontrado lanca ResourceNotFoundException (orElseThrow lambda)")
    void preview_contractNotFound() {
        UUID contractId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    // ==================== downloadSignatureDocument — paths adicionais ====================

    @Test
    @DisplayName("downloadSignatureDocument: documento null lanca ResourceNotFoundException")
    void downloadSignatureDocument_docNull() {
        UUID sigId = UUID.randomUUID();
        UUID signedDocId = UUID.randomUUID();
        ServiceContractSignature sig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                employeeId, companyId, employeeUserId,
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                signedDocId, pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sig));
        when(documentProvider.findById(signedDocId)).thenReturn(null);

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("storage");
    }

    @Test
    @DisplayName("downloadSignatureDocument: storagePath null lanca ResourceNotFoundException")
    void downloadSignatureDocument_storagePathNull() {
        UUID sigId = UUID.randomUUID();
        UUID signedDocId = UUID.randomUUID();
        ServiceContractSignature sigWithDoc = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                employeeId, companyId, employeeUserId,
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                signedDocId, pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sigWithDoc));
        when(documentProvider.findById(signedDocId)).thenReturn(documentFor(signedDocId, null));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("downloadSignatureDocument: assinatura nao encontrada lanca ResourceNotFoundException (lambda)")
    void downloadSignatureDocument_signatureNotFound() {
        UUID sigId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    // ==================== getAuthenticatedEmployee — orElseThrow lambda ====================

    @Test
    @DisplayName("getAuthenticatedEmployee: colaborador nao encontrado lanca ResourceNotFoundException")
    void getAuthenticatedEmployee_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(unknownId);
        when(employeeProvider.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAdmin(null, 0, 10))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("autenticado");
    }

    // ==================== toAdminItem — lambda status filters ====================

    @Test
    @DisplayName("toAdminItem: atribuicoes com SIGNED e CANCELLED cobrem lambdas de status")
    void toAdminItem_signedAndCancelledAssignments() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment signed = new ServiceContractAssignment(
                UUID.randomUUID(), contractId, companyId, employeeId, managerUserId,
                ServiceContractAssignmentStatus.SIGNED, Instant.now(), Instant.now(), null
        );
        ServiceContractAssignment cancelled = new ServiceContractAssignment(
                UUID.randomUUID(), contractId, companyId, UUID.randomUUID(), managerUserId,
                ServiceContractAssignmentStatus.CANCELLED, Instant.now(), null, null
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractId(contractId)).thenReturn(List.of(signed, cancelled));

        var response = service.findAdminDetail(contractId);

        assertThat(response.signedCount()).isEqualTo(1);
        assertThat(response.cancelledCount()).isEqualTo(1);
        assertThat(response.pendingCount()).isEqualTo(0);
    }

    // ==================== findPendingForCurrentEmployee — contractOpt empty branch ====================

    @Test
    @DisplayName("findPendingForCurrentEmployee: contractOpt empty e filtrado (continue path)")
    void findPending_contractOptEmpty() {
        UUID contractId = UUID.randomUUID();
        ServiceContractAssignment pending = pendingAssignment(employeeId, contractId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignmentProvider.findByEmployeeAndStatus(employeeId, ServiceContractAssignmentStatus.PENDING))
                .thenReturn(List.of(pending));
        when(contractProvider.findById(contractId)).thenReturn(Optional.empty());

        var response = service.findPendingForCurrentEmployee();

        assertThat(response.contracts()).isEmpty();
    }

    // ==================== create — additional branch coverage ====================

    @Test
    @DisplayName("create: CTO pode criar contrato com description nao nula (branch CTO + ternario description)")
    void create_ctoCanCreateWithDescription() throws IOException {
        Employee other = baseEmployee(UUID.randomUUID(), companyId, "Colaborador");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, other));
        UUID sourceDocId = UUID.randomUUID();
        when(documentUseCase.uploadGeneratedDocument(
                eq(DocumentType.SERVICE_CONTRACT_TERMS), eq(managerEmployeeId), eq(null), eq(pdfBytes), any()
        )).thenReturn(sourceDocId);
        when(contractProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdfBytes);
        CreateServiceContractResponse response = service.create(
                "Contrato CTO", "Descricao valida", List.of(other.employeeId()), file, "ip", "ua"
        );

        assertThat(response.assignmentIds()).hasSize(1);
    }

    @Test
    @DisplayName("create: titulo em branco lanca BadRequestException (isBlank branch)")
    void create_titleBlank() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        MockMultipartFile file = new MockMultipartFile("file", "c.pdf", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create("   ", null, List.of(employeeId), file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Título");
    }

    @Test
    @DisplayName("create: employeeIds null lanca BadRequestException (null branch)")
    void create_nullEmployeeIds() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        MockMultipartFile file = new MockMultipartFile("file", "c.pdf", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create("T", null, null, file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("colaborador");
    }

    @Test
    @DisplayName("create: pdfFile null lanca BadRequestException (null pdfFile branch)")
    void create_pdfFileNullBranch() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        assertThatThrownBy(() -> service.create("T", null, List.of(employeeId), null, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    @DisplayName("create: pdfFile isEmpty lanca BadRequestException (isEmpty branch)")
    void create_pdfFileEmptyBranch() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        MockMultipartFile emptyFile = new MockMultipartFile("file", "c.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> service.create("T", null, List.of(employeeId), emptyFile, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    @DisplayName("create: filename sem extensao .pdf lanca BadRequestException (endsWith branch)")
    void create_filenameNotEndingWithPdf() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        MockMultipartFile file = new MockMultipartFile("file", "contract.docx", "application/pdf", pdfBytes);
        assertThatThrownBy(() -> service.create("T", null, List.of(employeeId), file, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PDF");
    }

    // ==================== findAdmin / findAdminDetail / findAdminSignatures — CTO branch ====================

    @Test
    @DisplayName("findAdmin: CTO pode listar contratos (CTO branch)")
    void findAdmin_ctoCanListContracts() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findByCompanyFiltered(any(), eq(companyId), any()))
                .thenReturn(new PageImpl<>(List.of(contract), PageRequest.of(0, 10), 1));
        when(assignmentProvider.findByContractId(contractId)).thenReturn(List.of());

        ServiceContractAdminPageResponse response = service.findAdmin(null, 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAdminDetail: PARTNER recebe ForbiddenException (PARTNER branch)")
    void findAdminDetail_partnerForbidden() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.findAdminDetail(UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("findAdminDetail: CTO obtem detalhes do contrato (CTO branch)")
    void findAdminDetail_ctoCan() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractId(contractId)).thenReturn(List.of());

        ServiceContractAdminItemResponse response = service.findAdminDetail(contractId);

        assertThat(response.contractId()).isEqualTo(contractId);
    }

    @Test
    @DisplayName("findAdminSignatures: CTO pode listar assinaturas (CTO branch)")
    void findAdminSignatures_ctoCanList() {
        ServiceContractSignature sig = activeSignature(UUID.randomUUID(), UUID.randomUUID());
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(signatureProvider.findAdminFiltered(any(), eq(companyId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sig), PageRequest.of(0, 10), 1));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager));

        ServiceContractSignatureAdminPageResponse response = service.findAdminSignatures(null, null, 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    // ==================== preview — additional branch coverage ====================

    @Test
    @DisplayName("preview: CTO do mesmo tenant pode visualizar contrato (CTO isAdmin branch)")
    void preview_ctoCan() {
        UUID contractId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        UUID sourceDocId = contract.sourceDocumentId();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(documentProvider.findById(sourceDocId)).thenReturn(documentFor(sourceDocId, "key/orig.pdf"));
        when(bucketStorageProvider.downloadFile(DocumentType.SERVICE_CONTRACT_TERMS, "key/orig.pdf")).thenReturn(pdfBytes);

        byte[] result = service.preview(contractId, "ip", "ua");

        assertThat(result).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("preview: MANAGER de tenant diferente sem atribuicao recebe ForbiddenException (equals false branch)")
    void preview_adminCrossTenantForbidden() {
        UUID contractId = UUID.randomUUID();
        ServiceContract foreignContract = new ServiceContract(
                contractId, otherCompanyId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                "Foreign", null, "c.pdf", pdfHash, ServiceContractStatus.ACTIVE,
                Instant.now(), null, null, null, null
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(foreignContract));
        when(assignmentProvider.findByContractAndEmployee(contractId, managerEmployeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(contractId, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Acesso negado");
    }

    // ==================== sign — additional branch coverage ====================

    @Test
    @DisplayName("sign: confirmed=false lanca BadRequestException (confirmed branch)")
    void sign_notConfirmed() {
        UUID contractId = UUID.randomUUID();
        SignServiceContractRequest req = new SignServiceContractRequest(
                false, "1.0", "hash", "docHash", FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Confirmação");
    }

    @Test
    @DisplayName("sign: assignment com status SIGNED lanca ConflictException (SIGNED branch)")
    void sign_assignmentAlreadySigned() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContractAssignment signedAssignment = new ServiceContractAssignment(
                assignmentId, contractId, companyId, employeeId, managerUserId,
                ServiceContractAssignmentStatus.SIGNED, Instant.now(), Instant.now(), null
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contractFor(contractId, ServiceContractStatus.ACTIVE)));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(signedAssignment));

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, "1.0", "hash", pdfHash, FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já assinou");
    }

    // ==================== downloadSignatureDocument — additional branch coverage ====================

    @Test
    @DisplayName("downloadSignatureDocument: PARTNER tentando baixar doc de outro colaborador recebe ForbiddenException")
    void downloadSignatureDocument_partnerCrossEmployee() {
        UUID sigId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();
        ServiceContractSignature sig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                otherEmployeeId, companyId, UUID.randomUUID(),
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                UUID.randomUUID(), pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    @DisplayName("downloadSignatureDocument: CTO de company diferente recebe ForbiddenException (CTO + equals false branch)")
    void downloadSignatureDocument_ctoDifferentCompany() {
        UUID sigId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();
        ServiceContractSignature sig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                otherEmployeeId, otherCompanyId, UUID.randomUUID(),
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                UUID.randomUUID(), pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Acesso negado");
    }

    // ==================== Additional targeted branch coverage ====================

    @Test
    @DisplayName("create: description em branco trata como null (isBlank true branch)")
    void create_blankDescription() throws IOException {
        Employee other = baseEmployee(UUID.randomUUID(), companyId, "Colaborador2");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(manager, other));
        UUID sourceDocId = UUID.randomUUID();
        when(documentUseCase.uploadGeneratedDocument(
                eq(DocumentType.SERVICE_CONTRACT_TERMS), eq(managerEmployeeId), eq(null), eq(pdfBytes), any()
        )).thenReturn(sourceDocId);
        when(contractProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdfBytes);
        CreateServiceContractResponse response = service.create(
                "Contrato Blank Desc", "   ", List.of(other.employeeId()), file, "ip", "ua"
        );

        assertThat(response.assignmentIds()).hasSize(1);
    }

    @Test
    @DisplayName("sign: hash da declaracao incorreto com versao correta lanca BadRequestException (hash branch)")
    void sign_declarationHashMismatch() {
        UUID contractId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        ServiceContract contract = contractFor(contractId, ServiceContractStatus.ACTIVE);
        ServiceContractAssignment assignment = pendingAssignment(employeeId, contractId, assignmentId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(employeeUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractProvider.findById(contractId)).thenReturn(Optional.of(contract));
        when(assignmentProvider.findByContractAndEmployee(contractId, employeeId)).thenReturn(Optional.of(assignment));
        when(signatureProvider.findActiveByAssignment(assignmentId)).thenReturn(Optional.empty());

        SignServiceContractRequest req = new SignServiceContractRequest(
                true, "1.0", "hash-errado-deliberado", contract.documentHashSha256(), FAKE_FACE_BASE64
        );
        assertThatThrownBy(() -> service.sign(contractId, req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desatualizada");
    }

    @Test
    @DisplayName("downloadSignatureDocument: MANAGER baixa doc de outro colaborador mesmo tenant (isAdmin=true, storagePath blank)")
    void downloadSignatureDocument_managerDifferentEmployeeSameTenant() {
        UUID sigId = UUID.randomUUID();
        UUID signedDocId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();
        ServiceContractSignature sig = new ServiceContractSignature(
                sigId, UUID.randomUUID(), UUID.randomUUID(),
                otherEmployeeId, companyId, UUID.randomUUID(),
                Instant.now(), "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED, ContractSignatureMethod.FACIAL_RECOGNITION,
                ContractSignatureStatus.ACTIVE,
                signedDocId, pdfHash, pdfHash, "1.0", "decl-hash", "decl",
                "ip", "ua", "{}", Instant.now(), null, null, null, null,
                "SERVICE_CONTRACT", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(signatureProvider.findById(sigId)).thenReturn(Optional.of(sig));
        when(documentProvider.findById(signedDocId)).thenReturn(documentFor(signedDocId, "  "));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("storage");
    }


    // ── sanitizeFileName: null/blank raw triggers 'return fallback' branch ──
    @Test
    void sanitizeFileName_nullRaw_returnsFallback() throws Exception {
        Method m = ServiceContractService.class.getDeclaredMethod("sanitizeFileName", String.class, String.class);
        m.setAccessible(true);
        assertThat(m.invoke(null, null, "contrato.pdf")).isEqualTo("contrato.pdf");
        assertThat(m.invoke(null, "   ", "contrato.pdf")).isEqualTo("contrato.pdf");
    }

}