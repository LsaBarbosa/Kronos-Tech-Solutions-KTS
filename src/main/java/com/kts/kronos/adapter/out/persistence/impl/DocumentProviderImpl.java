package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;

@RequiredArgsConstructor
@Component
public class DocumentProviderImpl implements DocumentProvider {

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public void save(Document doc) {
        var entity = DocumentEntity.fromDomain(doc);
        var saved = documentRepository.save(entity);
        saved.toDomain();
    }

    @Override
    public Document findById(UUID documentId) {
        var entity = documentRepository.findById(documentId).
                orElseThrow(()-> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
        return entity.toDomain();
    }

    @Override
    public List<Document> findByEmployeeAndType(UUID employeeId, DocumentType type) {
        return documentRepository.findByEmployeeIdAndType(employeeId,type ).stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Document> findByEmployeeAndDateAndType(UUID employeeId, LocalDate date, DocumentType type) {
        // define início e fim daquele dia em instantes
        Instant start = date.atStartOfDay(SAO_PAULO).toInstant();
        Instant end = date.plusDays(1)
                .atStartOfDay(SAO_PAULO)
                .minusNanos(1)
                .toInstant();

        return documentRepository
                .findByEmployeeIdAndTypeAndUploadedAtBetween(employeeId, start, end, String.valueOf(type))
                .stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID employeeId, UUID documentId) {
        var employee = employeeRepository.findById(employeeId)
                .orElseThrow(()-> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
       var doc =  findById(documentId);

       if (!doc.employeeId().equals(employee.getEmployeeId())){
           throw new BadRequestException(DOCUMENT_NOT_BELONGS_EMPLOYEE);
       }

       documentRepository.deleteById(doc.documentId());
    }

}
