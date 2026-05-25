package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.inventory.CreateInventoryRequest;
import com.kts.kronos.adapter.in.web.dto.inventory.DataProcessingInventoryResponse;
import com.kts.kronos.application.service.DataProcessingInventoryService;
import com.kts.kronos.constants.ApiPaths;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.LGPD + ApiPaths.LGPD_INVENTORY)
@RequiredArgsConstructor
public class DataProcessingInventoryController {
    private final DataProcessingInventoryService service;

    @PreAuthorize("hasAnyRole('CTO')")
    @GetMapping
    public ResponseEntity<Page<DataProcessingInventoryResponse>> listInventories(Pageable pageable) {
        var page = service.listAllInventories(pageable)
                .map(DataProcessingInventoryResponse::fromDomain);
        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasAnyRole('CTO')")
    @GetMapping("/active")
    public ResponseEntity<Page<DataProcessingInventoryResponse>> listActiveInventories(Pageable pageable) {
        var page = service.listActiveInventories(pageable)
                .map(DataProcessingInventoryResponse::fromDomain);
        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasAnyRole('CTO')")
    @GetMapping("/{processCode}")
    public ResponseEntity<DataProcessingInventoryResponse> getByProcessCode(@PathVariable String processCode) {
        var inventory = service.getInventoryByProcessCode(processCode);
        return ResponseEntity.ok(DataProcessingInventoryResponse.fromDomain(inventory));
    }

    @PreAuthorize("hasAnyRole('CTO')")
    @PostMapping
    public ResponseEntity<DataProcessingInventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        var created = service.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DataProcessingInventoryResponse.fromDomain(created));
    }

    @PreAuthorize("hasAnyRole('CTO')")
    @PatchMapping("/{inventoryId}")
    public ResponseEntity<DataProcessingInventoryResponse> updateInventory(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody CreateInventoryRequest request) {
        var updated = service.updateInventory(inventoryId, request);
        return ResponseEntity.ok(DataProcessingInventoryResponse.fromDomain(updated));
    }
}
