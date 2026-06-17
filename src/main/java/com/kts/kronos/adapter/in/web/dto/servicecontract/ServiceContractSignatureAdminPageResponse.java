package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.util.List;

public record ServiceContractSignatureAdminPageResponse(
        List<ServiceContractSignatureAdminItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
