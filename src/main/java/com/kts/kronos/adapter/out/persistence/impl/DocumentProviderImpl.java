package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
        var entity = documentRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
        return entity.toDomain();
    }

    @Override
    public Optional<Document> findByIdAndEmployeeId(UUID documentId, UUID employeeId) {
        return documentRepository.findByDocumentIdAndEmployeeId(documentId, employeeId).map(DocumentEntity::toDomain);
    }

    @Override
    public List<Document> findByEmployeeAndType(UUID employeeId, DocumentType type, boolean isManagerView) {
        if (isManagerView) {
            return documentRepository.findVisibleToManager(employeeId, type).stream().map(DocumentEntity::toDomain).collect(Collectors.toList());
        } else {
            return documentRepository.findVisibleToEmployee(employeeId, type).stream().map(DocumentEntity::toDomain).collect(Collectors.toList());
        }
    }

    @Override
    public List<Document> findByEmployeeAndDateAndType(UUID employeeId, LocalDate date, DocumentType type, boolean isManagerView) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<DocumentEntity> entities;

        if (isManagerView) {
            entities = documentRepository.findVisibleToManagerByDate(employeeId, start, end, type);
        } else {
            entities = documentRepository.findVisibleToEmployeeByDate(employeeId, start, end, type);
        }

        return entities.stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID employeeId, UUID documentId) {
        var employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var doc = findById(documentId);

        if (!doc.employeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException(DOCUMENT_NOT_BELONGS_EMPLOYEE);
        }

        documentRepository.deleteById(doc.documentId());
    }

    @Override
    public void deleteByEmployeeId(UUID employeeId) {
        documentRepository.deleteByEmployeeId(employeeId);
    }

    @Override
    public List<Document> findByTimeRecordId(Long timeRecordId) {
        return documentRepository.findByTimeRecordId(timeRecordId).stream().map(DocumentEntity::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmployeeIdAndType(UUID employeeId, DocumentType type) {
        return documentRepository.existsByEmployeeIdAndType(employeeId, type);
    }

    @Override
    public List<Document> findByTimeRecordIds(Collection<Long> timeRecordIds) {
        return documentRepository.findByTimeRecordIdIn(timeRecordIds)
                .stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

}
