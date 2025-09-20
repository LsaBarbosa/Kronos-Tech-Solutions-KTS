package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kts.kronos.application.exceptions.BadRequestException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;
import static com.kts.kronos.constants.Messages.TIME_ZONE_BRAZIL;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    private final DocumentProvider documentProvider;
    private final EmployeeProvider employeeProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @Override
    public void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new BadRequestException("Apenas arquivos PDF são permitidos.");
        }

        try {
            var employee = getEmployee(employeeId);

            var bytes = file.getBytes();
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bytes,
                    TIME_ZONE_BRAZIL
            );
            documentProvider.save(doc);
        } catch (Exception e) {
            throw new BadRequestException("Não foi possível ler o arquivo: ");
        }
    }

    @Override
    public Document downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        getEmployee(employeeId);
        try {
            return documentProvider.findById(documentId);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Documento com registro:" + documentId + " não foi encontrado.");
        }
    }

    @Override
    public List<Document> listDocuments(DocumentType type, UUID employeeId, LocalDate date) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return date == null
                ? documentProvider.findByEmployeeAndType(employeeIdWith, type)
                : documentProvider.findByEmployeeAndDateAndType(employeeIdWith, date, type);
    }

    @Override
    public void deleteDocument(UUID employeeId, UUID documentId) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        documentProvider.delete(employeeIdWith, documentId);
    }

    private Employee getEmployee(UUID employeeId) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return employeeProvider.findById(employeeIdWith)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }
}
