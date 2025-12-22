package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService implements EmployeeUseCase {

    private final EmployeeProvider employeeProvider;
    private final AddressLookupProvider viaCep;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final UserProvider userProvider;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;


    // MANAGER
    @Override
    public Employee createEmployee(CreateEmployeeRequest req) {
        var userRole = jwtAuthenticatedUser.getRoleFromToken();
        UUID companyId;

        if ("CTO".equals(userRole)) {
            if (req.companyId() == null) {
                throw new BadRequestException("O companyId é obrigatório para a criação de um colaborador por um CTO.");
            }
            companyId = req.companyId();
        } else if ("MANAGER".equals(userRole)) {
            var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
            var managerEmployee = employeeProvider.findById(managerEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
            companyId = managerEmployee.companyId();
        } else {
            throw new ForbiddenException("Usuário sem permissão para criar colaboradores.");
        }

        var existingEmployeeOpt = employeeProvider.findByCpf(req.cpf());

        if (existingEmployeeOpt.isPresent()) {
            var existingEmployee = existingEmployeeOpt.get();
            if (userProvider.findByEmployeeId(existingEmployee.employeeId()).isPresent()) {
                throw new BadRequestException(CPF_ALREADY_EXIST);
            }
            return updateOrphanEmployee(existingEmployee, req, companyId);
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

        var savedEmployee = employeeProvider.save(newEmployee);

        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            var s3Key = handleFaceRegistration(
                    savedEmployee.employeeId(),
                    savedEmployee.faceS3ObjectKey(),
                    req.faceImageBase64()
            );

            savedEmployee = savedEmployee.withFaceS3ObjectKey(s3Key);
            employeeProvider.save(savedEmployee);
        }

        return savedEmployee;
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

        String newS3ObjectKey = updatedEmployee.faceS3ObjectKey();

        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            newS3ObjectKey = handleFaceRegistration(
                    updatedEmployee.employeeId(),
                    updatedEmployee.faceS3ObjectKey(),
                    req.faceImageBase64()
            );
        }
        updatedEmployee = updatedEmployee.withFaceS3ObjectKey(newS3ObjectKey);

        employeeProvider.save(updatedEmployee);
    }


    @Override
    public void deleteEmployee(UUID id) {
        var employee = getEmployee(id);
        employeeProvider.deleteById(employee.employeeId());
    }
    // PARTNER

    @Override
    public EmployeeProfile getOwnProfile() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);

        // BUSCA O USER PELO employeeId
        var user = userProvider.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado para este colaborador."));

        // RETORNA O EMPLOYEE E A ROLE
        return new EmployeeProfile(employee, user.role().name());
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
    }

    @Override
    public void markMessagesAsSeen() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var updatedEmployee = employee.withLastSeenMessageTimestamp(LocalDateTime.now());
        employeeProvider.save(updatedEmployee);
    }

    public boolean cpfExists(String cpf) {
        return employeeProvider.cpfExists(cpf);
    }

    @Override
    public void toggleActivate(UUID employeeId) {
        var employee = getEmployee(employeeId);

        var newStatus = !employee.active();

        // 3. Atualiza o Employee
        var updatedEmployee = employee.withActive(newStatus);
        employeeProvider.save(updatedEmployee);

    }

    private String handleFaceRegistration(UUID employeeId, String oldS3ObjectKey, String faceImageBase64) {
        String newS3ObjectKey = null;
        try {
            // 1. Decodifica e cria Stream da Imagem
            byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            // 2. Upload para o S3 (cria um novo arquivo)
            newS3ObjectKey = faceStorageProvider.uploadFaceImage(employeeId, inputStream, "image/jpeg");

            // 3. Indexar a Face no Rekognition (usa employeeId como ExternalImageId)
            String faceId = faceRecognitionProvider.indexFace(newS3ObjectKey, employeeId);

            if (faceId == null) {
                // Se nenhuma face for detectada, deletar o novo arquivo do S3 e lançar erro
                faceStorageProvider.deleteFaceImage(newS3ObjectKey);
                throw new BadRequestException(NO_FACE_DETECTED);
            }

            // 4. Se a indexação foi bem-sucedida, deletar a imagem antiga do S3 (se existir)
            if (oldS3ObjectKey != null && !oldS3ObjectKey.isBlank()) {
                faceStorageProvider.deleteFaceImage(oldS3ObjectKey);
            }

            // 5. Retorna a nova chave S3
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
        var managerId = jwtAuthenticatedUser.getEmployeeId();
        var manager = employeeProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        return manager.companyId();
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
        var savedEmployee = employeeProvider.save(updatedEmployee);

        // Processa a imagem facial novamente
        // Se houver nova foto, o handleFaceRegistration cuidará de deletar a antiga do S3/Rekognition
        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            var s3Key = handleFaceRegistration(
                    savedEmployee.employeeId(),
                    savedEmployee.faceS3ObjectKey(),
                    req.faceImageBase64()
            );
            savedEmployee = savedEmployee.withFaceS3ObjectKey(s3Key);
            employeeProvider.save(savedEmployee);
        }

        return savedEmployee;
    }


}