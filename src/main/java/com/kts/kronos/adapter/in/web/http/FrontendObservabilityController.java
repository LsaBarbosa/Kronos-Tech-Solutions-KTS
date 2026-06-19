package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.observability.FrontendObservabilityEventRequest;
import com.kts.kronos.application.service.FrontendObservabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/observability/frontend")
@RequiredArgsConstructor
public class FrontendObservabilityController {

    private final FrontendObservabilityService frontendObservabilityService;

    @PostMapping("/events")
    public ResponseEntity<Void> ingest(@Valid @RequestBody FrontendObservabilityEventRequest request) {
        frontendObservabilityService.accept(request);
        return ResponseEntity.accepted().build();
    }
}
