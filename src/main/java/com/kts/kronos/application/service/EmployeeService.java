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

        // Lógica para determinar o companyId baseado na role
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

        var employee = new Employee(
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
        return employeeProvider.save(employee);
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
                null,
                req.homeOffice() != null ? req.homeOffice() : employee.homeOffice()
        );

        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            var updatedAddress = lookup.withNumber(req.address().number());
            updatedEmployee = updatedEmployee.withAddress(updatedAddress);
        }
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
    public void registerFaceReference(RegisterFaceRequest request) throws IOException {

        UUID loggedInEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        String userRole = jwtAuthenticatedUser.getRoleFromToken();

        // 1. Determina o alvo e valida a permissão
        UUID targetEmployeeId;
        if ("PARTNER".equals(userRole)) {
            if (request.employeeId() != null && !request.employeeId().equals(loggedInEmployeeId)) {
                throw new ForbiddenException("Parceiro não pode registrar a face de outros colaboradores.");
            }
            targetEmployeeId = loggedInEmployeeId;
        } else if ("MANAGER".equals(userRole) || "CTO".equals(userRole)) {
            if (request.employeeId() == null) {
                targetEmployeeId = loggedInEmployeeId;
            } else {
                targetEmployeeId = request.employeeId();
            }
        } else {
            throw new ForbiddenException("Usuário sem permissão para registrar face.");
        }

        var employee = getEmployee(targetEmployeeId);

        // 2. Decodificar e criar Stream da Imagem
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(request.faceImageBase64());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Dados de imagem inválidos: Formato Base64 incorreto.");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

        // 3. Upload para o S3
        String s3ObjectKey = null;
        try {
            // Salva a imagem no S3, sob a pasta 'faces/{employeeId}/'
            s3ObjectKey = faceStorageProvider.uploadFaceImage(employee.employeeId(), inputStream, "image/jpeg");

            // 4. Indexar a Face no Rekognition
            // O employeeId é o ExternalImageId, que será usado na busca futura (SearchFacesByImage).
            String faceId = faceRecognitionProvider.indexFace(s3ObjectKey, employee.employeeId());

            if (faceId == null) {
                // 5. Se nenhuma face for detectada, deletar o arquivo do S3 e lançar erro
                faceStorageProvider.deleteFaceImage(s3ObjectKey);
                throw new BadRequestException(NO_FACE_DETECTED);
            }


            // NOTA: Em uma aplicação real, aqui você salvaria o 's3ObjectKey' e o 'faceId'
            // em um campo do EmployeeEntity/tabela separada para poder DELETAR
            // o registro do Rekognition e do S3 no futuro.

        } catch (RuntimeException e) {
            // Em caso de falha na indexação ou upload (exceto face não detectada),
            // garante que o arquivo temporário no S3 seja removido.
            if (s3ObjectKey != null) {
                faceStorageProvider.deleteFaceImage(s3ObjectKey);
            }

            throw new RuntimeException("Falha ao registrar face no Rekognition.", e);
        }
    }
}