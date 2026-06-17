package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogProviderImpl implements AuditLogProvider {

    private final AuditLogRepository repository;

    @Override
    public UUID registerLog(AuditLog domainLog) {
        try {
            AuditLogEntity entity = AuditLogEntity.builder()
                    .actorUserId(domainLog.actorUserId())
                    .targetEmployeeId(domainLog.targetEmployeeId())
                    .action(domainLog.action())
                    .ipAddress(domainLog.ipAddress())
                    .userAgent(domainLog.userAgent())
                    .details(SensitiveDataMasker.sanitizeDetails(domainLog.details()))
                    .timestamp(domainLog.timestamp())
                    .companyId(domainLog.companyId())
                    .resourceType(domainLog.resourceType())
                    .resourceId(domainLog.resourceId())
                    .correlationId(domainLog.correlationId())
                    .riskLevel(domainLog.riskLevel())
                    .build();

            AuditLogEntity saved = repository.save(entity);
            // Em produção `repository.save` nunca retorna null. Guardar contra null
            // só para tolerar testes que mockam save sem `thenReturn(entity)`.
            return saved != null ? saved.getId() : null;
        } catch (DataAccessException e) {
            log.warn(
                    "Falha absorvida ao salvar log de auditoria. actorUserId={}, action={}",
                    domainLog.actorUserId(),
                    domainLog.action(),
                    e
            );
            return null;
        }
    }

    @Override
    public List<AuditLog> findByActorUserId(UUID actorUserId) {
        return repository.findByActorUserIdOrderByTimestampDesc(actorUserId)
                .stream()
                .map(AuditLogEntity::toDomain)
                .toList();
    }

    @Override
    public List<AuditLog> findRelatedToDataSubject(UUID actorUserId, UUID targetEmployeeId) {
        return repository.findRelatedToDataSubject(actorUserId, targetEmployeeId)
                .stream()
                .map(AuditLogEntity::toDomain)
                .toList();
    }
}
