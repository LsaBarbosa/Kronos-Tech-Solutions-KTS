package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.domain.model.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.DOCUMENT_NOT_FOUND;
import static com.kts.kronos.constants.Messages.SAO_PAULO;

@RequiredArgsConstructor
@Component
public class DocumentProviderImpl implements DocumentProvider {

    private final DocumentRepository repository;

    @Override
    public Document save(Document doc) {
        var entity = DocumentEntity.fromDomain(doc);
        var saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Document findById(UUID documentId) {
        var entity = repository.findById(documentId).
                orElseThrow(()-> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
        return entity.toDomain();
    }

    @Override
    public List<Document> findAllByEmployee(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Document> findByEmployeeAndDate(UUID employeeId, LocalDate date) {
        // define início e fim daquele dia em instantes
        Instant start = date.atStartOfDay(SAO_PAULO).toInstant();
        Instant end = date.plusDays(1)
                .atStartOfDay(SAO_PAULO)
                .minusNanos(1)
                .toInstant();

        return repository.findByEmployeeIdAndUploadedAtBetween(employeeId, start, end)
                .stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

}
