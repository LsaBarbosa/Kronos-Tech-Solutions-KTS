package com.kts.kronos.adapter.in.web.dto.timesheetsignature;

import java.util.List;

public record AdminTimesheetSignaturePageResponse(
        List<AdminTimesheetSignatureItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
