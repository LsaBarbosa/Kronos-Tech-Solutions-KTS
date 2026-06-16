package com.kts.kronos.adapter.in.web.dto.timesheetsignature;

import java.time.Instant;
import java.util.UUID;

public record AdminTimesheetSignatureItem(
        UUID signatureId,
        UUID employeeId,
        String employeeName,
        int referenceYear,
        int referenceMonth,
        Instant signedAt,
        String status,
        String signatureType,
        String signatureMethod,
        String pointMirrorHashSha256
) {}
