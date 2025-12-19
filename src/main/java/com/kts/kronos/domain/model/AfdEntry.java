package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AfdEntry(
        Long id,
        Long nsr,
        String recordType,
        LocalDateTime recordDate,
        String employeeCpf,
        String employeePis,
        UUID companyId,
        UUID employeeId,
        String previousHash,
        String currentHash
) {
    // Construtor auxiliar para criação
    public AfdEntry(Long nsr, String recordType, LocalDateTime recordDate, String employeeCpf, String employeePis, UUID companyId, UUID employeeId, String previousHash, String currentHash) {
        this(null, nsr, recordType, recordDate, employeeCpf, employeePis, companyId, employeeId, previousHash, currentHash);
    }
}