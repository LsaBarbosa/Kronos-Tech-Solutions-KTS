package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import com.kts.kronos.domain.model.enuns.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxDataFactory {

    private static final String SANDBOX_CNPJ       = "00000000000191";
    private static final String SANDBOX_EMAIL       = "demo@kronos-sandbox.local";
    private static final String SANDBOX_CPF         = "000.000.000-00";
    private static final String SANDBOX_PIS         = "00000000000";
    private static final String SANDBOX_POSTAL_CODE = "01310-100";

    private final CompanyRepository            companyRepo;
    private final UserRepository               userRepo;
    private final EmployeeRepository           employeeRepo;
    private final UserCompanyAccessRepository  accessRepo;
    private final TimeRecordRepository         timeRecordRepo;
    private final DocumentRepository           documentRepo;
    private final LegalConsentRepository       consentRepo;
    private final TimeRecordApprovalRepository approvalRepo;
    private final PasswordEncoder              passwordEncoder;
    private final DemoSandboxProperties        props;

    public record SeedResult(
            UUID companyId,
            UUID userId,
            UUID employeeId,
            int pointRecords,
            int documents,
            int requests,
            int files
    ) {}

    @Transactional
    public SeedResult createAll() {
        UUID companyId   = UUID.randomUUID();
        UUID employeeId  = UUID.randomUUID();
        UUID userId      = UUID.randomUUID();

        createCompany(companyId);
        createEmployee(companyId, employeeId);
        createUser(userId, employeeId);
        createUserCompanyAccess(userId, companyId, employeeId);

        int pointRecords = createPointRecords(employeeId);
        int files        = createSandboxDirectory(companyId);
        int documents    = createDocuments(companyId, employeeId);
        createBiometricConsent(employeeId, userId);
        int requests     = createPendingRequests(companyId, employeeId, userId);

        log.info("[DemoSandbox] Seed complete: company={} user={} employee={} " +
                        "pointRecords={} docs={} requests={}",
                companyId, userId, employeeId, pointRecords, documents, requests);

        return new SeedResult(companyId, userId, employeeId,
                pointRecords, documents, requests, files);
    }

    private void createCompany(UUID companyId) {
        CompanyEntity company = CompanyEntity.builder()
                .id(companyId)
                .name(props.getCompanyName())
                .cnpj(SANDBOX_CNPJ)
                .email(SANDBOX_EMAIL)
                .active(true)
                .address(AddressEmbeddable.builder()
                        .street("Avenida Paulista")
                        .number("1000")
                        .postalCode(SANDBOX_POSTAL_CODE)
                        .city("São Paulo")
                        .state("SP")
                        .build())
                .latitude(-23.5614)
                .longitude(-46.6560)
                .sandbox(true)
                .sandboxKey(props.getSandboxKey())
                .build();
        companyRepo.save(company);
    }

    private void createEmployee(UUID companyId, UUID employeeId) {
        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeId(employeeId)
                .fullName("Kronos Demo Manager")
                .cpf(SANDBOX_CPF)
                .pis(SANDBOX_PIS)
                .jobPosition("Gerente Demo")
                .email("kronos_teste@kronos-sandbox.local")
                .salary(5000.0)
                .phone("(11) 99999-0000")
                .active(true)
                .companyId(companyId)
                .homeOffice(false)
                .address(AddressEmbeddable.builder()
                        .street("Avenida Paulista")
                        .number("1000")
                        .postalCode(SANDBOX_POSTAL_CODE)
                        .city("São Paulo")
                        .state("SP")
                        .build())
                .workStartTime(LocalTime.of(8, 0))
                .workEndTime(LocalTime.of(17, 0))
                .breakStartTime(LocalTime.of(12, 0))
                .breakEndTime(LocalTime.of(13, 0))
                .scheduleType(WorkScheduleType.TRADITIONAL_5X2)
                .build();
        employeeRepo.save(employee);
    }

    private void createUser(UUID userId, UUID employeeId) {
        UserEntity user = UserEntity.builder()
                .userId(userId)
                .username(props.getUsername())
                .password(passwordEncoder.encode(props.getInitialPassword()))
                .role(Role.MANAGER)
                .active(true)
                .employeeId(employeeId)
                .sessionVersion(0L)
                .build();
        userRepo.save(user);
    }

    private void createUserCompanyAccess(UUID userId, UUID companyId, UUID employeeId) {
        UserCompanyAccessEntity access = UserCompanyAccessEntity.builder()
                .accessId(UUID.randomUUID())
                .userId(userId)
                .companyId(companyId)
                .employeeId(employeeId)
                .role(Role.MANAGER.name())
                .active(true)
                .defaultCompany(true)
                .createdAt(LocalDateTime.now())
                .build();
        accessRepo.save(access);
    }

    private int createPointRecords(UUID employeeId) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());

        List<TimeRecordEntity> records = new ArrayList<>();
        LocalDate cursor = firstDay;
        while (!cursor.isAfter(lastDay) && !cursor.isAfter(today)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                LocalDateTime start = cursor.atTime(8, 0);
                LocalDateTime end   = cursor.atTime(17, 0);
                records.add(TimeRecordEntity.builder()
                        .startWork(start)
                        .endWork(end)
                        .statusRecord(StatusRecord.CREATED)
                        .edited(false)
                        .active(true)
                        .employeeId(employeeId)
                        .latitude(-23.5614)
                        .longitude(-46.6560)
                        .endLatitude(-23.5614)
                        .endLongitude(-46.6560)
                        .build());
            }
            cursor = cursor.plusDays(1);
        }
        timeRecordRepo.saveAll(records);
        return records.size();
    }

    private int createSandboxDirectory(UUID companyId) {
        Path basePath = Path.of(props.getLocalStorageRoot(), "company", companyId.toString());
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.warn("[DemoSandbox] Could not create sandbox directory: {}", basePath, e);
        }
        return 0;
    }

    private int createDocuments(UUID companyId, UUID employeeId) {
        DocumentType[] types = {
                DocumentType.PAYSLIP,
                DocumentType.TIME_OFF,
                DocumentType.DOCUMENTS,
                DocumentType.EMPLOYEE_DOCUMENTS,
                DocumentType.POINT_RECORD_RECEIPT,
                DocumentType.SERVICE_CONTRACT_TERMS
        };

        List<DocumentEntity> docs = new ArrayList<>();
        for (DocumentType type : types) {
            UUID docId = UUID.randomUUID();
            String relativePath = "company/" + companyId + "/documents/" + type.name().toLowerCase() + "/" + docId + ".pdf";
            Path fullPath = Path.of(props.getLocalStorageRoot(), relativePath);

            writeSyntheticFile(fullPath, "DOCUMENTO SINTETICO - " + type.name() + " - SANDBOX KRONOS TESTE");

            docs.add(DocumentEntity.builder()
                    .documentId(docId)
                    .employeeId(employeeId)
                    .fileName("sandbox-" + type.name().toLowerCase() + ".pdf")
                    .contentType("application/pdf")
                    .storagePath(fullPath.toString())
                    .checksumSha256("SANDBOX_SYNTHETIC")
                    .type(type)
                    .build());
        }
        documentRepo.saveAll(docs);
        return docs.size();
    }

    private void createBiometricConsent(UUID employeeId, UUID userId) {
        LegalConsentEntity consent = LegalConsentEntity.builder()
                .consentId(UUID.randomUUID())
                .employeeId(employeeId)
                .userId(userId)
                .consentType(ConsentType.BIOMETRIC_AUTHENTICATION)
                .legalBasis(LegalBasis.CONSENT)
                .purpose("Consentimento sintético para autenticação biométrica — Empresa Demo Sandbox")
                .version("SANDBOX-1.0")
                .contentHashSha256("SANDBOX_SYNTHETIC_HASH")
                .grantedAt(Instant.now())
                .ipAddress("127.0.0.1")
                .userAgent("Kronos-Demo-Sandbox/1.0")
                .createdAt(Instant.now())
                .build();
        consentRepo.save(consent);
    }

    private int createPendingRequests(UUID companyId, UUID employeeId, UUID userId) {
        LocalDate today = LocalDate.now();

        // Férias pendente (próxima semana)
        TimeRecordEntity vacation = TimeRecordEntity.builder()
                .startWork(today.plusDays(7).atTime(8, 0))
                .endWork(today.plusDays(14).atTime(17, 0))
                .statusRecord(StatusRecord.REQUEST_VACATION)
                .active(true)
                .edited(false)
                .employeeId(employeeId)
                .build();

        // Abono pendente (ontem)
        TimeRecordEntity timeOff = TimeRecordEntity.builder()
                .startWork(today.minusDays(1).atTime(8, 0))
                .endWork(today.minusDays(1).atTime(17, 0))
                .statusRecord(StatusRecord.TIME_OFF_REQUEST)
                .active(true)
                .edited(false)
                .employeeId(employeeId)
                .build();

        // Ajuste de registro pendente (anteontem, esqueceu de bater)
        TimeRecordEntity adjustment = TimeRecordEntity.builder()
                .startWork(today.minusDays(2).atTime(8, 0))
                .endWork(today.minusDays(2).atTime(17, 0))
                .statusRecord(StatusRecord.WORK_TIME_REQUEST)
                .active(true)
                .edited(false)
                .employeeId(employeeId)
                .build();

        List<TimeRecordEntity> saved = timeRecordRepo.saveAll(List.of(vacation, timeOff, adjustment));

        // Cria aprovação para o ajuste de registro
        UUID managerId = userId;
        TimeRecordApprovalEntity approvalEntry = TimeRecordApprovalEntity.builder()
                .timeRecordId(saved.get(2).getTimeRecordId())
                .requestingEmployeeId(employeeId)
                .managerId(managerId)
                .newStartWork(today.minusDays(2).atTime(8, 0))
                .newEndWork(today.minusDays(2).atTime(17, 0))
                .build();
        approvalRepo.save(approvalEntry);

        return 3;
    }

    private void writeSyntheticFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("[DemoSandbox] Could not write synthetic file: {}", path, e);
        }
    }
}
