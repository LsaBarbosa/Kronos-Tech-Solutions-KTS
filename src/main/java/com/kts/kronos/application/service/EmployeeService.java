package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.EmployeeDetailResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeListItemResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeListResponse;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.dto.employee.RegisterFaceRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.cache.ApplicationCacheNames;
import com.kts.kronos.application.cache.CacheScopes;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.service.AuditService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmployeeService implements EmployeeUseCase {

    private final EmployeeProvider employeeProvider;
    private final AddressLookupProvider viaCep;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final UserProvider userProvider;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final BiometricProtectionService biometricProtectionService;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final AuthenticationRateLimitService authenticationRateLimitService;
    private final KronosMetrics kronosMetrics;
    private final LegalConsentProvider legalConsentProvider;
    private final AuditService auditService;
    private final CompanyProvider companyProvider;
    private final CacheProvider cacheProvider;

    // MANAGER
    @Override
    public Employee createEmployee(CreateEmployeeRequest req) {
        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            throw new BadRequestException("BIOMETRIC_ENROLLMENT_REQUIRES_DATA_SUBJECT_ACTION");
        }

        var userRole = jwtAuthenticatedUser.getCurrentRole();
        UUID companyId;

        if (userRole == Role.CTO) {
            if (req.companyId() == null) {
                throw new BadRequestException(COMPANY_ID_IS_REQUIRED_TO_CREATE_FIRST_MANAGER);
            }
            companyId = req.companyId();
        } else if (userRole == Role.MANAGER) {
            companyId = getCompanyIdFromLoggedUser();
        } else {
            throw new ForbiddenException("Usuário sem permissão para criar colaboradores.");
        }

        // Verificar se já existe CPF nesta empresa (bloqueado pela constraint por tenant)
        if (employeeProvider.cpfExistsInCompany(companyId, req.cpf())) {
            throw new ConflictException(CPF_ALREADY_EXIST_IN_COMPANY);
        }

        var address = viaCep.lookup(req.address().postalCode())
                .withNumber(req.address().number());

        double salary = req.salary() != null ? req.salary() : 0.0;

        LocalTime start = req.workStartTime() != null ? req.workStartTime() : LocalTime.of(8, 0);
        LocalTime end = req.workEndTime() != null ? req.workEndTime() : LocalTime.of(17, 0);
        LocalTime breakStart = req.breakStartTime() != null ? req.breakStartTime() : LocalTime.of(12, 0);
        LocalTime breakEnd = req.breakEndTime() != null ? req.breakEndTime() : LocalTime.of(13, 0);

        // --- MAPEAR NOVOS CAMPOS PARA O DOMÍNIO ---
        var newEmployee = new Employee(
                UUID.randomUUID(), // Gera ID
                req.fullName(),
                req.cpf(),
                req.pis(),
                req.jobPosition(),
                req.email(),
                salary,
                req.phone(),
                true, // Active
                address,
                companyId,
                null,
                req.homeOffice(),
                null, // S3 Key (será setada abaixo)
                start,
                end,
                breakStart,
                breakEnd,
                // Novos Campos de Escala
                req.scheduleType(),
                req.scaleStartDate(),
                req.preferredDayOff(),
                req.weekendOffIndex(),
                req.fixedWorkDays()
        );

        Employee savedEmployee;
        try {
            savedEmployee = employeeProvider.save(newEmployee);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(CPF_ALREADY_EXIST);
        }

        kronosMetrics.employeeCreated();
        invalidateEmployeeCaches();
        return savedEmployee;
    }

    @Override
    public EmployeeListResponse listEmployeesResponse(Boolean active) {
        UUID companyId = getCompanyIdFromLoggedUser();
        return cache(
                ApplicationCacheNames.EMPLOYEE_LIST,
                CacheScopes.authenticatedScope("companyId=" + companyId, "active=" + active),
                EmployeeListResponse.class,
                () -> buildEmployeeListResponse(active)
        );
    }

    @Override
    public EmployeeListResponse listEmployeesByCompany(UUID companyId, Boolean active) {
        var employees = active == null
                ? employeeProvider.findByCompanyId(companyId)
                : employeeProvider.findByCompanyIdAndActive(companyId, active);

        var companyName = companyProvider.findById(companyId)
                .map(Company::name)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        var items = employees.stream()
                .map(e -> EmployeeListItemResponse.fromDomain(e, companyName))
                .toList();
        return new EmployeeListResponse(items);
    }

    @Override
    public List<Employee> listEmployees(Boolean active) {
        // 1. Obtém a empresa do gerente logado
        UUID companyId = getCompanyIdFromLoggedUser();

        // 2. Busca APENAS dentro dessa empresa
        if (active == null) {
            return employeeProvider.findByCompanyId(companyId);
        } else {
            return employeeProvider.findByCompanyIdAndActive(companyId, active);
        }
    }

    @Override
    public Employee getEmployee(UUID employeeId) {
        UUID managerCompanyId = getCompanyIdFromLoggedUser();

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId));

        // SEGURANÇA: Se o funcionário buscado não for da mesma empresa do gerente, BLOQUEIA.
        if (!employee.companyId().equals(managerCompanyId)) {
            // Lança 404 para não revelar que o ID existe em outra empresa
            throw new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId);
        }

        return employee;
    }

    @Override
    public void updateEmployee(UUID id, UpdateEmployeeManagerRequest req) {
        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            throw new BadRequestException("BIOMETRIC_ENROLLMENT_REQUIRES_DATA_SUBJECT_ACTION");
        }

        var existingEmployee = getEmployee(id);

        var updatedEmployee = new Employee(
                existingEmployee.employeeId(), // Garante que o ID é o mesmo do funcionário original
                req.fullName() != null ? req.fullName() : existingEmployee.fullName(),
                existingEmployee.cpf(),
                req.pis() != null ? req.pis() : existingEmployee.pis(),
                req.jobPosition() != null ? req.jobPosition() : existingEmployee.jobPosition(),
                req.email() != null ? req.email() : existingEmployee.email(),
                req.salary() != null ? req.salary() : existingEmployee.salary(),
                req.phone() != null ? req.phone() : existingEmployee.phone(),
                existingEmployee.active(),
                existingEmployee.address(),
                existingEmployee.companyId(),
                existingEmployee.lastSeenMessageTimestamp(),
                req.homeOffice() != null ? req.homeOffice() : existingEmployee.homeOffice(),
                existingEmployee.faceS3ObjectKey(),
                req.workStartTime() != null ? req.workStartTime() : existingEmployee.workStartTime(),
                req.workEndTime() != null ? req.workEndTime() : existingEmployee.workEndTime(),
                req.breakStartTime() != null ? req.breakStartTime() : existingEmployee.breakStartTime(),
                req.breakEndTime() != null ? req.breakEndTime() : existingEmployee.breakEndTime(),
                req.scheduleType() != null ? req.scheduleType() : existingEmployee.scheduleType(),
                req.scaleStartDate() != null ? req.scaleStartDate() : existingEmployee.scaleStartDate(),
                req.preferredDayOff() != null ? req.preferredDayOff() : existingEmployee.preferredDayOff(),
                req.weekendOffIndex() != null ? req.weekendOffIndex() : existingEmployee.weekendOffIndex(),
                req.fixedWorkDays() != null ? req.fixedWorkDays() : existingEmployee.fixedWorkDays()

        );

        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            var updatedAddress = lookup.withNumber(req.address().number());
            updatedEmployee = updatedEmployee.withAddress(updatedAddress);
        }

        employeeProvider.save(updatedEmployee);
        kronosMetrics.employeeUpdated();
        invalidateEmployeeCaches();
    }


    @Override
    public void deleteEmployee(UUID id) {
        var employee = getEmployee(id);

        if (userProvider.existsByEmployeeId(employee.employeeId())) {
            throw new BadRequestException(EMPLOYEE_HAS_LINKED_USER);
        }
        employeeProvider.save(employee.deactivate(currentUserIdOrNull(), "EMPLOYEE_DELETE"));
        invalidateEmployeeCaches();
    }
    // PARTNER

    @Override
    public EmployeeProfile getOwnProfile() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);
        // Role comes from the JWT (Spring Security context) — covers both single-company
        // (User.employee_id) and multi-company (tb_user_company_access.employee_id) users
        // without querying User.employee_id which only exists for the primary company.
        var role = jwtAuthenticatedUser.getCurrentRole().name();
        return new EmployeeProfile(employee, role);
    }

    @Override
    public EmployeeDetailResponse getOwnProfileResponse() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        return cache(
                ApplicationCacheNames.EMPLOYEE_OWN_PROFILE,
                CacheScopes.authenticatedScope("employeeId=" + employeeId),
                EmployeeDetailResponse.class,
                () -> {
                    var profile = getOwnProfile();
                    var companyName = companyProvider.findById(profile.employee().companyId())
                            .map(Company::name)
                            .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
                    return EmployeeDetailResponse.fromDomain(profile.employee(), companyName, profile.role());
                }
        );
    }

    @Override
    public void updateOwnProfile(UpdateEmployeePartnerRequest req) {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);
        var updateAddress = employee.address();
        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updateAddress = lookup.withNumber(req.address().number());
        }
        var updated = employee
                .withEmail(req.email() != null ? req.email() : employee.email())
                .withPhone(req.phone() != null ? req.phone() : employee.phone())
                .withAddress(updateAddress);
        employeeProvider.save(updated);
        invalidateEmployeeCaches();
    }

    @Override
    public void markMessagesAsSeen() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var updatedEmployee = employee.withLastSeenMessageTimestamp(LocalDateTime.now());
        employeeProvider.save(updatedEmployee);
        invalidateEmployeeCaches();
    }

    public boolean cpfExists(String cpf) {
        authenticationRateLimitService.checkAdminSearchRateLimit();
        return employeeProvider.cpfExists(cpf);
    }

    @Override
    public boolean cpfExistsInActiveCompany(String cpf) {
        authenticationRateLimitService.checkAdminSearchRateLimit();
        UUID companyId = getCompanyIdFromLoggedUser();
        return employeeProvider.cpfExistsInCompany(companyId, cpf);
    }

    @Override
    public boolean cpfExistsInCompany(UUID companyId, String cpf) {
        authenticationRateLimitService.checkAdminSearchRateLimit();
        return employeeProvider.cpfExistsInCompany(companyId, cpf);
    }

    @Override
    public java.util.Optional<EmployeeDetailResponse> findByCpfGlobal(String cpf) {
        authenticationRateLimitService.checkAdminSearchRateLimit();
        return employeeProvider.findAllByCpf(cpf).stream()
                .findFirst()
                .map(employee -> {
                    String companyName = companyProvider.findById(employee.companyId())
                            .map(Company::name)
                            .orElse("");
                    return EmployeeDetailResponse.fromDomain(employee, companyName, null);
                });
    }

    @Override
    public void toggleActivate(UUID employeeId) {
        var employee = getEmployee(employeeId);

        var newStatus = !employee.active();

        // 3. Atualiza o Employee
        var updatedEmployee = employee.withActive(newStatus);
        employeeProvider.save(updatedEmployee);
        invalidateEmployeeCaches();

    }

    @Override
    public void enrollBiometricByManager(UUID employeeId, RegisterFaceRequest req) {
        var employee = getEmployee(employeeId);

        var consentStatus = acceptTermsUseCase.getBiometricConsentStatus(employeeId);
        if (!consentStatus.accepted()) {
            throw new ConflictException("O colaborador ainda não aceitou o termo biométrico vigente.");
        }

        boolean isReplacement = employee.faceS3ObjectKey() != null
                && !employee.faceS3ObjectKey().isBlank();

        biometricProtectionService.protectEnrollment(
                employeeId, req.faceImageBase64(), req.livenessPassed());

        var s3Key = handleFaceRegistration(
                employeeId, employee.faceS3ObjectKey(), req.faceImageBase64());

        var updatedEmployee = employee.withFaceS3ObjectKey(s3Key);
        employeeProvider.save(updatedEmployee);

        var action = isReplacement
                ? AuditAction.BIOMETRIC_ENROLLMENT_REPLACED_BY_MANAGER
                : AuditAction.BIOMETRIC_ENROLLMENT_BY_MANAGER;

        auditService.register(
                action,
                jwtAuthenticatedUser.getuserId(),
                employeeId,
                employee.companyId(),
                "BIOMETRIC",
                employeeId.toString(),
                "HIGH",
                null, null,
                "Biometria cadastrada/substituída por gestor. Target: " + employeeId
        );

        kronosMetrics.employeeCreated();
        invalidateEmployeeCaches();
    }

    private void invalidateEmployeeCaches() {
        if (cacheProvider == null) {
            return;
        }

        try {
            cacheProvider.evictNamespace(ApplicationCacheNames.EMPLOYEE_LIST);
            cacheProvider.evictNamespace(ApplicationCacheNames.EMPLOYEE_OWN_PROFILE);
            cacheProvider.evictNamespace(ApplicationCacheNames.DASHBOARD_SUMMARY);
        } catch (RuntimeException ex) {
            log.warn("event=redis_cache_invalidation_failed scope=employee reason={}", ex.getClass().getSimpleName());
        }
    }

    private EmployeeListResponse buildEmployeeListResponse(Boolean active) {
        var employees = listEmployees(active);

        var employeeResponses = employees.stream().map(employee -> {
            String companyName = companyProvider.findById(employee.companyId())
                    .map(Company::name)
                    .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

            return EmployeeListItemResponse.fromDomain(employee, companyName);
        }).toList();
        return new EmployeeListResponse(employeeResponses);
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, java.util.function.Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }

    private String handleFaceRegistration(UUID employeeId, String oldS3ObjectKey, String faceImageBase64) {
        String newS3ObjectKey = null;
        try {
            // 1. Decodifica e cria Stream da Imagem
            byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            // 2. Upload para o S3 (cria um novo arquivo)
            newS3ObjectKey = faceStorageProvider.uploadFaceImage(employeeId, inputStream, "image/jpeg");

            // 3. Remove templates antigos do colaborador antes de indexar a nova biometria
            faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);

            // 4. Indexar a Face no Rekognition (usa employeeId como ExternalImageId)
            String faceId = faceRecognitionProvider.indexFace(newS3ObjectKey, employeeId);

            if (faceId == null) {
                // Se nenhuma face for detectada, deletar o novo arquivo do S3 e lançar erro
                faceStorageProvider.deleteFaceImage(newS3ObjectKey);
                throw new BadRequestException(NO_FACE_DETECTED);
            }

            // 5. Se a indexação foi bem-sucedida, deletar a imagem antiga do S3 (se existir)
            if (oldS3ObjectKey != null && !oldS3ObjectKey.isBlank()) {
                faceStorageProvider.deleteFaceImage(oldS3ObjectKey);
            }

            return newS3ObjectKey;

        } catch (IllegalArgumentException e) {
            if (newS3ObjectKey != null) {
                faceStorageProvider.deleteFaceImage(newS3ObjectKey);
            }
            throw new BadRequestException("Dados de imagem inválidos: Formato Base64 incorreto.");
        } catch (RuntimeException e) {
            // Captura falhas de serviço do Rekognition ou S3
            if (newS3ObjectKey != null) {
                faceStorageProvider.deleteFaceImage(newS3ObjectKey);
            }
            throw new RuntimeException("Falha ao registrar face no Rekognition.", e);
        }
    }

    private UUID getCompanyIdFromLoggedUser() {
        UUID activeCompanyId = jwtAuthenticatedUser.getActiveCompanyId();
        if (activeCompanyId != null) {
            return activeCompanyId;
        }
        // Fallback para tokens antigos sem activeCompanyId
        var managerId = jwtAuthenticatedUser.getEmployeeId();
        var manager = employeeProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        return manager.companyId();
    }

    private UUID currentUserIdOrNull() {
        try {
            return jwtAuthenticatedUser.getuserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Employee updateOrphanEmployee(Employee existing, CreateEmployeeRequest req, UUID companyId) {
        var address = viaCep.lookup(req.address().postalCode())
                .withNumber(req.address().number());

        double salary = req.salary() != null ? req.salary() : 0.0;

        // Reconstrói o objeto mantendo o ID original e a chave S3 antiga (se houver)
        var updatedEmployee = new Employee(
                existing.employeeId(), // Importante: Mantém o UUID original
                req.fullName(),
                req.cpf(),
                req.pis(),
                req.jobPosition(),
                req.email(),
                salary,
                req.phone(),
                true, // Reativa o funcionário caso estivesse inativo
                address,
                companyId,
                null, // Reseta o timestamp de mensagem
                req.homeOffice(),
                existing.faceS3ObjectKey(),
                existing.workStartTime(),
                existing.workEndTime(),
                existing.breakStartTime(),
                existing.breakEndTime(),
                req.scheduleType(),
                req.scaleStartDate(),
                req.preferredDayOff(),
                req.weekendOffIndex(),
                req.fixedWorkDays()
                // Mantém a chave antiga temporariamente
        );

        // Salva os dados cadastrais atualizados
        Employee savedEmployee;
        try {
            savedEmployee = employeeProvider.save(updatedEmployee);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(CPF_ALREADY_EXIST);
        }

        // Processa a imagem facial novamente
        // Se houver nova foto, o handleFaceRegistration cuidará de deletar a antiga do S3/Rekognition
        // NOTA: Este código é inalcançável pois createEmployee() rejeita faceImageBase64 no início (LGPD-S01-01)
        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            biometricProtectionService.protectEnrollment(
                    savedEmployee.employeeId(),
                    req.faceImageBase64(),
                    null
            );

            var s3Key = handleFaceRegistration(
                    savedEmployee.employeeId(),
                    savedEmployee.faceS3ObjectKey(),
                    req.faceImageBase64()
            );
            savedEmployee = savedEmployee.withFaceS3ObjectKey(s3Key);
            employeeProvider.save(savedEmployee);
        }

        invalidateEmployeeCaches();
        return savedEmployee;
    }


}
