package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LegalTextEntity;
import com.kts.kronos.domain.model.LegalText;
import org.springframework.stereotype.Component;

@Component
public class LegalTextMapper {

    public LegalText toDomain(LegalTextEntity entity) {
        return new LegalText(
                entity.getLegalTextId(),
                entity.getDocumentType(),
                entity.getVersion(),
                entity.getTitle(),
                entity.getContent(),
                entity.getContentHashSha256(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getPublishedAt()
        );
    }

    public LegalTextEntity toEntity(LegalText domain) {
        return LegalTextEntity.builder()
                .legalTextId(domain.legalTextId())
                .documentType(domain.documentType())
                .version(domain.version())
                .title(domain.title())
                .content(domain.content())
                .contentHashSha256(domain.contentHashSha256())
                .active(domain.active())
                .createdAt(domain.createdAt())
                .publishedAt(domain.publishedAt())
                .build();
    }
}
