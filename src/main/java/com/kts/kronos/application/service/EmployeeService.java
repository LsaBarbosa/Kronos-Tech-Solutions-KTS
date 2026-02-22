package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService implements EmployeeUseCase {

    public static final String MANAGER_AND_EMPLOYEE_NOT_SAME_COMPANY = "Acesso negado: Manager da empresa {} tentou acessar colaborador da empresa {}";
    public static final String NO_USER_LINKED_TO_EMPLOYEE = "Colaborador não está linkado com usuário";
    public static final String PROFILE_UPDATED_BY_EMPLOYEE = "Perfil atualizado pelo colaborador {}";
    private final EmployeeProvider employeeProvider;
    private final AddressLookupProvider viaCep;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final UserProvider userProvider;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;


    // MANAGER
    @Override
    public Employee createEmployee(CreateEmployeeRequest req) {
        log.info(LOG_CREATE_INIT, req.fullName());

        var companyId = resolveCompanyIdForCreation(req);
        var existingEmployeeOpt = employeeProvider.findByCpf(req.cpf());

        if (existingEmployeeOpt.isPresent()) {
            var existing = existingEmployeeOpt.get();
            if (userProvider.findByEmployeeId(existing.employeeId()).isPresent()) {
                throw new BadRequestException(CPF_ALREADY_EXIST);
            }
            log.info(LOG_UPDATE_ORPHAN, req.cpf());
            return updateOrphanEmployee(existing, req, companyId);
        }

        var address = viaCep.lookup(req.address().postalCode()).withNumber(req.address().number());

        Employee newEmployee = new Employee(
                req.fullName(), req.cpf(), req.pis(), req.jobPosition(),
                req.email(), req.salary() != null ? req.salary() : 0.0,
                req.phone(), true, address, companyId, null, req.homeOffice(),
                req.workStartTime() != null ? req.workStartTime() : LocalTime.of(8, 0),
                req.workEndTime() != null ? req.workEndTime() : LocalTime.of(17, 0),
                req.breakStartTime() != null ? req.breakStartTime() : LocalTime.of(12, 0),
                req.breakEndTime() != null ? req.breakEndTime() : LocalTime.of(13, 0),
                req.scheduleType(), req.scaleStartDate(), req.preferredDayOff(),
                req.weekendOffIndex(), req.fixedWorkDays()
        );

        var saved = employeeProvider.save(newEmployee);
        processFaceImageIfPresent(saved, req.faceImageBase64());

        log.info(LOG_CREATE_SUCCESS, saved.employeeId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> listEmployees(Boolean active) {
        var companyId = getCompanyIdFromLoggedUser();
        log.debug(LOG_LIST_EMPLOYEES, active, companyId);

        return (active == null)
                ? employeeProvider.findByCompanyId(companyId)
                : employeeProvider.findByCompanyIdAndActive(companyId, active);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployee(UUID employeeId) {
        log.debug(LOG_GET_EMPLOYEE, employeeId);
        var managerCompanyId = getCompanyIdFromLoggedUser();

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId));

        if (!employee.companyId().equals(managerCompanyId)) {
            log.error(MANAGER_AND_EMPLOYEE_NOT_SAME_COMPANY, managerCompanyId, employee.companyId());
            throw new ResourceNotFoundException(EMPLOYEE_NOT_FOUND);
        }

        return employee;
    }

    @Override
    public void updateEmployee(UUID id, UpdateEmployeeManagerRequest req) {
        log.info(LOG_UPDATE_INIT, id);
        var existing = getEmployee(id);
        var updated = new Employee(
                existing.employeeId(),
                req.fullName() != null ? req.fullName() : existing.fullName(),
                existing.cpf(),
                req.pis() != null ? req.pis() : existing.pis(),
                req.jobPosition() != null ? req.jobPosition() : existing.jobPosition(),
                req.email() != null ? req.email() : existing.email(),
                req.salary() != null ? req.salary() : existing.salary(),
                req.phone() != null ? req.phone() : existing.phone(),
                existing.active(), existing.address(), existing.companyId(),
                existing.lastSeenMessageTimestamp(),
                req.homeOffice() != null ? req.homeOffice() : existing.homeOffice(),
                existing.faceS3ObjectKey(),
                req.workStartTime() != null ? req.workStartTime() : existing.workStartTime(),
                req.workEndTime() != null ? req.workEndTime() : existing.workEndTime(),
                req.breakStartTime() != null ? req.breakStartTime() : existing.breakStartTime(),
                req.breakEndTime() != null ? req.breakEndTime() : existing.breakEndTime(),
                req.scheduleType() != null ? req.scheduleType() : existing.scheduleType(),
                req.scaleStartDate() != null ? req.scaleStartDate() : existing.scaleStartDate(),
                req.preferredDayOff() != null ? req.preferredDayOff() : existing.preferredDayOff(),
                req.weekendOffIndex() != null ? req.weekendOffIndex() : existing.weekendOffIndex(),
                req.fixedWorkDays() != null ? req.fixedWorkDays() : existing.fixedWorkDays()
        );

        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updated = updated.withAddress(lookup.withNumber(req.address().number()));
        }

        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            var newKey = handleFaceRegistration(updated.employeeId(), updated.faceS3ObjectKey(), req.faceImageBase64());
            updated = updated.withFaceS3ObjectKey(newKey);
        }

        employeeProvider.save(updated);
    }

    @Override
    public void deleteEmployee(UUID id) {
        log.warn(LOG_DELETE_EMPLOYEE, id);
        var employee = getEmployee(id);
        employeeProvider.deleteById(employee.employeeId());
    }
    // PARTNER

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfile getOwnProfile() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var user = userProvider.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_USER_LINKED_TO_EMPLOYEE));

        return new EmployeeProfile(employee, user.role().name());
    }

    @Override
    public void updateOwnProfile(UpdateEmployeePartnerRequest req) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var updatedAddress = employee.address();
        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updatedAddress = lookup.withNumber(req.address().number());
        }

        var updated = employee
                .withEmail(req.email() != null ? req.email() : employee.email())
                .withPhone(req.phone() != null ? req.phone() : employee.phone())
                .withAddress(updatedAddress);

        employeeProvider.save(updated);
        log.info(PROFILE_UPDATED_BY_EMPLOYEE, employeeId);
    }

    @Override
    public void markMessagesAsSeen() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        employeeProvider.save(employee.withLastSeenMessageTimestamp(LocalDateTime.now()));
        log.debug(LOG_MARK_SEEN, employeeId);
    }

    public boolean cpfExists(String cpf) {
        return employeeProvider.cpfExists(cpf);
    }

    @Override
    public void toggleActivate(UUID employeeId) {
        var employee = getEmployee(employeeId);
        var newStatus = !employee.active();
        log.info(LOG_TOGGLE_STATUS, employeeId, newStatus);
        employeeProvider.save(employee.withActive(newStatus));
    }

    private UUID resolveCompanyIdForCreation(CreateEmployeeRequest req) {
        String role = jwtAuthenticatedUser.getRoleFromToken();
        if ("CTO".equals(role)) {
            if (req.companyId() == null) throw new BadRequestException("O companyId é obrigatório para CTO.");
            return req.companyId();
        }
        return getCompanyIdFromLoggedUser();
    }

    private UUID getCompanyIdFromLoggedUser() {
        return employeeProvider.findById(jwtAuthenticatedUser.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND))
                .companyId();
    }

    private void processFaceImageIfPresent(Employee emp, String base64) {
        if (base64 != null && !base64.isBlank()) {
            String key = handleFaceRegistration(emp.employeeId(), emp.faceS3ObjectKey(), base64);
            employeeProvider.save(emp.withFaceS3ObjectKey(key));
        }
    }

    private String handleFaceRegistration(UUID employeeId, String oldKey, String base64) {
        log.info(LOG_FACE_INIT, employeeId);
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            String newKey = faceStorageProvider.uploadFaceImage(employeeId, new ByteArrayInputStream(imageBytes), "image/jpeg");

            String faceId = faceRecognitionProvider.indexFace(newKey, employeeId);
            if (faceId == null) {
                faceStorageProvider.deleteFaceImage(newKey);
                throw new BadRequestException(NO_FACE_DETECTED);
            }

            if (oldKey != null && !oldKey.isBlank()) {
                faceStorageProvider.deleteFaceImage(oldKey);
            }

            log.info(LOG_FACE_SUCCESS, newKey);
            return newKey;
        } catch (Exception e) {
            log.error(LOG_FACE_ERROR, employeeId, e.getMessage());
            throw new RuntimeException("Falha no processamento biométrico.", e);
        }
    }

    private Employee updateOrphanEmployee(Employee existing, CreateEmployeeRequest req, UUID companyId) {
        var address = viaCep.lookup(req.address().postalCode()).withNumber(req.address().number());

        Employee updated = new Employee(
                existing.employeeId(), req.fullName(), existing.cpf(), req.pis(),
                req.jobPosition(), req.email(), req.salary() != null ? req.salary() : 0.0,
                req.phone(), true, address, companyId, null, req.homeOffice(),
                existing.faceS3ObjectKey(), existing.workStartTime(), existing.workEndTime(),
                existing.breakStartTime(), existing.breakEndTime(), req.scheduleType(),
                req.scaleStartDate(), req.preferredDayOff(), req.weekendOffIndex(), req.fixedWorkDays()
        );

        var saved = employeeProvider.save(updated);
        processFaceImageIfPresent(saved, req.faceImageBase64());
        return saved;
    }

}