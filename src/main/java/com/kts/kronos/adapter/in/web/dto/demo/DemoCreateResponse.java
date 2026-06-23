package com.kts.kronos.adapter.in.web.dto.demo;

import java.util.UUID;

public record DemoCreateResponse(
        UUID operationId,
        String status,
        String companyName,
        String username,
        boolean initialPasswordAvailable,
        DemoOperationCounters created,
        DemoValidationResult validation
) {}
