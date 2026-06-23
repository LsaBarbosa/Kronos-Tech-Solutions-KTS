package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.servicecontract.CreateServiceContractResponse;
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
}
