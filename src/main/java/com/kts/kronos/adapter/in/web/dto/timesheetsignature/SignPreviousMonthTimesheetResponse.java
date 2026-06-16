package com.kts.kronos.adapter.in.web.dto.timesheetsignature;

import java.time.Instant;
import java.util.UUID;

public record SignPreviousMonthTimesheetResponse(
        UUID signatureId,
        int referenceYear,
        int referenceMonth,
        Instant signedAt,
        String signatureType,
        String signatureMethod,
        String pointMirrorHashSha256,
        String recordsSnapshotHashSha256,
        UUID pointMirrorDocumentId,
        String declarationVersion
) {}
