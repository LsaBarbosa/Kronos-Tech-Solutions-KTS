package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LegalTextRepository;
import com.kts.kronos.adapter.out.persistence.mapper.LegalTextMapper;
import com.kts.kronos.application.port.out.provider.LegalTextProvider;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LegalTextProviderImpl implements LegalTextProvider {
    private final LegalTextRepository repository;
    private final LegalTextMapper mapper;

    @Override
    public Optional<LegalText> findActiveByDocumentType(DocumentType documentType) {
        return repository.findByDocumentTypeAndActiveTrue(documentType)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<LegalText> findByDocumentTypeAndVersion(DocumentType documentType, String version) {
        return repository.findByDocumentTypeAndVersion(documentType, version)
                .map(mapper::toDomain);
    }
}
