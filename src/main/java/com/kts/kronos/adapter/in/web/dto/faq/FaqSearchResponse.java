package com.kts.kronos.adapter.in.web.dto.faq;

import java.util.List;

public record FaqSearchResponse(
        List<FaqSearchItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
