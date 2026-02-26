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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;

@RequiredArgsConstructor
@Component
public class DocumentProviderImpl implements DocumentProvider {

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public Document save(Document doc) {
        var entity = DocumentEntity.fromDomain(doc);
        var saved = documentRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Document findById(UUID documentId) {
        var entity = documentRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
        return entity.toDomain();
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
        var doc = findByIdAndEmployeeId(documentId, employeeId);
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
    public List<Document> findByTimeRecordIdIn(Set<Long> timeRecordIds) {
        if (timeRecordIds == null || timeRecordIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findByTimeRecordIdIn(timeRecordIds).stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmployeeIdAndType(UUID employeeId, DocumentType type) {
        return documentRepository.existsByEmployeeIdAndType(employeeId, type);
    }

    @Override
    public List<Document> saveAll(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<DocumentEntity> entitiesToSave = documents.stream()
                .map(DocumentEntity::fromDomain)
                .collect(Collectors.toList());

        List<DocumentEntity> savedEntities = documentRepository.saveAll(entitiesToSave);

        return savedEntities.stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Document findLatestByEmployeeIdAndType(UUID employeeId, DocumentType type) {
        return documentRepository.findTopByEmployeeIdAndTypeOrderByUploadedAtDesc(employeeId, type)
                .map(DocumentEntity::toDomain)
                .orElse(null);
    }

    @Override
    public Document findByIdAndEmployeeId(UUID documentId, UUID employeeId) {
        return documentRepository.findByIdAndEmployeeId(documentId, employeeId)
                .map(DocumentEntity::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
    }
}
