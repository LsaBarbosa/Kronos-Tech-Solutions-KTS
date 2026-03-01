package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.domain.model.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.kts.kronos.constants.Logs.LOG_AUDIT_SAVE_CRITICAL_ERROR;

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
            
        } catch (Exception e) {
            // Log de auditoria não deve quebrar a aplicação, mas deve ser reportado no console
            log.error(LOG_AUDIT_SAVE_CRITICAL_ERROR, e.getMessage(), e);
        }
    }
}