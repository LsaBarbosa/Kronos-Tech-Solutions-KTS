package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.demo.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.service.demo.DemoSandboxService;
import com.kts.kronos.constants.ApiPaths;
import com.kts.kronos.constants.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CTO-only endpoints for the demo sandbox lifecycle.
 *
 * <p>All endpoints require role CTO. No sensitive data is logged or returned.</p>
 */
@RestController
@RequestMapping(ApiPaths.CTO_DEMO)
@RequiredArgsConstructor
public class CtoDemoController {

    private final DemoSandboxService     demoService;
    private final JwtAuthenticatedUser   currentUser;

    @PostMapping(ApiPaths.CTO_DEMO_CREATE)
    @PreAuthorize(Messages.KRONOS)
    public ResponseEntity<DemoCreateResponse> createDemo() {
        UUID actorId   = currentUser.getuserId();
        String role    = currentUser.getCurrentRole().name();
        DemoCreateResponse response = demoService.create(actorId, role);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @PreAuthorize(Messages.KRONOS)
    public ResponseEntity<DemoPurgeResponse> deleteDemo() {
        UUID actorId   = currentUser.getuserId();
        String role    = currentUser.getCurrentRole().name();
        DemoPurgeResponse response = demoService.purge(actorId, role);
        return ResponseEntity.ok(response);
    }

    @GetMapping(ApiPaths.CTO_DEMO_STATUS)
    @PreAuthorize(Messages.KRONOS)
    public ResponseEntity<DemoStatusResponse> demoStatus() {
        return ResponseEntity.ok(demoService.status());
    }

    @PostMapping(ApiPaths.CTO_DEMO_VALIDATE)
    @PreAuthorize(Messages.KRONOS)
    public ResponseEntity<DemoValidationResult> validateDemo() {
        return ResponseEntity.ok(demoService.validate());
    }
}
