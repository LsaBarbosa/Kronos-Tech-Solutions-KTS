package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.util.List;

public record PendingServiceContractListResponse(
        List<PendingServiceContractResponse> contracts
) {}
