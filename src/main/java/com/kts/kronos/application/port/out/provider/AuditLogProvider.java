package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogProvider {
    /**
     * Persiste a evidência de auditoria e retorna o UUID gerado.
     * Pode retornar {@code null} se a persistência falhar e o erro for tolerado
     * pela implementação (auditoria não deve quebrar o fluxo de negócio).
     */
    UUID registerLog(AuditLog auditLog);

    List<AuditLog> findByActorUserId(UUID actorUserId);

    List<AuditLog> findRelatedToDataSubject(UUID actorUserId, UUID targetEmployeeId);
}
