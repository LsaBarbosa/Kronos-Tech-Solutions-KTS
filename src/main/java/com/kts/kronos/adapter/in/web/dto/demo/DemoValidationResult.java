package com.kts.kronos.adapter.in.web.dto.demo;

import java.util.List;

public record DemoValidationResult(boolean clean, List<DemoValidationIssue> issues) {
    public static DemoValidationResult noResidues() {
        return new DemoValidationResult(true, List.of());
    }
}
