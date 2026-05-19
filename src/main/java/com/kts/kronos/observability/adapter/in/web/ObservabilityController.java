package com.kts.kronos.observability.adapter.in.web;

import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import com.kts.kronos.observability.domain.ObservabilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ObservabilityController {

    private final ObservabilityStatusUseCase useCase;

    @GetMapping("/observability/status")
    public ObservabilityStatus status() {
        return useCase.getStatus();
    }
}
