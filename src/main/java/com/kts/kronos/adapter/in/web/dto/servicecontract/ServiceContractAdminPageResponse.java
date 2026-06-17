package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.util.List;

public record ServiceContractAdminPageResponse(
        List<ServiceContractAdminItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
