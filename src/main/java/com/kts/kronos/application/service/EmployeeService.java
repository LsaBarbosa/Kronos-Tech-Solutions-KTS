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
        var userRole = jwtAuthenticatedUser.getRoleFromToken(); // Obtém a role

        UUID companyId;

        if ("CTO".equals(userRole)) {
            // CTO deve passar o companyId no request
            if (req.companyId() == null) {
                throw new BadRequestException("O companyId é obrigatório para a criação de um colaborador por um CTO.");
            }
            companyId = req.companyId();
        } else if ("MANAGER".equals(userRole)) {
            // MANAGER: Obtém o companyId do próprio funcionário autenticado
            var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
            var managerEmployee = employeeProvider.findById(managerEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
            companyId = managerEmployee.companyId();
        } else {
            // Proteção extra
            throw new ForbiddenException("Usuário sem permissão para criar colaboradores.");
        }


        if (employeeProvider.findByCpf(req.cpf()).isPresent())
            throw new BadRequestException(CPF_ALREADY_EXIST);

        var address = viaCep.lookup(req.address().postalCode())
                .withNumber(req.address().number());

        double salary = req.salary() != null ? req.salary() : 0.0;

        var newEmployee = new Employee(
                req.fullName(),
                req.cpf(),
                req.jobPosition(),
                req.email(),
                salary,
                req.phone(),
                address,
                companyId,
                null,
                req.homeOffice()
        );
        var savedEmployee = employeeProvider.save(newEmployee);

        if (req.faceImageBase64() != null && !req.faceImageBase64().isBlank()) {
            // 1. Chama o método de registro (passando null para oldS3ObjectKey)
            var s3Key = handleFaceRegistration(
                    savedEmployee.employeeId(),
                    savedEmployee.faceS3ObjectKey(),
                    req.faceImageBase64()
            );

            // 2. Re-salva o Employee com a chave S3 (faceS3ObjectKey)
            savedEmployee = savedEmployee.withFaceS3ObjectKey(s3Key);
            employeeProvider.save(savedEmployee);
        }

        return savedEmployee;
    }

    @Override
    public List<Employee> listEmployees(Boolean active) {
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var managerEmployee = employeeProvider.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = managerEmployee.companyId();
        return active == null
                ? employeeProvider.findByCompanyId(companyId)
                : employeeProvider.findByCompanyIdAndActive(companyId, active);
    }

    @Override

    public Employee getEmployee(UUID employeeId) {
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var managerEmployee = employeeProvider.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId));

        if (!employee.companyId().equals(managerEmployee.companyId())) {
            throw new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId);
        }

        return employee;
    }

    @Override
    public void updateEmployee(UUID id, UpdateEmployeeManagerRequest req) {
        var employee = getEmployee(id);
        var updatedEmployee = new Employee(
                employee.employeeId(), // Garante que o ID é o mesmo do funcionário original
                req.fullName() != null ? req.fullName() : employee.fullName(),
                req.cpf() != null ? req.cpf() : employee.cpf(),
                req.jobPosition() != null ? req.jobPosition() : employee.jobPosition(),
                req.email() != null ? req.email() : employee.email(),
                req.salary() != null ? req.salary() : employee.salary(),
                req.phone() != null ? req.phone() : employee.phone(),
                employee.active(),
                employee.address(),
                employee.companyId(),
                employee.lastSeenMessageTimestamp(),
                req.homeOffice() != null ? req.homeOffice() : employee.homeOffice(),
                employee.faceS3ObjectKey()
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

    @Override
    public void toggleActivate(UUID employeeId) {
        var employee = getEmployee(employeeId);

        var newStatus = !employee.active();

        // 3. Atualiza o Employee
        var updatedEmployee = employee.withActive(newStatus);
        employeeProvider.save(updatedEmployee);

    }
}