package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.domain.model.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogProviderImpl implements AuditLogProvider {

    private final AuditLogRepository repository;

    @Override
    public void registerLog(AuditLog domainLog) {
        try {
            // Mapeamento Domínio -> Entidade JPA
            AuditLogEntity entity = AuditLogEntity.builder()
                    .userId(domainLog.userId())
                    .action(domainLog.action())
                    .ipAddress(domainLog.ipAddress())
                    .userAgent(domainLog.userAgent())
                    .details(domainLog.details())
                    .timestamp(domainLog.timestamp())
                    .build();

            repository.save(entity);

        } catch (DataAccessException e) {
            log.warn(
                    "Falha absorvida ao salvar log de auditoria. userId={}, action={}",
                    domainLog.userId(),
                    domainLog.action(),
                    e
            );
        }
    }
}