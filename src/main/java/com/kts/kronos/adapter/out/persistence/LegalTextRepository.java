package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LegalTextEntity;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LegalTextRepository extends JpaRepository<LegalTextEntity, UUID> {
    Optional<LegalTextEntity> findByDocumentTypeAndActiveTrue(DocumentType documentType);
    Optional<LegalTextEntity> findByDocumentTypeAndVersion(DocumentType documentType, String version);
}
