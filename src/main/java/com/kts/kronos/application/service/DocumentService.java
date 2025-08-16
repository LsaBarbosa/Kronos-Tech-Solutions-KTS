package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.DocumentType;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


import static com.kts.kronos.constants.Messages.DOCUMENT_NOT_BELONGS_EMPLOYEE;
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
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        var employee = employeeProvider.findById(employeeIdWith).orElseThrow(
                ()-> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var bytes = file.getBytes();
        var doc = new Document(
                null,
                file.getOriginalFilename(),
                file.getContentType(),
                bytes,
                TIME_ZONE_BRAZIL,
                employee.employeeId(),
                type
        );
      documentProvider.save(doc);
    }

    @Override
    public Document downloadDocument(UUID employeeId,UUID documentId) throws IOException {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        var doc = documentProvider.findById(documentId);
       var employee = employeeProvider.findById(employeeIdWith)
               .orElseThrow(()-> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

       if (!doc.employeeId().equals(employee.employeeId())){
           throw new BadRequestException(DOCUMENT_NOT_BELONGS_EMPLOYEE);
       }
       return doc;
    }

    @Override
    public List<Document> listDocuments(DocumentType type,UUID employeeId, LocalDate date) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return date == null
                ? documentProvider.findByEmployeeAndType(employeeIdWith, type)
                : documentProvider.findByEmployeeAndDateAndType(employeeIdWith, date,type );
    }

    @Override
    public void deleteDocument(UUID employeeId, UUID documentId) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        documentProvider.delete(employeeIdWith,documentId);
    }
}
