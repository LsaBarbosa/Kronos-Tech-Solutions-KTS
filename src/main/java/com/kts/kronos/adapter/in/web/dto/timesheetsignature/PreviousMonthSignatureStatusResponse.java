package com.kts.kronos.adapter.in.web.dto.timesheetsignature;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PreviousMonthSignatureStatusResponse(
        int referenceYear,
        int referenceMonth,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        boolean eligible,
        boolean alreadySigned,
        UUID signatureId,
        Instant signedAt,
        String pointMirrorHashSha256,
        String recordsSnapshotHashSha256,
        String declarationVersion,
        String declarationText,
        String declarationHashSha256,
        List<String> blockers
) {}
