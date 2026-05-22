package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;

import java.util.Optional;

public interface LegalTextProvider {
    Optional<LegalText> findActiveByDocumentType(DocumentType documentType);
    Optional<LegalText> findByDocumentTypeAndVersion(DocumentType documentType, String version);
}
