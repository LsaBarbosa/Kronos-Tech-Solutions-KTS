package com.kts.kronos.adapter.in.web.dto.faq;

import java.util.List;

public record FaqContextualResponse(
        String screen,
        List<FaqSearchItemResponse> items
) {
}
