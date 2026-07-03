package com.kts.kronos.adapter.in.web.dto.company;

import java.util.List;

public record CompanyHardDeleteResultDTO(
        String companyCnpj,
        String companyName,
        int employeesDeleted,
        int externalCleanupFailures,
        List<String> externalFailureDetails
) {}
