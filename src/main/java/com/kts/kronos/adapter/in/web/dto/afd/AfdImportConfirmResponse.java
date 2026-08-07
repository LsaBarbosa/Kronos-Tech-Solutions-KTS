package com.kts.kronos.adapter.in.web.dto.afd;

import java.util.List;

public record AfdImportConfirmResponse(
        int timeRecordsCreated,
        int timeRecordsDuplicated,
        int afdEntriesSaved,
        int afdEntriesDuplicated,
        int employeesNotLinked,
        List<String> notLinkedPisList
) {}
